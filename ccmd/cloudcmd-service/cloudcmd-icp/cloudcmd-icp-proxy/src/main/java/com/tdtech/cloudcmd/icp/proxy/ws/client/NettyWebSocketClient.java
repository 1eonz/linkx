package com.tdtech.cloudcmd.icp.proxy.ws.client;

import com.tdtech.cloudcmd.icp.proxy.ws.event.WsHandshakeFinishedEvent;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.concurrent.ScheduledFuture;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;

import javax.annotation.PreDestroy;
import javax.net.ssl.SSLException;
import java.net.URI;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
public class NettyWebSocketClient implements WebSocketClient {
    // 链接URL
    @Getter
    private URI uri;
    // 次级消息处理器
    private final Supplier<WebSocketMessageHandler> messageHandlerSupplier;
    // netty启动器
    private final Bootstrap bootstrap;
    // 线程池、连接池
    private final EventLoopGroup group;
    // 当前链接 目前只支持一个，需要支持多个需要换成Channel容器
    private Channel channel;

    private final ApplicationEventPublisher eventPublisher;

    public NettyWebSocketClient(ApplicationEventPublisher eventPublisher, Supplier<WebSocketMessageHandler> messageHandlerSupplier) {
        Objects.requireNonNull(messageHandlerSupplier);
        this.eventPublisher = eventPublisher;
        this.messageHandlerSupplier = messageHandlerSupplier;
        this.group = new NioEventLoopGroup();
        this.bootstrap =
            new Bootstrap().group(group).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(1024 * 1024))// 这行配置比较重要
                .handler(new WebSocketChannelInitializer());
    }

    @Override
    public boolean isActive() {
        return this.channel != null && this.channel.isActive();
    }

    @Override
    public NettyWebSocketClient connect(URI uri) {
        if (this.channel != null && this.channel.isActive()) {
            return this;
        }
        this.uri=uri;
        this.channel = bootstrap.connect(uri.getHost(), uri.getPort()).syncUninterruptibly().channel();
        this.channel.pipeline().get(WebSocketHandshakeHandler.class).handshakePromise().syncUninterruptibly();
        log.debug("connect to {} succeed", uri);
        return this;
    }

    public Channel channel() {
        return this.channel;
    }

    @Override
    public ChannelFuture write(WebSocketFrame frame) {
        return channel.writeAndFlush(frame);
    }

    @Override
    public ChannelFuture writeSync(WebSocketFrame frame) {
        try {
            return channel.writeAndFlush(frame).sync();
        } catch (InterruptedException e) {
            log.error("write msg interrupted");
            Thread.currentThread().interrupt();
            throw new WebSocketWriteMessageInterruptedException();
        }
    }

    @Override
    public void closeChannel() {
        if (channel != null && channel.isActive()) {
            channel.close();
        }
    }

    public void shutdown() {
        if (channel != null && channel.isActive()) {
            channel.close();
        }
        group.shutdownGracefully();
    }

    @PreDestroy
    public void preDestroy() {
        shutdown();
    }

    public static class WebSocketChannelNotReadyException extends RuntimeException {
    }

    public static class WebSocketWriteMessageInterruptedException extends RuntimeException {
    }

    public class WebSocketChannelInitializer extends ChannelInitializer<SocketChannel> {

        @Override
        protected void initChannel(SocketChannel channel) throws SSLException {
            ChannelPipeline pipeline = channel.pipeline();
            if ("wss".equals(NettyWebSocketClient.this.uri.getScheme())) {
                SslContext context =
                    SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
                pipeline = pipeline.addLast(context.newHandler(channel.alloc()));
            }
            pipeline.addLast(new HttpClientCodec()).addLast(new HttpObjectAggregator(8192))
                .addLast(WebSocketClientCompressionHandler.INSTANCE).addLast(new WebSocketHandshakeHandler());
        }
    }

    public static class HeartBeatHandler extends ChannelInboundHandlerAdapter {

        private ScheduledFuture<?> schedule;
        private int callCounter = 0;
        private boolean isChannelInactive = false;

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
            setCheck(ctx);
            super.handlerAdded(ctx);
        }

        private void setCheck(ChannelHandlerContext ctx) {
            if (isChannelInactive) {
                return;
            }
            if (callCounter >= 5) {
                ctx.channel().close();
            }
            callCounter++;
            ctx.writeAndFlush(new PingWebSocketFrame());
            log.info("ping!!");
            schedule = ctx.executor().schedule(() -> setCheck(ctx), 10L, TimeUnit.SECONDS);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            isChannelInactive = true;
            schedule.cancel(false);
            super.channelInactive(ctx);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            callCounter = 0;
            super.channelRead(ctx, msg);
        }
    }

    public static class ExceptionHandler extends ChannelHandlerAdapter {

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.error("message error:", cause);
        }
    }

    /**
     * webSocket 协议实现</br> tcp连接建立后发送http请求握手，升级http协议为webSocket协议</br> 通过handshakePromise管理握手状态，true时成功，false时失败</br>
     * 握手成功后从管道中删除自己
     */
    public class WebSocketHandshakeHandler extends SimpleChannelInboundHandler<Object> {

        private final WebSocketClientHandshaker handshaker;
        private ChannelPromise handshakePromise;

        public WebSocketHandshakeHandler() {
            handshaker =
                WebSocketClientHandshakerFactory.newHandshaker(NettyWebSocketClient.this.uri, WebSocketVersion.V13,
                    null, true, new DefaultHttpHeaders(), 1024 * 1024);
        }

        public ChannelPromise handshakePromise() {
            return handshakePromise;
        }

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) {
            handshakePromise = ctx.newPromise();
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            // check encoder and decoder
            // fire exception when check failed
            handshaker.handshake(ctx.channel()).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
            ctx.fireChannelActive();
        }

        @Override
        public void channelRead0(ChannelHandlerContext ctx, Object msg) {
            Channel ch = ctx.channel();
            if (!handshaker.isHandshakeComplete()) {
                try {
                    handshaker.finishHandshake(ch, (FullHttpResponse)msg);
                    log.info("after handshake finished for connection uri:{}", uri.toString());
                    handshakePromise.setSuccess();
                    WebSocketMessageHandler webSocketMessageHandler =
                        NettyWebSocketClient.this.messageHandlerSupplier.get();
                    ctx.pipeline().remove(this).addFirst(new ExceptionHandler()).addLast(new HeartBeatHandler())
                        .addLast(webSocketMessageHandler);
                    eventPublisher.publishEvent(new WsHandshakeFinishedEvent(ctx.channel()));
                } catch (WebSocketHandshakeException e) {
                    log.debug("after handshake failed for connection uri:" + uri.toString(), e);
                    handshakePromise.setFailure(e);
                }
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.error("exception when do handshake ", cause);
            if (!handshakePromise.isDone()) {
                handshakePromise.setFailure(cause);
            }
            ctx.close();
        }
    }
}
