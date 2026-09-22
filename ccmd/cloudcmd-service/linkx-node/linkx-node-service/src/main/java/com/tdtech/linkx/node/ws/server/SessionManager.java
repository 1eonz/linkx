package com.tdtech.linkx.node.ws.server;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket会话管理器
 */
@Slf4j
@Component
public class SessionManager {

    /**
     * peerId -> Channel 映射
     */
    private final Map<String, Channel> peerChannelMap = new ConcurrentHashMap<>();

    /**
     * sessionId -> peerId 映射
     */
    private final Map<String, String> sessionPeerMap = new ConcurrentHashMap<>();

    /**
     * Channel -> peerId 映射
     */
    private final Map<Channel, String> channelPeerMap = new ConcurrentHashMap<>();

    /**
     * 注册会话
     */
    public void register(String sessionId, String peerId, Channel channel) {
        peerChannelMap.put(peerId, channel);
        sessionPeerMap.put(sessionId, peerId);
        channelPeerMap.put(channel, peerId);
        log.info("Session registered: sessionId={}, peerId={}", sessionId, peerId);
    }

    /**
     * 注销会话
     */
    public void unregister(String peerId) {
        Channel channel = peerChannelMap.remove(peerId);
        if (channel != null) {
            channelPeerMap.remove(channel);
        }
        sessionPeerMap.entrySet().removeIf(entry -> entry.getValue().equals(peerId));
        log.info("Session unregistered: peerId={}", peerId);
    }

    /**
     * 根据peerId获取Channel
     */
    public Channel getChannel(String peerId) {
        return peerChannelMap.get(peerId);
    }

    /**
     * 根据Channel获取peerId
     */
    public String getPeerId(Channel channel) {
        return channelPeerMap.get(channel);
    }

    /**
     * 检查是否已认证
     */
    public boolean isAuthenticated(Channel channel) {
        return channelPeerMap.containsKey(channel);
    }

    /**
     * 获取所有已认证的Channel
     */
    public Collection<Channel> getAllChannels() {
        return peerChannelMap.values();
    }

    /**
     * 获取已认证节点数量
     */
    public int getAuthenticatedCount() {
        return peerChannelMap.size();
    }

    public boolean isClientOnline(String peerId) {
        Channel channel = peerChannelMap.get(peerId);
        return channel != null && channel.isActive();
    }
}
