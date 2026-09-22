package com.tdtech.linkx.node.ws.server;

import com.tdtech.cloudcmd.util.json.JsonUtil;
import com.tdtech.linkx.node.ws.WsMessage;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

/**
 * P2P WebSocket服务端常量
 */
@Slf4j
public class P2pWebSocketServer {

    /**
     * Channel属性Key - peerId
     */
    public static final AttributeKey<String> PEER_ID_KEY = AttributeKey.valueOf("peerId");

    /**
     * 广播消息给所有已连接的客户端
     */
    public static void broadcast(Iterable<Channel> channels, WsMessage<?> message) {
        String json = JsonUtil.toJsonStr(message);
        TextWebSocketFrame frame = new TextWebSocketFrame(json);
        for (Channel channel : channels) {
            if (channel.isActive()) {
                channel.writeAndFlush(frame.copy());
            }
        }
    }

}
