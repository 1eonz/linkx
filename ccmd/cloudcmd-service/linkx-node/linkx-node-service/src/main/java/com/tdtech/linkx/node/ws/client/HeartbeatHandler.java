package com.tdtech.linkx.node.ws.client;

import com.tdtech.cloudcmd.util.json.JsonUtil;
import com.tdtech.linkx.node.config.NodeProperties;
import com.tdtech.linkx.node.ws.WsMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;
import io.netty.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 客户端心跳处理器
 */
@Slf4j
public class HeartbeatHandler extends ChannelInboundHandlerAdapter {
    private final NodeProperties nodeProperties;
    private final P2pWebSocketClient client;
    private final String peerId;

    private ScheduledFuture<?> heartbeatFuture;

    public HeartbeatHandler(NodeProperties nodeProperties, P2pWebSocketClient client, String peerId) {
        this.nodeProperties = nodeProperties;
        this.client = client;
        this.peerId = peerId;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        long interval = nodeProperties.getClient().getHeartbeatInterval();
        heartbeatFuture = ctx.executor().scheduleAtFixedRate(() -> {
            if (ctx.channel().isActive()) {
                sendHeartbeat(peerId);
            }
        }, interval, interval, TimeUnit.MILLISECONDS);
        client.getHeartbeatTasks().put(peerId, heartbeatFuture);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (heartbeatFuture != null) {
            heartbeatFuture.cancel(false);
            client.getHeartbeatTasks().remove(peerId);
        }
        super.channelInactive(ctx);
    }

    public void sendHeartbeat(String peerId) {
        Channel channel = client.getChannels().get(peerId);
        if (channel != null && channel.isActive()) {
            WsMessage<Object> ping = WsMessage.ping();
            String json = JsonUtil.toJsonStr(ping);
            channel.writeAndFlush(new TextWebSocketFrame(json));
            log.debug("Sent ping to server: peerId={}", peerId);
        }
    }
}
