package com.tdtech.linkx.node.ws.server;

import com.tdtech.linkx.node.constants.Constants;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * 服务端WebSocket握手处理器
 * */
@Slf4j
public class WebSocketHandshakeHandler extends ChannelInboundHandlerAdapter {

    private static final String WEBSOCKET_PATH = "/ws";

    private final P2pWebSocketServerHandler messageHandler;
    private WebSocketServerHandshaker handshaker;

    public WebSocketHandshakeHandler(P2pWebSocketServerHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest req = (FullHttpRequest) msg;
            try {
                handleHttpRequest(ctx, req);
            } finally {
                ReferenceCountUtil.release(req);
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) {
        // 检查请求解码是否成功
        if (!req.decoderResult().isSuccess()) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }

        // 检查是否是 WebSocket 握手请求
        HttpHeaders headers = req.headers();
        if (!headers.contains(HttpHeaderNames.UPGRADE) ||
                !headers.get(HttpHeaderNames.UPGRADE).equalsIgnoreCase(HttpHeaderValues.WEBSOCKET.toString())) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }

        // 提取 peerId
        String uri = req.uri();
        log.info("WebSocket handshake request: uri={}", uri);
        QueryStringDecoder decoder = new QueryStringDecoder(uri);
        Map<String, List<String>> params = decoder.parameters();

        List<String> peerIdList = params.get(Constants.PEER_ID);
        String peerId;
        if (peerIdList != null && !peerIdList.isEmpty()) {
            peerId = peerIdList.get(0);
            log.info("WebSocket handshake with peerId: {}", peerId);
            ctx.channel().attr(P2pWebSocketServer.PEER_ID_KEY).set(peerId);
        } else {
            peerId = null;
            log.warn("WebSocket handshake without peerId, uri={}", uri);
        }

        // 执行 WebSocket 握手
        WebSocketServerHandshakerFactory handshakerFactory = new WebSocketServerHandshakerFactory(
                getWebSocketLocation(req), null, true, 65536, false);
        handshaker = handshakerFactory.newHandshaker(req);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
            return;
        }

        handshaker.handshake(ctx.channel(), req).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    log.info("WebSocket handshake completed: peerId={}", peerId);
                    if (peerId != null && !peerId.isEmpty()) {
                        messageHandler.sendChallenge(ctx, peerId);
                    } else {
                        log.warn("Missing client identifier, websocket handshake failed.");
                        ctx.close();
                    }
                } else {
                    log.error("WebSocket handshake failed: peerId={}", peerId, future.cause());
                    ctx.close();
                }
            }
        });
    }

    private static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse resp) {
        HttpUtil.setContentLength(resp, resp.content().readableBytes());
        if (HttpUtil.isKeepAlive(req)) {
            resp.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            ctx.writeAndFlush(resp);
        } else {
            resp.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
            ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);
        }
    }

    private static String getWebSocketLocation(FullHttpRequest req) {
        // APISIX 卸载 SSL 后转发到后端为明文 ws，但 Host 头可能保留 wss 信息
        // 此处统一返回 ws://，因为后端 pipeline 无 SslHandler
        String location = req.headers().get(HttpHeaderNames.HOST) + WEBSOCKET_PATH;
        return "ws://" + location;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("WebSocketHandshakeHandler.exceptionCaught", cause);
        ctx.close();
    }
}
