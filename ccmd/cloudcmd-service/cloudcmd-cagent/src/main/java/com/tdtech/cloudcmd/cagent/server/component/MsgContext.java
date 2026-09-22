package com.tdtech.cloudcmd.cagent.server.component;

import com.tdtech.cloudcmd.cagent.server.frame.CdcFrame;

import io.netty.channel.ChannelHandlerContext;
import lombok.AllArgsConstructor;

/**
 * pack ChannelHandlerContext to block close channel action
 */
@AllArgsConstructor
public class MsgContext {

    private ChannelHandlerContext context;

    public void write(CdcFrame cdcFrame) {
        context.writeAndFlush(cdcFrame);
    }

}
