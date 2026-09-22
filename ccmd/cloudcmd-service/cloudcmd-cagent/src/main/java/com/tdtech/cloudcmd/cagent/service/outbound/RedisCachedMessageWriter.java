package com.tdtech.cloudcmd.cagent.service.outbound;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.tdtech.cloudcmd.cagent.server.ChannelStatusHandler;
import com.tdtech.cloudcmd.cagent.server.frame.CdcFrame;
import com.tdtech.cloudcmd.cagent.server.frame.CdcFrameHeader;
import com.tdtech.cloudcmd.cagent.service.GroupChannelTable;
import com.tdtech.cloudcmd.cagent.service.UserInfo;
import com.tdtech.cloudcmd.cagent.service.entity.LocalChannel;
import com.tdtech.cloudcmd.cagent.service.outbound.callback.SendMessageCallBack;
import com.tdtech.cloudcmd.redis.RedisUtil;
import com.tdtech.cloudcmd.util.IdWorker;

import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RedisCachedMessageWriter implements ChannelStatusHandler {

    private static final String REDIS_MESSAGE_QUEUE_PREFIX = "cloudcmd:cagent:message-queue:";
    private static final long QUEUE_EXPIRE_TIME = 30L;

    @Resource
    private RedisUtil redisUtil;
    @Resource
    private GroupChannelTable channelTable;
    @Resource
    private List<SendMessageCallBack> callBacks;
    @Resource
    private IdWorker idWorker;

    public void sendByDataStrategy(List<UserInfo> userInfos, CdcFrame data) {
        switch (data.getHeader().getSendStrategy()) {
            case CdcFrameHeader.DEFAULT_SEND_STRATEGY: {
                send(userInfos, data);
                break;
            }
            case CdcFrameHeader.QUEUE_CACHED_SEND_STRATEGY: {
                pushQueue(userInfos, data);
                break;
            }
            default: {
                throw new UnsupportedOperationException("error");
            }
        }
    }

    public void sendByDataStrategy(Stream<LocalChannel> localChannelStream, CdcFrame data) {
        switch (data.getHeader().getSendStrategy()) {
            case CdcFrameHeader.DEFAULT_SEND_STRATEGY: {
                send(localChannelStream, data);
                break;
            }
            case CdcFrameHeader.QUEUE_CACHED_SEND_STRATEGY: {
                pushQueue(localChannelStream, data);
                break;
            }
            default: {
                throw new UnsupportedOperationException("error");
            }
        }
    }

    public void send(Collection<UserInfo> userInfos, CdcFrame data) {
        for (UserInfo userInfo : userInfos) {
            try {
                LocalChannel localByUser = channelTable.findLocalByUser(userInfo);
                if (localByUser == null || !localByUser.getChannel().isActive()) {
                    redisUtil.rPush(REDIS_MESSAGE_QUEUE_PREFIX + userInfo.compareTag(),
                        new QueueMsg(data, idWorker.nextId()));
                    redisUtil.expire(REDIS_MESSAGE_QUEUE_PREFIX + userInfo.compareTag(), QUEUE_EXPIRE_TIME,
                        TimeUnit.SECONDS);
                    continue;
                }
                log.info("write data to:{} with:{}", userInfo, data);
                localByUser.getChannel().writeAndFlush(data).addListener(f -> {
                    for (SendMessageCallBack callBack : callBacks) {
                        try {
                            callBack.callback(localByUser, f, data);
                        } catch (Exception e) {
                            log.error("callback error", e);
                        }
                    }
                });
            } catch (Exception e) {
                log.error("send error", e);
                redisUtil.rPush(REDIS_MESSAGE_QUEUE_PREFIX + userInfo.compareTag(),
                    new QueueMsg(data, idWorker.nextId()));
                redisUtil.expire(REDIS_MESSAGE_QUEUE_PREFIX + userInfo.compareTag(), QUEUE_EXPIRE_TIME,
                    TimeUnit.SECONDS);
            }
        }
    }

    public void send(Stream<LocalChannel> localChannels, CdcFrame data) {
        localChannels.forEach(localChannel -> {
            String key = REDIS_MESSAGE_QUEUE_PREFIX + localChannel.getUserInfo().compareTag();
            try {
                Channel channel = localChannel.getChannel();
                if (channel.isActive()) {
                    log.info("write data to:{} with:{}", localChannel.getUserInfo(), data);
                    channel.writeAndFlush(data).addListener(f -> {
                        for (SendMessageCallBack callBack : callBacks) {
                            try {
                                callBack.callback(localChannel, f, data);
                            } catch (Exception e) {
                                log.error("callback error", e);
                            }
                        }
                    });
                } else {
                    redisUtil.rPush(key, new QueueMsg(data, idWorker.nextId()));
                    redisUtil.expire(key, QUEUE_EXPIRE_TIME, TimeUnit.SECONDS);
                }
            } catch (Exception e) {
                log.error("send error", e);
                redisUtil.rPush(key, new QueueMsg(data, idWorker.nextId()));
                redisUtil.expire(key, QUEUE_EXPIRE_TIME, TimeUnit.SECONDS);
            }
        });
    }

    public void pushQueue(Collection<UserInfo> userInfos, CdcFrame data) {
        Objects.requireNonNull(userInfos);
        for (UserInfo userInfo : userInfos) {
            redisUtil.rPush(REDIS_MESSAGE_QUEUE_PREFIX + userInfo.compareTag(), new QueueMsg(data, idWorker.nextId()));
            redisUtil.expire(REDIS_MESSAGE_QUEUE_PREFIX + userInfo.compareTag(), QUEUE_EXPIRE_TIME,
                    TimeUnit.SECONDS);
        }
    }

    public void pushQueue(Stream<LocalChannel> localChannelStream, CdcFrame data) {
        Objects.requireNonNull(localChannelStream);
        localChannelStream.map(LocalChannel::getUserInfo).map(UserInfo::compareTag).forEach(tag -> {
            redisUtil.rPush(REDIS_MESSAGE_QUEUE_PREFIX + tag, new QueueMsg(data, idWorker.nextId()));
            redisUtil.expire(REDIS_MESSAGE_QUEUE_PREFIX + tag, QUEUE_EXPIRE_TIME,
                    TimeUnit.SECONDS);
        });
    }

    @Override
    public void onChannelAuth(Channel channel, UserInfo userInfo) {
        // do nothing
    }

    @Override
    public void onChannelInactive(Channel channel, UserInfo userInfo) {
        // do nothing
    }

    @Override
    public void onChannelHeartBeat(Channel channel, UserInfo userInfo) {
        String key = REDIS_MESSAGE_QUEUE_PREFIX + userInfo.compareTag();
        List<QueueMsg> history;
        while ((history = redisUtil.lPop(key, 100, QueueMsg.class)) != null && !history.isEmpty()) {
            log.debug("queued message:{} for user:{}", history.toArray(), userInfo);
            history.sort(Comparator.comparingLong(QueueMsg::getSeq));
            for (var msg : history) {
                var frame = msg.data;
                try {
                    channel.writeAndFlush(frame);
                } catch (Exception e) {
                    log.error("", e);
                    if (frame.getRetryCount() < 3) {
                        frame.setRetryCount(frame.getRetryCount() + 1);
                        log.warn("send msg failed:{}",frame);
                    }
                }
            }
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    private static class QueueMsg {
        private CdcFrame data;
        private Long seq;
    }
}
