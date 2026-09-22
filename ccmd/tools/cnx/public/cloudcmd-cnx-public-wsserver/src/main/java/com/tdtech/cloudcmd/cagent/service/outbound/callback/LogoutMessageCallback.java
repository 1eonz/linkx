package com.tdtech.cloudcmd.cagent.service.outbound.callback;

import org.springframework.stereotype.Component;

import com.tdtech.cloudcmd.cagent.server.frame.CdcFrame;
import com.tdtech.cloudcmd.cagent.server.frame.CdcFrameHeader;
import com.tdtech.cloudcmd.cagent.service.entity.LocalChannel;

import io.netty.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class LogoutMessageCallback implements SendMessageCallBack {

    @Override
    public void callback(LocalChannel localChannel, Future<? super Void> channelFuture, CdcFrame payload) {
        if (payload.getHeader().getType() == CdcFrameHeader.TYPE_PASSPORT_KICKOUT
            && localChannel.getChannel().isActive()) {
            log.trace("send logout msg call back:{}", payload);
            localChannel.getChannel().close();
        }
    }
}
