package com.tdtech.cloudcmd.cagent.service.inbound;

import org.springframework.stereotype.Component;

import com.tdtech.cloudcmd.cagent.server.MsgHandler;
import com.tdtech.cloudcmd.cagent.server.component.MsgContext;
import com.tdtech.cloudcmd.cagent.server.frame.CdcFrame;
import com.tdtech.cloudcmd.cagent.server.frame.CdcFrameHeader;

@Component
public class HeartBeatMessageHandler implements MsgHandler {
    @Override
    public void onMessage(MsgContext ctx, CdcFrame msg) {
        CdcFrameHeader header = new CdcFrameHeader(CdcFrameHeader.TYPE_HANDSHAKE_HEARTBEAT_RPN);
        header.setToken(msg.getHeader().getToken());
        header.setSubsystem("");
        String body = "hearbeat:heartbeat";
        CdcFrame echo = new CdcFrame(header, body);
        ctx.write(echo);
    }

    @Override
    public boolean shouldHandle(CdcFrame msg) {
        return CdcFrameHeader.TYPE_HANDSHAKE_HEARTBEAT == msg.getHeader().getType();
    }
}
