package com.tdtech.cloudcmd.cagent.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import com.tdtech.cloudcmd.cagent.conf.SystemProperties;
import com.tdtech.cloudcmd.cagent.rpc.GroupRpcClient;
import com.tdtech.cloudcmd.cagent.rpc.GroupRpcClientFactory;
import com.tdtech.cloudcmd.cagent.rpc.TaskDTO;
import com.tdtech.cloudcmd.cagent.server.frame.CdcFrame;
import com.tdtech.cloudcmd.cagent.service.entity.ShadowedChannel;
import com.tdtech.cloudcmd.cagent.service.outbound.RedisCachedMessageWriter;
import com.tdtech.cloudcmd.cagent.service.outbound.rule.DispatcherRule;
import com.tdtech.cloudcmd.cagent.service.outbound.rule.PrivilegeDispatcherRule;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MessageService {

    @Resource
    private List<DispatcherRule> dispatcherRuleList;
    @Resource
    private GroupChannelTable channelTable;
    @Resource
    private GroupRpcClientFactory clientFactory;
    @Resource
    private RedisCachedMessageWriter redisCachedMessageWriter;
    @Resource
    private TaskExecutor taskExecutor;
    @Resource
    private AuthManager authManager;
    @Resource
    private DiscoveryClient discoveryClient;
    @Resource
    private SystemProperties systemProperties;
    @Resource
    private PrivilegeDispatcherRule privilegeDispatcherRule;

    public void dispatch(CdcFrame data) {
        switch (channelTable.leaderStatus()) {
            case LEADER: {
                distribute(data);
                return;
            }
            case FOLLOWER: {
                GroupRpcClient groupRpcClient = clientFactory.newRpcClient(channelTable.leaderHost());
                groupRpcClient.reportTask(data);
                return;
            }
            case INIT: {
                channelTable.waitLeaderStatus();
                dispatch(data);
            }
            default: {
                throw new UnsupportedOperationException();
            }
        }
    }

    private void distribute(String from, String to, CdcFrame data, Stream<ShadowedChannel> channelStream) {
        for (DispatcherRule dispatcherRule : dispatcherRuleList) {
            if (dispatcherRule.match(from, to)) {
                channelStream = dispatcherRule.pick(from, to, channelStream);
            }
        }
        Map<String, List<ShadowedChannel>> group =
            channelStream.collect(Collectors.groupingBy(a -> a.isLocal() ? "LOCAL" : a.getIp() + ":" + a.getPort()));
        if (log.isDebugEnabled()) {
            group.forEach((k, v) -> log.debug("host:{} channel:{} for msg:{}", k, Arrays.toString(v.toArray()), data));
        }
        // send local
        List<ShadowedChannel> local = group.get("LOCAL");
        if (local != null && !local.isEmpty()) {
            taskExecutor.execute(() -> redisCachedMessageWriter
                .sendByDataStrategy(local.stream().map(ShadowedChannel::getLocalChannel), data));
        }
        // send remote
        List<ServiceInstance> instances = discoveryClient.getInstances(systemProperties.getAppName());
        var collect = instances.stream().map(a -> a.getHost() + ":" + a.getPort()).collect(Collectors.toList());
        group.forEach((k, v) -> taskExecutor.execute(() -> {
            if ("LOCAL".equals(k)) {
                return;
            }
            if (!collect.contains(k)) {
                log.warn("service node {} not exists anymore", k);
                return;
            }
            ShadowedChannel shadowedChannel = v.get(0);
            GroupRpcClient groupRpcClient =
                clientFactory.newRpcClient(shadowedChannel.getIp(), shadowedChannel.getPort());
            groupRpcClient.onDistributeTask(
                new TaskDTO(data, v.stream().map(ShadowedChannel::getUserInfo).collect(Collectors.toList())));
        }));
    }

    /**
     * distribute message according to token in data header
     *
     * when empty do broadcast
     *
     * when contains dash mark, split with dash mark and filter channels
     *
     * when not contains dash mark, considering use as login token and find login channel to send message
     */
    public void distribute(CdcFrame data) {
        String tk = data.getHeader().getToken();
        if (StringUtils.isBlank(tk)) {
            Stream<ShadowedChannel> all = channelTable.getAllRemote().stream();
            if (data.getPrivOrg() != null) {
                all = privilegeDispatcherRule.pick(data.getPrivOrg(), all);
            }
            distribute(DispatcherRule.ASTERISK, DispatcherRule.ASTERISK, data, all);
        } else if (!tk.contains(DispatcherRule.DASH)) {
            UserInfo redisInfo = authManager.getRedisInfo(tk);
            if (redisInfo == null) {
                return;
            }
            ShadowedChannel remoteByUser = channelTable.findRemoteByUser(redisInfo);
            if (remoteByUser != null) {
                distribute(DispatcherRule.ASTERISK, DispatcherRule.ASTERISK, data, Stream.of(remoteByUser));
            }
        } else {
            String[] split = tk.split(DispatcherRule.DASH, 2);
            String from = split[0].trim();
            String to = split[1].trim();
            Stream<ShadowedChannel> all = channelTable.getAllRemote().stream();
            if (data.getPrivOrg() != null) {
                all = privilegeDispatcherRule.pick(data.getPrivOrg(), all);
            }
            distribute(from, to, data, all);
        }
    }

}
