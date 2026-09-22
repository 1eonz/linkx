package com.tdtech.cloudcmd.im.jingxin.client.ws;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 实现了Netty默认的channelRead0方法，并对基础消息类型做了判断 需要实现onTextMessage单独处理Text类型消息</br>
 * wensocket连接建立后将会回调onHandShakeSucceed方法
 */
@Slf4j
@ChannelHandler.Sharable
public abstract class WebSocketMessageHandler extends SimpleChannelInboundHandler<Object> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object msg) {
        if (msg instanceof FullHttpResponse) {
            FullHttpResponse response = (FullHttpResponse)msg;
            throw new IllegalStateException("Unexpected FullHttpResponse (getStatus=" + response.status() + ", content="
                + response.content().toString(CharsetUtil.UTF_8) + ')');
        }
        WebSocketFrame frame = (WebSocketFrame)msg;
        if (msg instanceof TextWebSocketFrame) {
            TextWebSocketFrame textFrame = (TextWebSocketFrame)frame;
            log.info("WebSocket Client received message: " + textFrame.text());
            onTextMessage(channelHandlerContext, textFrame);
        } else if (msg instanceof PongWebSocketFrame) {
            log.info("pong!!");
        } else if (msg instanceof CloseWebSocketFrame) {
            log.warn("WebSocket Client received closing");
            channelHandlerContext.channel().close();
        } else {
            log.warn("unknown message type:{}", msg.getClass());
        }
    }

    public abstract void onTextMessage(ChannelHandlerContext channelHandlerContext, TextWebSocketFrame msg);

    public abstract void onHandShakeFinished(ChannelHandlerContext channelHandlerContext);
}
