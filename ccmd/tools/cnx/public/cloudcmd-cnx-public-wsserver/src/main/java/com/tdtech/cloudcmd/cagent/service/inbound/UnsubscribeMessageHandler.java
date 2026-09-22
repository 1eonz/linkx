package com.tdtech.cloudcmd.cagent.service.inbound;

import org.springframework.stereotype.Component;

import com.tdtech.cloudcmd.cagent.server.MsgHandler;
import com.tdtech.cloudcmd.cagent.server.component.MsgContext;
import com.tdtech.cloudcmd.cagent.server.frame.CdcFrame;
import com.tdtech.cloudcmd.cagent.server.frame.CdcFrameHeader;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class UnsubscribeMessageHandler implements MsgHandler {
    @Override
    public void onMessage(MsgContext ctx, CdcFrame msg) {
        log.info("on unsubscribe event:{}", msg);
        // todo unsubscribe
    }

    @Override
    public boolean shouldHandle(CdcFrame msg) {
        return CdcFrameHeader.TYPE_HANDSHAKE_UNSUBSCRIBE == msg.getHeader().getType();
    }
}
