package com.tdtech.cloudcmd.cagent.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

import com.tdtech.cloudcmd.cagent.conf.CagentProperties;
import com.tdtech.cloudcmd.cagent.conf.SystemProperties;
import com.tdtech.cloudcmd.cagent.exception.PinGaoServiceException;
import com.tdtech.cloudcmd.cagent.rpc.ChannelUpdateActionEnum;
import com.tdtech.cloudcmd.cagent.rpc.ChannelUpdateDTO;
import com.tdtech.cloudcmd.cagent.rpc.GroupRpcClient;
import com.tdtech.cloudcmd.cagent.rpc.GroupRpcClientFactory;
import com.tdtech.cloudcmd.cagent.server.ChannelStatusHandler;
import com.tdtech.cloudcmd.cagent.service.distributed.LeaderStatus;
import com.tdtech.cloudcmd.cagent.service.distributed.LeaderStatusObserver;
import com.tdtech.cloudcmd.cagent.service.entity.BaseChannel;
import com.tdtech.cloudcmd.cagent.service.entity.LocalChannel;
import com.tdtech.cloudcmd.cagent.service.entity.ShadowedChannel;
import com.tdtech.cloudcmd.util.IdWorker;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

/**
 * based on leader status machine
 *
 *
 * there will two table for both local channel and remote channel, local table works for local channel without caring
 * about leader status, but remote channel only works when leaderStatus in LEADER status.
 *
 *
 * there will be two different status: leaderStatus, queueStatus.leaderStatus used for outcome message status
 * predication. queueStatus used for predicate whether to push queue when leaderStatus in INIT status.
 *
 *
 * when leaderStatus change to INIT, it will automatically transform to LEADER or FOLLOWER status, according to
 * LeaderStatusMachine, but there will be some difference in timeline for the status in GroupChannelTable and
 * LeaderStatusMachine.
 *
 *
 * LeaderStatusObserver implemented method will be called every time when leader status check, so a duplicated method
 * implementation required
 *
 *
 * INIT: all changes almost in the same time.
 *
 *
 * LEADER: LeaderStatusMachine triggered ----------------------> queueStatus change to LEADER
 * -----------------------------------> leaderStatus change to LEADER
 *
 *
 * ---------------------------------------- INIT FOLLOWER TABLE ------------------------------- HANDLE MESSAGE WHEN INIT
 * STATUS
 *
 *
 * FOLLOWER: LeaderStatusMachine triggered ----------------------------------- queueStatus change to FOLLOWER
 * ----------------------------------- leaderStatus change to FOLLOWER
 *
 * -----------------------------------------REPORT LOCAL TABLE TO LEADER -------------------------------------------
 * NODE REPORT MESSAGE WHEN INIT STATUS
 *
 *
 */
@Slf4j
@Component
public class GroupChannelTable implements LeaderStatusObserver, ChannelStatusHandler {

    private final ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(10);
    private final ChannelTable<LocalChannel> localTable = new ChannelTable<>();
    private final Queue<ChannelUpdateDTO> updateQueue = new LinkedBlockingQueue<>();
    private final ReentrantLock conditionLock = new ReentrantLock();
    private final Condition condition = conditionLock.newCondition();
    @Resource
    private DiscoveryClient discoveryClient;
    @Resource
    private SystemProperties systemProperties;
    @Resource
    private CagentProperties cagentProperties;
    @Resource
    private GroupRpcClientFactory clientFactory;
    @Resource
    private IdWorker idWorker;
    private ChannelTable<ShadowedChannel> followerTable = new ChannelTable<>();
    private volatile LeaderStatus leaderStatus = LeaderStatus.INIT;
    private volatile LeaderStatus queueStatus = LeaderStatus.INIT;
    private String leaderHost;

    @PreDestroy
    public void close() {
        scheduledExecutorService.shutdown();
    }

    // ************************** leader status function ********************************//
    @Override
    public void onInit() {
        leaderStatus = LeaderStatus.INIT;
        queueStatus = LeaderStatus.INIT;
    }

