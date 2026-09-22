package com.tdtech.cloudcmd.cagent.service.outbound.callback;

import com.tdtech.cloudcmd.cagent.server.frame.CdcFrame;
import com.tdtech.cloudcmd.cagent.service.entity.LocalChannel;

import io.netty.util.concurrent.Future;

public interface SendMessageCallBack {

    void callback(LocalChannel localChannel, Future<? super Void> channelFuture, CdcFrame payload);

}
