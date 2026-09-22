package com.tdtech.cloudcmd.im.jingxin.client.ws;

import java.net.URI;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;

import com.tdtech.cloudcmd.im.jingxin.client.NamedLogger;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImHeadersReq;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImToken;
import com.tdtech.cloudcmd.im.jingxin.client.utils.RandomUtil;
import com.tdtech.cloudcmd.util.json.JsonUtil;

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
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
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
import lombok.Setter;
import lombok.ToString;

public class WsClient implements NamedLogger {

    // netty启动器
    private final Bootstrap bootstrap;
    // 线程池、连接池
    private final EventLoopGroup group;
    // 当前链接 目前只支持一个，需要支持多个需要换成Channel容器
    private final WebSocketMessageHandler messageHandler;

    @Getter
    private final String name;

    // 链接URL
    private URI uri;
    private Channel channel;
    private ImToken token;

    private ImHeadersReq headersReq;
    private String rand;

    public WsClient(String name, WebSocketMessageHandler messageHandler) {
        Objects.requireNonNull(messageHandler);
        this.name = name;
        this.group = new NioEventLoopGroup();
        this.bootstrap =
            new Bootstrap().group(group).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(1024 * 1024))// 这行配置比较重要
                .handler(new WebSocketChannelInitializer());
        this.messageHandler = messageHandler;
    }

    public boolean isActive() {
        return this.channel != null && this.channel.isActive();
    }

    public void close() throws InterruptedException {
        this.channel.close().sync();
        this.channel = null;
    }

    public WsClient connect(URI uri, ImToken token, ImHeadersReq imHeadersReq, String rand) {
        if (this.channel != null && this.channel.isActive()) {
            return this;
        }
        this.uri = Objects.requireNonNull(uri);
        this.token = Objects.requireNonNull(token);
        this.headersReq = Objects.requireNonNull(imHeadersReq);
        this.rand = Objects.requireNonNull(rand);
        this.channel = bootstrap.connect(uri.getHost(), uri.getPort()).syncUninterruptibly().channel();
        this.channel.pipeline().get(WebSocketHandshakeHandler.class).handshakePromise().syncUninterruptibly();
        log("info", "connect to {} succeed", uri);
        return this;
    }

    public ChannelFuture write(WebSocketFrame frame) {
        return channel.writeAndFlush(frame);
    }

    public class WebSocketChannelInitializer extends ChannelInitializer<SocketChannel> {

        @Override
        protected void initChannel(SocketChannel channel) throws SSLException {
            ChannelPipeline pipeline = channel.pipeline();
            if ("wss".equals(WsClient.this.uri.getScheme())) {
                SslContext context =
                    SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
                pipeline = pipeline.addLast(context.newHandler(channel.alloc()));
            }
            pipeline.addLast(new HttpClientCodec()).addLast(new HttpObjectAggregator(8192))
                .addLast(WebSocketClientCompressionHandler.INSTANCE).addLast(new WebSocketHandshakeHandler());
        }
    }

    @Getter
    @Setter
    @ToString
    public static class HeartBeatMsg {
        private String module = "app";
        private String notifyType = "ping";
        private String id = RandomUtil.randomString();
    }

    public class HeartBeatHandler extends ChannelInboundHandlerAdapter {

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
                log("warn", "heartbeat fail");
                ctx.channel().close();
                return;
            }
            callCounter++;
            var jsonStr = JsonUtil.toJsonStr(new HeartBeatMsg());
            var frame = new TextWebSocketFrame(jsonStr);
            ctx.writeAndFlush(frame);
            log("info", "ping!!:{}", jsonStr);
            schedule = ctx.executor().schedule(() -> setCheck(ctx), 30L, TimeUnit.SECONDS);
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

    public class ExceptionHandler extends ChannelHandlerAdapter {

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log("error", "message error:", cause);
        }
    }

    /**
     * webSocket 协议实现</br>
     * tcp连接建立后发送http请求握手，升级http协议为webSocket协议</br>
     * 通过handshakePromise管理握手状态，true时成功，false时失败</br>
     * 握手成功后从管道中删除自己
     */
    public class WebSocketHandshakeHandler extends SimpleChannelInboundHandler<Object> {

        private final WebSocketClientHandshaker handshaker;
        private ChannelPromise handshakePromise;

        public WebSocketHandshakeHandler() {
            var headers = new DefaultHttpHeaders();
            headers.set("X-User-Id", WsClient.this.token.getProxyUser().getId() + "");
            headers.set("X-Comm-Id", WsClient.this.rand);
            headers.set("Authorization", WsClient.this.token.getAccessToken());
            ImHeadersReq imHeaders = WsClient.this.headersReq;
            if (imHeaders.getXClientId() != null)
                headers.add("X-Client-Id", imHeaders.getXClientId());
            if (imHeaders.getUscc() != null)
                headers.add("uscc", imHeaders.getUscc());
            if (imHeaders.getAllowSeid() != null)
                headers.add("Allow-Seid", imHeaders.getAllowSeid());
            if (imHeaders.getXUserId() != null)
                headers.add("X-User-Id", imHeaders.getXUserId().toString());
            handshaker = WebSocketClientHandshakerFactory.newHandshaker(WsClient.this.uri, WebSocketVersion.V13, null,
                true, headers, 5 * 1024 * 1024);
            log("info", "handshake param:{}", headers);
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
                var resp = (FullHttpResponse)msg;
                try {
                    handshaker.finishHandshake(ch, resp);
                    log("info", "after handshake finished for connection uri:{}", uri.toString());
                    handshakePromise.setSuccess();
                    ctx.pipeline().remove(this).addFirst(new ExceptionHandler()).addLast(new HeartBeatHandler())
                        .addLast(messageHandler);
                    messageHandler.onHandShakeFinished(ctx);
                } catch (WebSocketHandshakeException e) {
                    log("error", "after handshake failed for connection uri:" + uri.toString(), e);
                    handshakePromise.setFailure(new HandshakeException(e, resp.status()));
                }
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log("error", "exception when do handshake ", cause);
            if (!handshakePromise.isDone()) {
                handshakePromise.setFailure(cause);
            }
            ctx.close();
        }
    }

    @Getter
    public static class HandshakeException extends RuntimeException {
        private HttpResponseStatus status;

        public HandshakeException(Throwable cause, HttpResponseStatus status) {
            super(cause);
            this.status = status;
        }
    }
}
