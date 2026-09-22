package com.tdtech.cloudcmd.im.jingxin.client.entity;

import lombok.Data;

@Data
public class SubscribeReq {

    private String module;

    private String notifyType;

    private Long tag = System.currentTimeMillis();

    public SubscribeReq() {
        super();
    }

    public SubscribeReq(String module, String notifyType) {
        super();
        this.module = module;
        this.notifyType = notifyType;
    }
}
