package com.tdtech.cloudcmd.im.jingxin.client.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class WsAck {
    private String transId;
    private Boolean needAck;
    private String module;
    private String notifyType;
    private String commId;
    private Integer code = 0;
    private String msg;

    public WsAck(WsResponse resp) {
        transId = resp.getTransId();
        needAck = resp.getNeedAck();
        module = resp.getModule();
        notifyType = resp.getNotifyType();
        commId = resp.getCommId();
    }
}