    @Override
    public void onLeader() {
        if (leaderStatus == LeaderStatus.LEADER) {
            return;
        }
        this.leaderHost = systemProperties.getIpPort();
        // clean follower table and pull all from cluster
        ChannelTable<ShadowedChannel> followerTableMap = new ChannelTable<>();
        List<ServiceInstance> instances = discoveryClient.getInstances(systemProperties.getAppName());
        if (instances != null && !instances.isEmpty()) {
            instances.stream().filter(a -> !Objects.equals(a.getHost(), systemProperties.getIp())).parallel()
                .forEach(ins -> {
                    try {
                        GroupRpcClient client = clientFactory.newRpcClient(ins.getHost(), ins.getPort());
                        List<ChannelUpdateDTO> allChannel = client.getAllLocalChannel();
                        if (allChannel != null && !allChannel.isEmpty()) {
                            for (ChannelUpdateDTO channel : allChannel) {
                                followerTableMap.register(channel.getChannel());
                            }
                        }
                    } catch (Exception e) {
                        log.error("pull follower map error, ip:" + ins.getHost() + " port:" + ins.getPort(), e);
                    }
                });
        }
        this.followerTable = followerTableMap;
        // stop push queue,then add to follower table
        queueStatus = LeaderStatus.LEADER;
        ChannelUpdateDTO ud;
        while ((ud = updateQueue.poll()) != null) {
            dealWithUpdateDTO(ud);
        }
        Collection<LocalChannel> all = this.localTable.getAll();
        all.stream().map(ShadowedChannel::new).forEach(this.followerTable::register);
        leaderStatus = LeaderStatus.LEADER;
        // notify waiting
        notifyWaiting();
    }

    @Override
    public void onFollower(String leaderHost) {
        if (leaderStatus == LeaderStatus.FOLLOWER && leaderHost.equals(this.leaderHost)) {
            return;
        }
        this.leaderHost = leaderHost;
        // clean follower table and report to leader
        this.followerTable = null;
        String[] split = leaderHost.split(":");
        Collection<LocalChannel> allLocal = getAllLocal();
        GroupRpcClient groupRpcClient = clientFactory.newRpcClient(split[0], Integer.valueOf(split[1]));
        if (allLocal != null && !allLocal.isEmpty()) {
            List<ChannelUpdateDTO> collect = allLocal.stream()
                .map(a -> new ShadowedChannel(a.getUserInfo(), a.getId(), systemProperties.getIp(),
                    systemProperties.getPort()))
                .map(a -> new ChannelUpdateDTO(a, ChannelUpdateActionEnum.SAVE)).collect(Collectors.toList());
            groupRpcClient.reportChannelUpdate(collect);
        }
        // stop push queue,then report queue to leader
        queueStatus = LeaderStatus.FOLLOWER;
        if (!updateQueue.isEmpty()) {
            List<ChannelUpdateDTO> list = new ArrayList<>(updateQueue);
            groupRpcClient.reportChannelUpdate(list);
            updateQueue.clear();
        }
        leaderStatus = LeaderStatus.FOLLOWER;
        // notify waiting
        notifyWaiting();
    }

    /**
     * push to queue when init, add to follower table when leader, and exception when else
     */
    public void followerChannelUpdate(Collection<ChannelUpdateDTO> channelList) {
        switch (queueStatus) {
            case INIT: {
                updateQueue.addAll(channelList);
                break;
            }
            case LEADER: {
                if (channelList != null) {
                    channelList.forEach(this::dealWithUpdateDTO);
                }
                break;
            }
            default: {
                throw new RuntimeException("only leader can receive follower update message");
            }
        }
    }

    public Collection<LocalChannel> getAllLocal() {
        return localTable.getAll();
    }

    public Collection<ShadowedChannel> getAllRemote() {
        waitLeaderStatus();
        switch (leaderStatus) {
            case LEADER: {
                return followerTable.getAll();
            }
            case FOLLOWER: {
                throw new PinGaoServiceException("follower cant deal this");
            }
            default: {
                // not possible to be here
                throw new UnsupportedOperationException();
            }
        }
    }

    /**
     * wait for leader status change to leader or follower
     *
     * throws exception when interrupted or run out of time
     */
    public void waitLeaderStatus() {
        if (leaderStatus != LeaderStatus.INIT) {
            return;
        }
        int count = 0;
        conditionLock.lock();
        try {
            while (leaderStatus == LeaderStatus.INIT && !condition.await(500L, TimeUnit.MILLISECONDS) && count < 6) {
                count++;
            }
            if (leaderStatus == LeaderStatus.INIT) {
                throw new PinGaoServiceException("wait leader status time out");
            }
        } catch (InterruptedException e) {
            log.error("interrupted", e);
            Thread.currentThread().interrupt();
            throw new PinGaoServiceException("lock interrupted");
        } finally {
            conditionLock.unlock();
        }
    }

    private void notifyWaiting() {
        conditionLock.lock();
        try {
            condition.signalAll();
        } finally {
            conditionLock.unlock();
        }
    }

    public LeaderStatus leaderStatus() {
        return leaderStatus;
    }

    public String leaderHost() {
        return leaderHost;
    }
    // ************************** leader status function ********************************//

    // ************************** service ********************************//
    public LocalChannel findLocalByUser(UserInfo userInfo) {
        return localTable.findByUserInfo(userInfo);
    }

