package com.tdtech.cloudcmd.icp.proxy.ws.event;

import org.springframework.context.ApplicationEvent;

public class HttpAuthSucceedEvent extends ApplicationEvent {
    public HttpAuthSucceedEvent(String session) {
        super(session);
    }

    @Override
    public String getSource() {
        return (String)super.getSource();
    }
}
