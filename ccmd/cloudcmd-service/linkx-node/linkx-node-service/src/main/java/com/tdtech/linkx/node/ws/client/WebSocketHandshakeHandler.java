package com.tdtech.linkx.node.ws.client;

import com.tdtech.linkx.node.config.NodeProperties;
import com.tdtech.linkx.node.service.ICoopShareMessageHandler;
import com.tdtech.linkx.node.service.IPeerNodeStatusService;
import com.tdtech.linkx.node.service.IWsAuthGrantMessageHandler;
import com.tdtech.linkx.node.util.P2pJwtUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.function.Consumer;

@Slf4j
public class WebSocketHandshakeHandler extends SimpleChannelInboundHandler<Object> {

    private final WebSocketClientHandshaker handshaker;
    private final String peerId;
    private final Consumer<String> onAuthSuccess;
    private final P2pJwtUtil p2pJwtUtil;
    private final NodeProperties nodeProperties;
    private final IPeerNodeStatusService peerNodeStatusService;
    private final IWsAuthGrantMessageHandler authGrantMessageHandler;
    private final ICoopShareMessageHandler coopShareMessageHandler;
    private final String localPeerId;
    private final String localIp;
    private final Integer localPort;
    private final P2pWebSocketClient client;
    private ChannelPromise handshakePromise;

    public WebSocketHandshakeHandler(URI uri, String peerId, Consumer<String> onAuthSuccess,
                                     P2pJwtUtil p2pJwtUtil, NodeProperties nodeProperties,
                                     IPeerNodeStatusService peerNodeStatusService,
                                     IWsAuthGrantMessageHandler authGrantMessageHandler,
                                     ICoopShareMessageHandler coopShareMessageHandler,
                                     String localPeerId, String localIp, Integer localPort,
                                     P2pWebSocketClient client) {
        this.handshaker = WebSocketClientHandshakerFactory.newHandshaker(
                uri, WebSocketVersion.V13, null, true, null, 65536);
        this.peerId = peerId;
        this.onAuthSuccess = onAuthSuccess;
        this.p2pJwtUtil = p2pJwtUtil;
        this.nodeProperties = nodeProperties;
        this.peerNodeStatusService = peerNodeStatusService;
        this.authGrantMessageHandler = authGrantMessageHandler;
        this.coopShareMessageHandler = coopShareMessageHandler;
        this.localPeerId = localPeerId;
        this.localIp = localIp;
        this.localPort = localPort;
        this.client = client;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        handshakePromise = ctx.newPromise();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        handshaker.handshake(ctx.channel());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        Channel ch = ctx.channel();
        if (!handshaker.isHandshakeComplete()) {
            handshaker.finishHandshake(ch, (FullHttpResponse) msg);
            handshakePromise.setSuccess();

            ctx.pipeline().addLast(new HeartbeatHandler(nodeProperties, client, peerId));
            P2pWebSocketClientHandler handler = new P2pWebSocketClientHandler(
                    p2pJwtUtil, nodeProperties, peerNodeStatusService,
                    coopShareMessageHandler, authGrantMessageHandler,
                    peerId, localPeerId, localIp, localPort, onAuthSuccess
            );
            ctx.pipeline().addLast(handler);
            ctx.pipeline().remove(this);

            client.registerHandler(peerId, handler);

            log.info("WebSocket handshake completed: peerId={}", peerId);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("WebSocket handshake failed: peerId={}", peerId, cause);
        if (!handshakePromise.isDone()) {
            handshakePromise.setFailure(cause);
        }
        ctx.close();
    }
}