    public ShadowedChannel findRemoteByUser(UserInfo userInfo) {
        waitLeaderStatus();
        switch (leaderStatus) {
            case LEADER: {
                return followerTable.findByUserInfo(userInfo);
            }
            case FOLLOWER: {
                throw new RuntimeException("follower cant deal this");
            }
            default: {
                // not possible enter here
                throw new UnsupportedOperationException();
            }
        }
    }

    // ************************** service *******************************//

    private void dealWithUpdateDTO(ChannelUpdateDTO channelUpdateDTO) {
        switch (channelUpdateDTO.getAction()) {
            case SAVE: {
                followerTable.register(channelUpdateDTO.getChannel());
                break;
            }
            case REMOVE: {
                followerTable.removeLater(channelUpdateDTO.getChannel());
                break;
            }
            default: {
                throw new UnsupportedOperationException("channel update action error:" + channelUpdateDTO.getAction());
            }
        }
    }

    // ************************** local call back ***************************************//
    @Override
    public void onChannelAuth(Channel channel, UserInfo userInfo) {
        log.debug("on channel auth:{}", userInfo);
        LocalChannel localChannel = new LocalChannel();
        localChannel.setChannel(channel);
        localChannel.setUserInfo(userInfo);
        localChannel.setId(idWorker.nextId());
        localTable.register(localChannel);
        // waitLeaderStatus();
        if (queueStatus == LeaderStatus.FOLLOWER) {
            GroupRpcClient groupRpcClient = clientFactory.newRpcClient(leaderHost);
            groupRpcClient.reportChannelUpdate(
                Collections.singletonList(new ChannelUpdateDTO(new ShadowedChannel(userInfo, localChannel.getId(),
                    systemProperties.getIp(), systemProperties.getPort()), ChannelUpdateActionEnum.SAVE)));
        } else if (queueStatus == LeaderStatus.LEADER) {
            this.followerTable.register(new ShadowedChannel(localChannel));
        } else {
            // queue INIT Status
            this.updateQueue.add(new ChannelUpdateDTO(new ShadowedChannel(localChannel), ChannelUpdateActionEnum.SAVE));
        }
    }

    @Override
    public void onChannelInactive(Channel channel, UserInfo userInfo) {
        log.info("on channel inactive:{}", userInfo);
        try {
            LocalChannel localChannel = localTable.findByUserInfo(userInfo);
            if (localChannel == null) {
                return;
            }
            localTable.remove(localChannel);
            // waitLeaderStatus();
            if (queueStatus == LeaderStatus.FOLLOWER) {
                GroupRpcClient groupRpcClient = clientFactory.newRpcClient(leaderHost);
                groupRpcClient.reportChannelUpdate(
                    Collections.singletonList(new ChannelUpdateDTO(new ShadowedChannel(userInfo, localChannel.getId(),
                        systemProperties.getIp(), systemProperties.getPort()), ChannelUpdateActionEnum.REMOVE)));
            } else if (queueStatus == LeaderStatus.LEADER) {
                this.followerTable.removeLater(new ShadowedChannel(localChannel));
            } else {
                // queue INIT Status
                this.updateQueue
                    .add(new ChannelUpdateDTO(new ShadowedChannel(localChannel), ChannelUpdateActionEnum.REMOVE));
            }
        } catch (Exception e) {
            log.error("error", e);
        }
    }

    @Override
    public void onChannelHeartBeat(Channel channel, UserInfo userInfo) {
        // do nothing
    }

    // ************************** local call back ***************************************//
    private class ChannelTable<T extends BaseChannel> {

        private final ConcurrentHashMap<String, T> userIdClientIdIndex;

        public ChannelTable() {
            this.userIdClientIdIndex = new ConcurrentHashMap<>();
        }

        public void register(T sc) {
            UserInfo userInfo = sc.getUserInfo();
            userIdClientIdIndex.put(userInfo.compareTag(), sc);
        }

        public void remove(T sc) {
            userIdClientIdIndex.compute(sc.getUserInfo().compareTag(), (k, v) -> {
                if (v == null) {
                    return null;
                }
                if (v.getId().equals(sc.getId())
                    && Objects.equals(v.getUserInfo().compareTag(), sc.getUserInfo().compareTag())) {
                    return null;
                }
                return v;
            });
        }

        public void removeLater(T sc) {
            if (cagentProperties.getRemoveDelay() > 0) {
                scheduledExecutorService.schedule(() -> remove(sc), cagentProperties.getRemoveDelay(),
                    TimeUnit.MILLISECONDS);
            } else {
                remove(sc);
            }
        }

        public T findByUserInfo(UserInfo userInfo) {
            Objects.requireNonNull(userInfo.getUserId());
            Objects.requireNonNull(userInfo.getAppKey());
            return userIdClientIdIndex.get(userInfo.compareTag());
        }

        public Collection<T> getAll() {
            return userIdClientIdIndex.values();
        }
    }
}
