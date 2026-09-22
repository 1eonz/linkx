package com.tdtech.linkx.node.ws.client;


import com.tdtech.cloudcmd.util.json.JsonUtil;
import com.tdtech.linkx.node.config.NodeProperties;
import com.tdtech.linkx.node.entity.PeerNodeServer;
import com.tdtech.linkx.node.enums.NodeStatusEnum;
import com.tdtech.linkx.node.service.ICoopShareMessageHandler;
import com.tdtech.linkx.node.service.IPeerNodeStatusService;
import com.tdtech.linkx.node.service.IWsAuthGrantMessageHandler;
import com.tdtech.linkx.node.util.P2pJwtUtil;
import com.tdtech.linkx.node.ws.WsMessage;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.concurrent.ScheduledFuture;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class P2pWebSocketClient {

    private final P2pJwtUtil p2pJwtUtil;
    private final NodeProperties nodeProperties;
    private final IPeerNodeStatusService peerNodeStatusService;
    private final IWsAuthGrantMessageHandler authGrantMessageHandler;
    private final ICoopShareMessageHandler coopShareMessageHandler;
    private final NioEventLoopGroup eventLoopGroup;
    
    @Getter
    private String localPeerId;
    @Getter
    private String localIp;
    @Getter
    private int localPort;

    @Getter
    private final Map<String, Channel> channels = new ConcurrentHashMap<>();

    @Getter
    private final Map<String, ScheduledFuture<?>> heartbeatTasks = new ConcurrentHashMap<>();

    @Getter
    private final Map<String, P2pWebSocketClientHandler> handlers = new ConcurrentHashMap<>();

    public void setLocalNode(String peerId, String ip, int port) {
        this.localPeerId = peerId;
        this.localIp = ip;
        this.localPort = port;
    }

    public void connectToServer(PeerNodeServer server, Consumer<String> onAuthSuccess) {
        String peerId = server.getPeerId();

        Channel existing = channels.get(peerId);
        if (existing != null && existing.isActive()) {
            log.info("Already connected to server: peerId={}", peerId);
            return;
        }

        String uriStr = nodeProperties.getLocal().buildWsUrl(server.getIp(), localPeerId);
        URI uri = URI.create(uriStr);

        try {
            ChannelInitializer<SocketChannel> initializer = new ChannelInitializer<>() {
                @Override
                protected void initChannel(SocketChannel ch) throws SSLException {
                    ChannelPipeline pipeline = ch.pipeline();

                    if ("wss".equalsIgnoreCase(uri.getScheme())) {
                        SslContext sslContext = SslContextBuilder.forClient()
                                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                                .build();
                        pipeline.addLast(sslContext.newHandler(ch.alloc()));
                    }

                    pipeline.addLast(new HttpClientCodec());
                    pipeline.addLast(new HttpObjectAggregator(65536));
                    pipeline.addLast(WebSocketClientCompressionHandler.INSTANCE);
                    pipeline.addLast(new WebSocketHandshakeHandler(uri, peerId, onAuthSuccess,
                            p2pJwtUtil, nodeProperties, peerNodeStatusService,
                            authGrantMessageHandler, coopShareMessageHandler,
                            localPeerId, localIp, localPort, P2pWebSocketClient.this));
                }
            };

            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(eventLoopGroup)
                    .channel(NioSocketChannel.class)
                    .handler(initializer);
            Channel channel = bootstrap.connect(uri.getHost(), uri.getPort()).syncUninterruptibly().channel();
            channels.put(peerId, channel);

            log.info("Connecting to server: peerId={}, uri={}", peerId, uriStr);

        } catch (Exception e) {
            log.error("Failed to connect to server: peerId={}", peerId, e);
            peerNodeStatusService.updateStatus(peerId, NodeStatusEnum.DISCONNECTED.getCode());
        }
    }

    public void disconnectAll() {
        for (String peerId : channels.keySet()) {
            disconnect(peerId);
        }
    }

    public void disconnect(String peerId) {
        Channel channel = channels.remove(peerId);
        if (channel != null && channel.isActive()) {
            WsMessage<Object> disconnect = WsMessage.disconnect("客户端主动断开");
            String json = JsonUtil.toJsonStr(disconnect);
            channel.writeAndFlush(new TextWebSocketFrame(json));
            channel.close();
        }

        ScheduledFuture<?> heartbeatTask = heartbeatTasks.remove(peerId);
        if (heartbeatTask != null) {
            heartbeatTask.cancel(false);
        }

        handlers.remove(peerId);
        log.info("Disconnected from server: peerId={}", peerId);
    }

    public Channel getChannel(String peerId) {
        return channels.get(peerId);
    }

    public boolean isActive(String peerId) {
        Channel channel = channels.get(peerId);
        return channel != null && channel.isActive();
    }

    public void registerHandler(String peerId, P2pWebSocketClientHandler handler) {
        handlers.put(peerId, handler);
    }
}
