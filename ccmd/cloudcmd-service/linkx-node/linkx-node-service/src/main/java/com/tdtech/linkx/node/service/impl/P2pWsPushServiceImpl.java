package com.tdtech.linkx.node.service.impl;

import com.tdtech.cloudcmd.util.json.JsonUtil;
import com.tdtech.linkx.node.config.NodeProperties;
import com.tdtech.linkx.node.service.IP2pWsPushService;
import com.tdtech.linkx.node.ws.WsMessage;
import com.tdtech.linkx.node.ws.client.P2pWebSocketClient;
import com.tdtech.linkx.node.ws.server.SessionManager;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * P2P WebSocket 消息推送 Service 实现
 * - 服务端推送：通过 SessionManager 获取客户端 Channel（对端作为 client 连入本节点）
 * - 客户端推送：通过 P2pWebSocketClient 获取到服务端的 Channel（本节点作为 client 连出到对端）
 * - 离线补偿：推送失败时暂存 Redis，节点上线后补推
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class P2pWsPushServiceImpl implements IP2pWsPushService {

    /**
     * Redis 暂存离线消息的 Key 前缀
     */
    private static final String PENDING_PUSH_KEY_PREFIX = "coop:pending:push:";

    /**
     * 离线消息最长保留时间（7 天）
     */
    private static final long PENDING_PUSH_TTL_DAYS = 7L;

    private final SessionManager sessionManager;
    private final P2pWebSocketClient p2pWebSocketClient;
    private final StringRedisTemplate redisTemplate;
    private final NodeProperties nodeProperties;

    @Override
    public boolean pushShare(String peerId, Long coopUserId, String originPeerId,
                             String targetPeerId, Long targetOrgId,
                             String coopUserName, String iconUrl, Long orgId, String orgName) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("coopUserId", coopUserId);
        payload.put("originPeerId", originPeerId);
        payload.put("fromPeerName", nodeProperties.getLocal().getName());
        payload.put("targetPeerId", targetPeerId);
        payload.put("targetOrgId", targetOrgId);
        payload.put("coopUserName", coopUserName);
        payload.put("iconUrl", iconUrl);
        payload.put("orgId", orgId);
        payload.put("orgName", orgName);

        WsMessage<Map<String, Object>> message = WsMessage.share(payload);
        return push(peerId, message);
    }

    @Override
    public boolean pushUnshare(String peerId, Long coopUserId, String originPeerId, Long targetOrgId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("coopUserId", coopUserId);
        payload.put("originPeerId", originPeerId);
        payload.put("targetOrgId", targetOrgId);

        WsMessage<Map<String, Object>> message = WsMessage.unshare(payload);
        return push(peerId, message);
    }

    @Override
    public boolean pushAuthGrant(String peerId, String grantFromPeerId, String grantToPeerId,
                                  String permission, String description, Long expiredTime, Long grantTime) {
        WsMessage<Map<String, Object>> message = WsMessage.auth(
                grantFromPeerId, grantToPeerId, permission, description, expiredTime, grantTime);
        return push(peerId, message);
    }

    @Override
    public boolean pushAuthRevoke(String peerId, String grantFromPeerId, String grantToPeerId,
                                   String permission, String description, Long grantTime) {
        WsMessage<Map<String, Object>> message = WsMessage.unauth(
                grantFromPeerId, grantToPeerId, permission, description, grantTime);
        return push(peerId, message);
    }

    /**
     * 统一推送方法：先尝试服务端通道（对端连入），再尝试客户端通道（本端连出）
     * 都失败则暂存 Redis
     */
    private boolean push(String peerId, WsMessage<?> message) {
        String json = JsonUtil.toJsonStr(message);
        TextWebSocketFrame frame = new TextWebSocketFrame(json);

        // 尝试服务端通道（对端作为 client 连入本节点）
        if (pushViaServer(peerId, frame)) {
            log.info("push: sent via server channel, peerId={}, subType={}", peerId, message.getSubType());
            return true;
        }

        // 尝试客户端通道（本节点作为 client 连出到对端）
        if (pushViaClient(peerId, frame)) {
            log.info("push: sent via client channel, peerId={}, subType={}", peerId, message.getSubType());
            return true;
        }

        // 离线暂存 Redis
        cachePendingMessage(peerId, json);
        log.warn("push: peer offline, cached in redis, peerId={}, subType={}", peerId, message.getSubType());
        return false;
    }

    /**
     * 通过服务端 Channel 推送（对端作为 client 连入本节点）
     */
    private boolean pushViaServer(String peerId, TextWebSocketFrame frame) {
        Channel channel = sessionManager.getChannel(peerId);
        if (channel == null || !channel.isActive()) {
            return false;
        }
        channel.writeAndFlush(frame.copy());
        return true;
    }

    /**
     * 通过客户端 Channel 推送（本节点作为 client 连出到对端）
     */
    private boolean pushViaClient(String peerId, TextWebSocketFrame frame) {
        Channel channel = p2pWebSocketClient.getChannel(peerId);
        if (channel == null || !channel.isActive()) {
            return false;
        }
        channel.writeAndFlush(frame.copy());
        return true;
    }

    /**
     * 暂存离线消息到 Redis（List 结构）
     */
    private void cachePendingMessage(String peerId, String json) {
        try {
            String key = PENDING_PUSH_KEY_PREFIX + peerId;
            redisTemplate.opsForList().rightPush(key, json);
            redisTemplate.expire(key, PENDING_PUSH_TTL_DAYS, TimeUnit.DAYS);
        } catch (Exception e) {
            log.error("cachePendingMessage: failed, peerId={}", peerId, e);
        }
    }

    /**
     * 节点上线后补推暂存消息（供 register success 后调用）
     *
     * @param peerId 上线节点ID
     */
    public void pushPendingMessages(String peerId) {
        String key = PENDING_PUSH_KEY_PREFIX + peerId;
        List<String> messages = redisTemplate.opsForList().range(key, 0, -1);
        if (messages == null || messages.isEmpty()) {
            return;
        }

        Channel channel = sessionManager.getChannel(peerId);
        if (channel == null || !channel.isActive()) {
            channel = p2pWebSocketClient.getChannel(peerId);
        }
        if (channel == null || !channel.isActive()) {
            return;
        }

        for (String msg : messages) {
            channel.writeAndFlush(new TextWebSocketFrame(msg));
        }
        redisTemplate.delete(key);
        log.info("pushPendingMessages: pushed {} messages to peerId={}", messages.size(), peerId);
    }
}
