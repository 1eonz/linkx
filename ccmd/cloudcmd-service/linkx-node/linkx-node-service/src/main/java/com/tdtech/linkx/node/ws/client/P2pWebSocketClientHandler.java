package com.tdtech.linkx.node.ws.client;

import com.tdtech.cloudcmd.util.json.JsonUtil;
import com.tdtech.linkx.node.config.NodeProperties;
import com.tdtech.linkx.node.enums.AuthFailReasonEnum;
import com.tdtech.linkx.node.enums.NodeStatusEnum;
import com.tdtech.linkx.node.enums.WsMessageSubTypeEnum;
import com.tdtech.linkx.node.enums.WsMessageTypeEnum;
import com.tdtech.linkx.node.service.ICoopShareMessageHandler;
import com.tdtech.linkx.node.service.IPeerNodeStatusService;
import com.tdtech.linkx.node.service.IWsAuthGrantMessageHandler;
import com.tdtech.linkx.node.util.P2pJwtUtil;
import com.tdtech.linkx.node.ws.WsMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
public class P2pWebSocketClientHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private final P2pJwtUtil p2pJwtUtil;
    private final NodeProperties nodeProperties;
    private final IPeerNodeStatusService peerNodeStatusService;
    private final ICoopShareMessageHandler coopShareMessageHandler;
    private final IWsAuthGrantMessageHandler authGrantMessageHandler;
    private final String peerId;
    private final String localPeerId;
    private final String localIp;
    private final int localPort;
    private final Consumer<String> onAuthSuccess;

    private volatile String pendingChallenge;
    private volatile ChannelHandlerContext currentCtx;

    public P2pWebSocketClientHandler(P2pJwtUtil p2pJwtUtil,
                                     NodeProperties nodeProperties,
                                     IPeerNodeStatusService peerNodeStatusService,
                                     ICoopShareMessageHandler coopShareMessageHandler,
                                     IWsAuthGrantMessageHandler authGrantMessageHandler,
                                     String peerId,
                                     String localPeerId,
                                     String localIp,
                                     int localPort,
                                     Consumer<String> onAuthSuccess) {
        this.p2pJwtUtil = p2pJwtUtil;
        this.nodeProperties = nodeProperties;
        this.peerNodeStatusService = peerNodeStatusService;
        this.coopShareMessageHandler = coopShareMessageHandler;
        this.authGrantMessageHandler = authGrantMessageHandler;
        this.peerId = peerId;
        this.localPeerId = localPeerId;
        this.localIp = localIp;
        this.localPort = localPort;
        this.onAuthSuccess = onAuthSuccess;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("Connected to server: {}", ctx.channel().remoteAddress());
        this.currentCtx = ctx;
        peerNodeStatusService.updateStatus(peerId, NodeStatusEnum.CONNECTING.getCode());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("Disconnected from server: peerId={}", peerId);
        super.channelInactive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame webSocketFrame) throws Exception {
        if (webSocketFrame instanceof TextWebSocketFrame) {
            String text = ((TextWebSocketFrame) webSocketFrame).text();
            handleTextMessage(ctx, text);
        }
    }

    private void handleTextMessage(ChannelHandlerContext ctx, String text) {
        try {
            WsMessage<?> message = JsonUtil.parseJson(text, WsMessage.class);
            if (message == null) {
                log.warn("message is null, return.");
                return;
            }

            String type = message.getType();
            String subType = message.getSubType();

            if (WsMessageTypeEnum.REGISTER.getType().equals(type)) {
                handleRegisterMessage(ctx, subType, message);
            } else if (WsMessageTypeEnum.PONG.getType().equals(type)) {
                handlePong(ctx, message);
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

    private void handleRegisterMessage(ChannelHandlerContext ctx, String subType, WsMessage<?> message) {
        if (WsMessageSubTypeEnum.REQUEST.getType().equals(subType)) {
            handleRegisterRequest(ctx, message);
        } else if (WsMessageSubTypeEnum.SUCCESS.getType().equals(subType)) {
            handleRegisterSuccess(ctx, message);
        } else if (WsMessageSubTypeEnum.FAILURE.getType().equals(subType)) {
            handleRegisterFailure(ctx, message);
        } else {
            log.warn("Unknown register subType: {}", subType);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleRegisterRequest(ChannelHandlerContext ctx, WsMessage<?> message) {
        Map<String, Object> payload = (Map<String, Object>) message.getPayload();
        String challenge = (String) payload.get("challenge");
        this.pendingChallenge = challenge;
        log.info("Received challenge from server: peerId={}, challenge={}, msgId={}", peerId, challenge, message.getMsgId());

        String token = p2pJwtUtil.generateToken(localPeerId, localIp, localPort, challenge);
        WsMessage<Map<String, Object>> response = WsMessage.registerResponse(token, message.getMsgId());
        sendMessage(ctx, response);
        log.info("Sent JWT to server: peerId={}, msgId={}", peerId, message.getMsgId());
    }

    @SuppressWarnings("unchecked")
    private void handleRegisterSuccess(ChannelHandlerContext ctx, WsMessage<?> message) {
        Map<String, Object> payload = (Map<String, Object>) message.getPayload();
        String sessionId = (String) payload.get("sessionId");
        log.info("Authentication successful: peerId={}, sessionId={}", peerId, sessionId);
        peerNodeStatusService.updateStatus(peerId, NodeStatusEnum.AUTHENTICATED.getCode());
        peerNodeStatusService.updateSession(peerId, sessionId);
        // 认证成功即视为"活跃"，避免首个心跳周期内被 HeartbeatMonitor 误判为超时
        peerNodeStatusService.updateLastSeen(peerId);

        if (onAuthSuccess != null) {
            onAuthSuccess.accept(sessionId);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleRegisterFailure(ChannelHandlerContext ctx, WsMessage<?> message) {
        Map<String, Object> payload = (Map<String, Object>) message.getPayload();
        String reason = (String) payload.get("reason");
        String reasonCode = (String) payload.get("reasonCode");

        log.warn("Authentication failed: peerId={}, reason={}, code={}", peerId, reason, reasonCode);
        AuthFailReasonEnum reasonEnum = AuthFailReasonEnum.fromCode(reasonCode);
        int retryStrategy = reasonEnum.getRetryStrategy();

        peerNodeStatusService.updateStatus(peerId, NodeStatusEnum.AUTH_FAILED.getCode());
        peerNodeStatusService.updateRetryStrategy(peerId, retryStrategy);
        if (reason != null) {
            peerNodeStatusService.updateAuthFail(peerId, reason);
        }
        ctx.channel().close();
    }

    private void handlePong(ChannelHandlerContext ctx, WsMessage<?> message) {
        peerNodeStatusService.updateLastSeen(peerId);
        log.debug("Received pong from server: peerId={}, msgId={}", peerId, message.getMsgId());
    }

    @SuppressWarnings("unchecked")
    private void handleMessage(ChannelHandlerContext ctx, WsMessage<?> message) {
        log.info("Received message from server: peerId={}, subType={}, payload={}", peerId, message.getSubType(), message.getPayload());

        String subType = message.getSubType();
        if (subType == null) {
            log.warn("Message subType is null, ignore");
            return;
        }

        Map<String, Object> payload = (Map<String, Object>) message.getPayload();
        if (payload == null) {
            payload = new HashMap<>();
        }

        // share/unshare/auth/unauth 子类型由业务处理器处理（单向通知，不要求 token）
        if (WsMessageSubTypeEnum.SHARE.getType().equals(subType)) {
            coopShareMessageHandler.handleShare(peerId, payload);
            return;
        }
        if (WsMessageSubTypeEnum.UNSHARE.getType().equals(subType)) {
            coopShareMessageHandler.handleUnshare(peerId, payload);
            return;
        }
        if (WsMessageSubTypeEnum.AUTH.getType().equals(subType)) {
            authGrantMessageHandler.handleAuthGrant(peerId, payload);
            return;
        }
        if (WsMessageSubTypeEnum.UNAUTH.getType().equals(subType)) {
            authGrantMessageHandler.handleAuthRevoke(peerId, payload);
            return;
        }

        String token = (String) payload.get("token");
        if (token != null) {
            handleServerRequest(ctx, subType, token, payload, message.getMsgId());
        } else {
            // WebSocket 查询响应已废弃，跨节点数据查询统一走 HTTP dispatch/proxy
            log.warn("Received deprecated open data response from server: peerId={}, subType={}", peerId, subType);
        }
    }

    private void handleServerRequest(ChannelHandlerContext ctx, String subType, String token, Map<String, Object> payload, String msgId) {
        if (!p2pJwtUtil.validateToken(token)) {
            sendErrorResponse(ctx, subType, "Token无效或已过期", msgId);
            return;
        }
        // WebSocket 查询接口已废弃，跨节点数据查询统一走 HTTP dispatch/proxy
        sendErrorResponse(ctx, subType, "该接口已废弃，请使用 HTTP dispatch/proxy 通道", msgId);
    }

    private void sendErrorResponse(ChannelHandlerContext ctx, String subType, String errorMsg, String msgId) {
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("success", false);
        responseData.put("error", errorMsg);

        WsMessage<Map<String, Object>> response = WsMessage.openDataResponse(subType, responseData, msgId);
        sendMessage(ctx, response);
    }

    private void handleDisconnect(ChannelHandlerContext ctx, WsMessage<?> message) {
        log.info("Server disconnecting: peerId={}, msg={}", peerId, message.getMsg());
        ctx.channel().close();
    }

    private void sendMessage(ChannelHandlerContext ctx, WsMessage<?> message) {
        String json = JsonUtil.toJsonStr(message);
        ctx.channel().writeAndFlush(new TextWebSocketFrame(json));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("WebSocket client exception: peerId={}", peerId, cause);
        ctx.close();
    }
}
