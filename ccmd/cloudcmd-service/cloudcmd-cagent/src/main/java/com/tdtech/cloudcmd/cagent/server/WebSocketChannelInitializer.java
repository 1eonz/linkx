package com.tdtech.cloudcmd.cagent.server;

import java.util.Objects;

import javax.annotation.Resource;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import com.tdtech.cloudcmd.cagent.conf.CagentProperties;
import com.tdtech.cloudcmd.cagent.server.component.MsgContext;
import com.tdtech.cloudcmd.cagent.server.frame.CdcFrame;
import com.tdtech.cloudcmd.cagent.server.frame.CdcFrameHeader;
import com.tdtech.cloudcmd.cagent.server.frame.CdcFrameSerializer;
import com.tdtech.cloudcmd.cagent.service.AuthManager;
import com.tdtech.cloudcmd.cagent.service.UserInfo;
import com.tdtech.cloudcmd.util.json.JsonUtil;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * handler chain: ws pre handler -> http reject handler -> auth handler(before auth)/ws message handler(after auth)
 *
 * triggers auth event when auth succeed, and channel inactive when channel inactive after auth succeed;
 */
@Slf4j
@Component
public class WebSocketChannelInitializer extends ChannelInitializer<Channel> {

    @Resource
    private CagentProperties cagentProperties;
    @Resource
    private ObjectProvider<MsgHandler> msgHandlers;
    @Resource
    private ObjectProvider<ChannelStatusHandler> channelStatusHandlers;
    @Resource
    private AuthManager authManager;

    @Override
    protected void initChannel(Channel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(65536));
        pipeline.addLast(new WebSocketServerCompressionHandler());
        pipeline.addLast(new WebSocketServerProtocolHandler(cagentProperties.getWebsocketPath(), null, true));
        pipeline.addLast("WebFrameEncoder", new WebFrameEncoder());
        pipeline.addLast(new HttpMessageHandler());
        pipeline.addLast(new IdleStateHandler(0, 0, cagentProperties.getHeartbeatTimeout()));
        pipeline.addLast(new HeartBeatHandler());
        pipeline.addLast(new AuthMessageHandler());
    }

    /**
     * for websocket, when length not enough return null and reject;
     */
    public CdcFrame decodeBinary(WebSocketFrame webSocketFrame) {
        ByteBuf in = webSocketFrame.content();
        if (in.readableBytes() < Integer.BYTES) {
            return null;
        }
        int length = in.readInt();
        if (in.readableBytes() < length - Integer.BYTES) {
            return null;
        }
        return CdcFrameSerializer.read(length, in);
    }

    /**
     * reject normal http message
     */
    public static class HttpMessageHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) {
            log.error("on http message:{}", msg.uri());
            DefaultFullHttpResponse res =
                new DefaultFullHttpResponse(msg.protocolVersion(), HttpResponseStatus.BAD_REQUEST);
            ByteBufUtil.writeUtf8(res.content(), "cant take http message on websocket port");
            HttpUtil.setContentLength(res, res.content().readableBytes());
            HttpUtil.setKeepAlive(res, false);
            ctx.writeAndFlush(res).addListener(ChannelFutureListener.CLOSE);
        }
    }

    public static class WebFrameEncoder extends ChannelOutboundHandlerAdapter {

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
            if (msg instanceof CdcFrame) {
                ctx.write(new TextWebSocketFrame(((CdcFrame)msg).getBody()), promise);
            } else {
                ctx.write(msg, promise);
            }
        }
    }

    public static class CnWebFrameEncoder extends ChannelOutboundHandlerAdapter {

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
            if (msg instanceof CdcFrame) {
                ctx.write(new TextWebSocketFrame(JsonUtil.toJsonStr(msg)), promise);
            } else {
                ctx.write(msg, promise);
            }
        }
    }

    /**
     * handle message when auth finished
     */
    public class WebSocketMessageHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

        private final UserInfo loginInfo;

        public WebSocketMessageHandler(UserInfo loginInfo) {
            Objects.requireNonNull(loginInfo);
            this.loginInfo = loginInfo;
        }

        /**
         * Ping/Pong/Close frame deal by WebSocketProtocolHandler, Only text and binary frame need to be deal with;
         */
        @Override
        public void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
            if (frame instanceof BinaryWebSocketFrame) {
                CdcFrame decode = decodeBinary(frame);
                msgHandlers.forEach(a -> {
                    try {
                        if (a.shouldHandle(decode)) {
                            a.onMessage(new MsgContext(ctx), decode);
                        }
                    } catch (Throwable e) {
                        log.error("handle msg error", e);
                    }
                });
            } else {
                // ignore text frame
                ctx.write(new TextWebSocketFrame("data type not supported yet"));
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            try {
                channelStatusHandlers.forEach(a -> {
                    a.onChannelInactive(ctx.channel(), loginInfo);
                });
            } catch (Throwable e) {
                log.error("inactive error", e);
            }
            ctx.fireChannelInactive();
        }
    }

    public class AuthMessageHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) {
            CdcFrame decode;
            if (frame instanceof BinaryWebSocketFrame) {
                decode = decodeBinary(frame);
            } else if (frame instanceof TextWebSocketFrame) {
                var txtf = (TextWebSocketFrame)frame;
                log.info("auth text:{}", txtf);
                decode = JsonUtil.parseJson(txtf.text(), CdcFrame.class);
            } else {
                // ignore binary frame
                ctx.writeAndFlush(new TextWebSocketFrame("data type not supported in auth"));
                return;
            }
            log.info("auth frame:{}", decode);

            if (decode == null) {
                authFailed(ctx, "decode error");
                return;
            }
            if (CdcFrameHeader.TYPE_SWITCH_ENCODER == decode.getHeader().getType()) {
                ctx.pipeline().replace("WebFrameEncoder", "WebFrameEncoder", new CnWebFrameEncoder());
            } else if (CdcFrameHeader.TYPE_HANDSHAKE_REGISTER == decode.getHeader().getType()) {
                // check token
                String token = decode.getHeader().getToken();
                UserInfo loginInfo = authManager.getLoginInfo(token);
                log.info("auth user:{}", loginInfo);
                if (loginInfo == null) {
                    authFailed(ctx, "token error");
                    return;
                }
                // switch handler
                ChannelPipeline pipeline = ctx.pipeline();
                HeartBeatHandler heartBeatHandler = pipeline.get(HeartBeatHandler.class);
                heartBeatHandler.setUserInfo(loginInfo);
                heartBeatHandler.setHandlers(channelStatusHandlers);
                pipeline.remove(this);
                pipeline.addLast(new WebSocketMessageHandler(loginInfo));
                // send event
                onChannelAuth(ctx, loginInfo);
            } else {
                // ignore not register message
                authFailed(ctx, "data type not supported yet");
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.error("auth error", cause);
            ctx.writeAndFlush(new TextWebSocketFrame("auth error"));
            ctx.close();
            ctx.fireExceptionCaught(cause);
        }

        private void authFailed(ChannelHandlerContext ctx, String message) {
            ctx.writeAndFlush(new TextWebSocketFrame(message)).addListener(ChannelFutureListener.CLOSE);
        }

        private void onChannelAuth(ChannelHandlerContext ctx, UserInfo loginInfo) {
            channelStatusHandlers.forEach(a -> a.onChannelAuth(ctx.channel(), loginInfo));
        }

    }
}
