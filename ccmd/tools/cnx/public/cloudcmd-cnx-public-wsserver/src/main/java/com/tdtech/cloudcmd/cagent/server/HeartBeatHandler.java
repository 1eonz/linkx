package com.tdtech.cloudcmd.cagent.server;

import org.springframework.beans.factory.ObjectProvider;

import com.tdtech.cloudcmd.cagent.service.UserInfo;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Setter
public class HeartBeatHandler extends ChannelInboundHandlerAdapter {

    private ObjectProvider<ChannelStatusHandler> handlers;

    private UserInfo userInfo;

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        try {
            if (userInfo != null) {
                handlers.forEach(a -> a.onChannelHeartBeat(ctx.channel(), userInfo));
            }
        } catch (Exception e) {
            log.error("error", e);
        }
        super.channelReadComplete(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent)evt;
            if (event.state() == IdleState.ALL_IDLE) {
                ctx.close();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

}
