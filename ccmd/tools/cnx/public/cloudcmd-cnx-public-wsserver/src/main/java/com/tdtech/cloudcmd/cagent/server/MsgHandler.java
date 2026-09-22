package com.tdtech.cloudcmd.cagent.server;

import com.tdtech.cloudcmd.cagent.server.component.MsgContext;
import com.tdtech.cloudcmd.cagent.server.frame.CdcFrame;

/**
 * Text Websocket message handler, triggered when received a text message in random order chain.
 *
 * All Exception will be caught, then any node in chain will be triggered.
 */
public interface MsgHandler {

    void onMessage(MsgContext ctx, CdcFrame msg);

    boolean shouldHandle(CdcFrame msg);
}
