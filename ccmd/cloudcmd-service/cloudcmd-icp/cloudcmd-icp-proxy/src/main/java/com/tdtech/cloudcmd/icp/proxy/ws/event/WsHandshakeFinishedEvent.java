package com.tdtech.cloudcmd.icp.proxy.ws.event;

import io.netty.channel.Channel;
import org.springframework.context.ApplicationEvent;

public class WsHandshakeFinishedEvent extends ApplicationEvent {
    public WsHandshakeFinishedEvent(Channel source) {
        super(source);
    }

    @Override
    public Channel getSource() {
        return (Channel)super.getSource();
    }
}
