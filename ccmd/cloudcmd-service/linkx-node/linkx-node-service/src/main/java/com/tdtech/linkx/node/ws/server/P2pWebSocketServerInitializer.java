package com.tdtech.linkx.node.ws.server;

import com.tdtech.linkx.node.service.ICoopShareMessageHandler;
import com.tdtech.linkx.node.service.IWsAuthGrantMessageHandler;
import com.tdtech.linkx.node.util.ChallengeManager;
import com.tdtech.linkx.node.util.P2pJwtUtil;
import com.tdtech.linkx.node.service.IPeerNodeClientService;
import com.tdtech.linkx.node.service.IPeerNodeStatusService;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.BiConsumer;

@Slf4j
@RequiredArgsConstructor
public class P2pWebSocketServerInitializer extends ChannelInitializer<SocketChannel> {

    private static final String WEBSOCKET_PATH = "/ws";

    private final ChallengeManager challengeManager;
    private final P2pJwtUtil p2pJwtUtil;
    private final SessionManager sessionManager;
    private final IPeerNodeClientService clientService;
    private final IPeerNodeStatusService peerNodeStatusService;
    private final ICoopShareMessageHandler coopShareMessageHandler;
    private final IWsAuthGrantMessageHandler authGrantMessageHandler;

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        pipeline.addLast("logging", new LoggingHandler(LogLevel.DEBUG));
        pipeline.addLast("httpCodec", new HttpServerCodec());
        pipeline.addLast("chunkedWrite", new ChunkedWriteHandler());
        pipeline.addLast("httpAggregator", new HttpObjectAggregator(65536));
        pipeline.addLast("wsCompression", new WebSocketServerCompressionHandler());

        P2pWebSocketServerHandler messageHandler = getP2pWebSocketServerHandler();

        pipeline.addLast("handshakeHandler", new WebSocketHandshakeHandler(messageHandler));
        pipeline.addLast("messageHandler", messageHandler);

        log.info("WebSocket pipeline initialized: {}", pipeline.names());
    }

    @NotNull
    private P2pWebSocketServerHandler getP2pWebSocketServerHandler() {
        BiConsumer<String, Map<String, Object>> shareHandler = coopShareMessageHandler::handleShare;
        BiConsumer<String, Map<String, Object>> unshareHandler = coopShareMessageHandler::handleUnshare;
        BiConsumer<String, Map<String, Object>> authGrantHandler = authGrantMessageHandler::handleAuthGrant;
        BiConsumer<String, Map<String, Object>> authRevokeHandler = authGrantMessageHandler::handleAuthRevoke;

        return new P2pWebSocketServerHandler(
                challengeManager,
                p2pJwtUtil,
                sessionManager,
                clientService,
                peerNodeStatusService,
                shareHandler,
                unshareHandler,
                authGrantHandler,
                authRevokeHandler
        );
    }
}
