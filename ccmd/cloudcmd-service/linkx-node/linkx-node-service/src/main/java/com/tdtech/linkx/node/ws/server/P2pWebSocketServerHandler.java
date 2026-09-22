package com.tdtech.linkx.node.ws.server;

import com.tdtech.cloudcmd.util.json.JsonUtil;
import com.tdtech.linkx.node.constants.Constants;
import com.tdtech.linkx.node.enums.WsMessageSubTypeEnum;
import com.tdtech.linkx.node.util.ChallengeManager;
import com.tdtech.linkx.node.util.P2pJwtUtil;
import com.tdtech.linkx.node.entity.PeerNodeClient;
import com.tdtech.linkx.node.enums.NodeStatusEnum;
import com.tdtech.linkx.node.enums.WsMessageTypeEnum;
import com.tdtech.linkx.node.service.IPeerNodeClientService;
import com.tdtech.linkx.node.service.IPeerNodeStatusService;
import com.tdtech.linkx.node.ws.WsMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

@Slf4j
public class P2pWebSocketServerHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private final ChallengeManager challengeManager;
    private final P2pJwtUtil p2pJwtUtil;
    private final SessionManager sessionManager;
    private final IPeerNodeClientService clientService;
    private final IPeerNodeStatusService peerNodeStatusService;

    /**
     * 协同岗分享消息处理器（peerId, payload）
     */
    private final BiConsumer<String, Map<String, Object>> shareMessageHandler;

    /**
     * 取消协同岗分享消息处理器（peerId, payload）
     */
    private final BiConsumer<String, Map<String, Object>> unshareMessageHandler;

    /**
     * 开放数据授权通知处理器（peerId, payload）
     */
    private final BiConsumer<String, Map<String, Object>> authGrantMessageHandler;

    /**
     * 开放数据取消授权通知处理器（peerId, payload）
     */
    private final BiConsumer<String, Map<String, Object>> authRevokeMessageHandler;

    private String pendingPeerId;

    public P2pWebSocketServerHandler(ChallengeManager challengeManager,
                                     P2pJwtUtil p2pJwtUtil,
                                     SessionManager sessionManager,
                                     IPeerNodeClientService clientService,
                                     IPeerNodeStatusService peerNodeStatusService,
                                     BiConsumer<String, Map<String, Object>> shareMessageHandler,
                                     BiConsumer<String, Map<String, Object>> unshareMessageHandler,
                                     BiConsumer<String, Map<String, Object>> authGrantMessageHandler,
                                     BiConsumer<String, Map<String, Object>> authRevokeMessageHandler) {
        this.challengeManager = challengeManager;
        this.p2pJwtUtil = p2pJwtUtil;
        this.sessionManager = sessionManager;
        this.clientService = clientService;
        this.peerNodeStatusService = peerNodeStatusService;
        this.shareMessageHandler = shareMessageHandler;
        this.unshareMessageHandler = unshareMessageHandler;
        this.authGrantMessageHandler = authGrantMessageHandler;
        this.authRevokeMessageHandler = authRevokeMessageHandler;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("New connection from: {}", ctx.channel().remoteAddress());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String peerId = sessionManager.getPeerId(ctx.channel());
        if (peerId != null) {
            sessionManager.unregister(peerId);
            log.info("Connection closed: peerId={}", peerId);
        } else if (pendingPeerId != null) {
            peerNodeStatusService.updateStatus(pendingPeerId, NodeStatusEnum.DISCONNECTED.getCode());
            log.info("Connection closed before auth: peerId={}", pendingPeerId);
        }
        super.channelInactive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        if (frame instanceof TextWebSocketFrame) {
            String text = ((TextWebSocketFrame) frame).text();
            handleTextMessage(ctx, text);
        }
    }

    private void handleTextMessage(ChannelHandlerContext ctx, String text) {
        try {
            WsMessage<?> message = JsonUtil.parseJson(text, WsMessage.class);
            if (message == null) {
                return;
            }
            String type = message.getType();

            if (WsMessageTypeEnum.REGISTER.getType().equals(type)) {
                handleRegisterMessage(ctx, message);
            } else if (WsMessageTypeEnum.PING.getType().equals(type)) {
                handlePing(ctx, message);
            } else if (WsMessageTypeEnum.MESSAGE.getType().equals(type)) {
                handleMessage(ctx, message);
            } else if (WsMessageTypeEnum.DISCONNECT.getType().equals(type)) {
                handleDisconnect(ctx, message);
            } else {
                log.warn("Unknown message type: {}", type);
            }
        } catch (Exception e) {
            log.error("Failed to handle message: {}", text, e);
        }
    }

    private void handleRegisterMessage(ChannelHandlerContext ctx, WsMessage<?> message) {
        String subType = message.getSubType();
        if (WsMessageSubTypeEnum.RESPONSE.getType().equals(subType)) {
            handleRegisterResponse(ctx, message);
        } else {
            log.warn("Unexpected register subType: {}", subType);
        }
    }

    private void handleRegisterResponse(ChannelHandlerContext ctx, WsMessage<?> message) {
        Map<String, Object> payload = (Map<String, Object>) message.getPayload();
        if (payload == null) {
            sendRegisterFailure(ctx, "payload为空", message.getMsgId());
            return;
        }
        String token = (String) payload.get("token");
        try {
            var claims = p2pJwtUtil.parseToken(token);
            String peerId = claims.get("peerId", String.class);
            String challenge = claims.get("challenge", String.class);

            if (!challengeManager.verifyAndConsume(peerId, challenge)) {
                sendRegisterFailure(ctx, "挑战码无效", message.getMsgId());
                peerNodeStatusService.updateStatus(peerId, NodeStatusEnum.AUTH_FAILED.getCode());
                return;
            }

            String sessionId = UUID.randomUUID().toString();
            sessionManager.register(sessionId, peerId, ctx.channel());

            WsMessage<Map<String, Object>> success = WsMessage.registerSuccess(sessionId, peerId, message.getMsgId());
            sendMessage(ctx, success);

            peerNodeStatusService.updateStatus(peerId, NodeStatusEnum.AUTHENTICATED.getCode());
            peerNodeStatusService.updateSession(peerId, sessionId);
            peerNodeStatusService.updateJwt(peerId, token);
            // 认证成功即视为"活跃"，避免首个心跳周期内被 HeartbeatMonitor 误判为超时
            peerNodeStatusService.updateLastSeen(peerId);

            log.info("Client authenticated: peerId={}, sessionId={}, msgId={}", peerId, sessionId, message.getMsgId());

        } catch (Exception e) {
            log.error("JWT verification failed", e);
            sendRegisterFailure(ctx, "JWT验证失败: " + e.getMessage(), message.getMsgId());
        }
    }

    private void handlePing(ChannelHandlerContext ctx, WsMessage<?> message) {
        String peerId = sessionManager.getPeerId(ctx.channel());
        if (peerId == null) {
            return;
        }

        PeerNodeClient client = clientService.getByPeerId(peerId);
        if (client != null && client.getGrant() != null) {
            if (client.getGrant() == 1 && client.getExpiredIn() != null && client.getExpiredIn().isBefore(LocalDateTime.now())) {
                log.warn("Client authorization expired, disconnecting: peerId={}, expiredIn={}",
                        peerId, client.getExpiredIn());
                sendRegisterFailure(ctx, "授权已过期", message.getMsgId());
                peerNodeStatusService.updateStatus(peerId, NodeStatusEnum.AUTH_FAILED.getCode());
                return;
            } else if (client.getGrant() == 0 || client.getGrant() == 2) {
                sendRegisterFailure(ctx, "节点未授权或已拒绝", message.getMsgId());
                peerNodeStatusService.updateStatus(peerId, NodeStatusEnum.AUTH_FAILED.getCode());
                return;
            }
        }

        sendMessage(ctx, WsMessage.pong(message.getMsgId()));
        log.debug("Sent pong to client: peerId={}, msgId={}", peerId, message.getMsgId());

        peerNodeStatusService.updateStatus(peerId, NodeStatusEnum.AUTHENTICATED.getCode());
        peerNodeStatusService.updateLastSeen(peerId);
    }

    private void handleDisconnect(ChannelHandlerContext ctx, WsMessage<?> message) {
        String peerId = sessionManager.getPeerId(ctx.channel());
        if (peerId == null) {
            log.warn("Received disconnect message from unauthenticated channel");
            return;
        }

        log.info("Received disconnect message from peerId={}, msg={}", peerId, message.getMsg());
        sessionManager.unregister(peerId);
        ctx.channel().close();
        log.info("Client disconnected: peerId={}", peerId);
    }

    @SuppressWarnings("unchecked")
    private void handleMessage(ChannelHandlerContext ctx, WsMessage<?> message) {
        String peerId = sessionManager.getPeerId(ctx.channel());
        if (peerId == null) {
            log.warn("Received message from unauthenticated channel");
            return;
        }

        log.info("Received message from peerId={}, subType={}", peerId, message.getSubType());

        String subType = message.getSubType();
        if (subType == null) {
            log.warn("Message subType is null, ignore");
            return;
        }

        Map<String, Object> payload = (Map<String, Object>) message.getPayload();
        if (payload == null) {
            payload = new HashMap<>();
        }

        // share/unshare/auth/unauth 子类型由业务处理器处理（不要求 token，单向通知）
        if (WsMessageSubTypeEnum.SHARE.getType().equals(subType)) {
            shareMessageHandler.accept(peerId, payload);
            return;
        }
        if (WsMessageSubTypeEnum.UNSHARE.getType().equals(subType)) {
            unshareMessageHandler.accept(peerId, payload);
            return;
        }
        if (WsMessageSubTypeEnum.AUTH.getType().equals(subType)) {
            authGrantMessageHandler.accept(peerId, payload);
            return;
        }
        if (WsMessageSubTypeEnum.UNAUTH.getType().equals(subType)) {
            authRevokeMessageHandler.accept(peerId, payload);
            return;
        }
    }

    public void sendChallenge(ChannelHandlerContext ctx, String peerId) {
        this.pendingPeerId = peerId;

        String challenge = challengeManager.generateChallenge(peerId);

        peerNodeStatusService.updateStatus(peerId, NodeStatusEnum.AUTHENTICATING.getCode());

        WsMessage<Map<String, Object>> registerReq = WsMessage.registerRequest(challenge, Constants.EXPIRE_SECONDS);
        sendMessage(ctx, registerReq);

        log.info("Challenge sent to peerId={}", peerId);
    }

    private void sendRegisterFailure(ChannelHandlerContext ctx, String reason, String msgId) {
        WsMessage<Map<String, Object>> failure = WsMessage.registerFailure(reason, msgId);
        sendMessage(ctx, failure);
        ctx.channel().close();
    }

    private void sendMessage(ChannelHandlerContext ctx, WsMessage<?> message) {
        String json = JsonUtil.toJsonStr(message);
        ctx.channel().writeAndFlush(new TextWebSocketFrame(json));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("P2pWebSocketServerHandler.exceptionCaught: peerId={}, error={}",
                sessionManager.getPeerId(ctx.channel()), cause.getMessage(), cause);
        ctx.close();
    }
}
