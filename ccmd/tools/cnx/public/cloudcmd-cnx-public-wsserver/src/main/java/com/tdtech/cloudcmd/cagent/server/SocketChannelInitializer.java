package com.tdtech.cloudcmd.cagent.server;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Resource;
import javax.net.ssl.SSLEngine;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tdtech.cloudcmd.cagent.conf.CagentProperties;
import com.tdtech.cloudcmd.cagent.server.component.MsgContext;
import com.tdtech.cloudcmd.cagent.server.component.SslEngineFactory;
import com.tdtech.cloudcmd.cagent.server.frame.CdcFrame;
import com.tdtech.cloudcmd.cagent.server.frame.CdcFrameHeader;
import com.tdtech.cloudcmd.cagent.server.frame.CdcFrameSerializer;
import com.tdtech.cloudcmd.cagent.service.AuthManager;
import com.tdtech.cloudcmd.cagent.service.UserInfo;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SocketChannelInitializer extends ChannelInitializer<Channel> {

    private static final int MAX_FRAME_LENGTH = 1024 * 1024;

    @Resource
    private CagentProperties cagentProperties;
    @Resource
    private ObjectProvider<MsgHandler> msgHandlers;
    @Resource
    private ObjectProvider<ChannelStatusHandler> channelStatusHandlers;
    @Resource
    private AuthManager authManager;
    @Resource
    private SslEngineFactory sslEngineFactory;
    @Resource
    private ObjectMapper objectMapper;

    @Override
    protected void initChannel(Channel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        if (cagentProperties.isSsl()) {
            SSLEngine sslEngine = sslEngineFactory.newSslEngine();
            sslEngine.setUseClientMode(false);
            sslEngine.setNeedClientAuth(false);
            pipeline.addFirst("ssl", new SslHandler(sslEngine));
        }
        pipeline.addLast("unpack",
            new LengthFieldBasedFrameDecoder(MAX_FRAME_LENGTH, 0, Integer.BYTES, -Integer.BYTES, 0, true));
        pipeline.addLast("decoder", new FrameDecoder());
        pipeline.addLast("encoder", new FrameEncoder());
        pipeline.addLast(new IdleStateHandler(0, 0, cagentProperties.getHeartbeatTimeout()));
        pipeline.addLast(new HeartBeatHandler());
        pipeline.addLast(new AuthMessageHandler());

    }

    /**
     * CdcFrame to ByteBuf
     */
    public static class FrameEncoder extends MessageToByteEncoder<CdcFrame> {
        @Override
        protected void encode(ChannelHandlerContext ctx, CdcFrame msg, ByteBuf out) {
            Objects.requireNonNull(msg);
            log.debug("decode message:{}", msg);
            CdcFrameSerializer.write(msg, out);
        }
    }

    /**
     * ByteBuf to CdcFrame
     */
    public static class FrameDecoder extends SimpleChannelInboundHandler<ByteBuf> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
            if (msg.readableBytes() < Integer.BYTES) {
                // shall not into here
                log.error("unpack error,no header found");
                return;
            }
            int length = msg.readInt();
            if (msg.readableBytes() < length - Integer.BYTES) {
                // shall not into here too
                log.error("unpack error,package length not enough");
                return;
            }
            CdcFrame read = CdcFrameSerializer.read(length, msg);
            log.debug("on socket message:{}", read);
            ctx.fireChannelRead(read);
        }
    }

    public class AuthMessageHandler extends SimpleChannelInboundHandler<CdcFrame> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, CdcFrame frame) throws Exception {
            if (frame == null) {
                authFailed(ctx, "decode error");
                return;
            }
            if (CdcFrameHeader.TYPE_HANDSHAKE_REGISTER != frame.getHeader().getType()) {
                // ignore not register message
                authFailed(ctx, "type error");
                return;
            }
            // check token
            String token = frame.getHeader().getToken();
            UserInfo loginInfo = authManager.getLoginInfo(token);
            if (loginInfo == null) {
                authFailed(ctx, "token error");
                return;
            }
            // send authResult
            authSucceed(ctx, token);
            // switch handler
            ChannelPipeline pipeline = ctx.pipeline();
            HeartBeatHandler heartBeatHandler = pipeline.get(HeartBeatHandler.class);
            heartBeatHandler.setUserInfo(loginInfo);
            heartBeatHandler.setHandlers(channelStatusHandlers);
            pipeline.remove(this);
            pipeline.addLast(new SocketMessageHandler(loginInfo));
            // send event
            onChannelAuth(ctx, loginInfo);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.error("auth error", cause);
            ctx.writeAndFlush(new TextWebSocketFrame("auth error"));
            ctx.close();
            ctx.fireExceptionCaught(cause);
        }

        private void authFailed(ChannelHandlerContext ctx, String message) throws JsonProcessingException {
            CdcFrameHeader header = new CdcFrameHeader(CdcFrameHeader.TYPE_HANDSHAKE_REJECT);
            Map<String, String> reason = Collections.singletonMap("reason", message);
            String payload = objectMapper.writeValueAsString(reason);
            ctx.writeAndFlush(new CdcFrame(header, payload)).addListener(ChannelFutureListener.CLOSE);
        }

        private void onChannelAuth(ChannelHandlerContext ctx, UserInfo loginInfo) {
            channelStatusHandlers.forEach(a -> a.onChannelAuth(ctx.channel(), loginInfo));
        }

        private void authSucceed(ChannelHandlerContext ctx, String token) throws JsonProcessingException {
            Map<String, Integer> content = new HashMap<>();
            content.put("heartbeatTimeout", cagentProperties.getHeartbeatTimeout());
            content.put("heartbeatLosttimes", 1);
            CdcFrameHeader header = new CdcFrameHeader(CdcFrameHeader.TYPE_HANDSHAKE_READY);
            header.setSubsystem("");
            header.setToken(token);
            CdcFrame ready = new CdcFrame(header, objectMapper.writeValueAsString(content));
            ctx.writeAndFlush(ready);
        }
    }

    /**
     * handle message when auth finished
     */
    public class SocketMessageHandler extends SimpleChannelInboundHandler<CdcFrame> {

        private final UserInfo loginInfo;

        public SocketMessageHandler(UserInfo loginInfo) {
            Objects.requireNonNull(loginInfo);
            this.loginInfo = loginInfo;
        }

        /**
         * Ping/Pong/Close frame deal by WebSocketProtocolHandler, Only text and binary frame need to be deal with;
         */
        @Override
        public void channelRead0(ChannelHandlerContext ctx, CdcFrame frame) {
            log.debug("on message:{}", frame);
            msgHandlers.forEach(a -> {
                try {
                    if (a.shouldHandle(frame)) {
                        a.onMessage(new MsgContext(ctx), frame);
                    }
                } catch (Throwable e) {
                    log.error("handle msg error", e);
                }
            });
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
}
