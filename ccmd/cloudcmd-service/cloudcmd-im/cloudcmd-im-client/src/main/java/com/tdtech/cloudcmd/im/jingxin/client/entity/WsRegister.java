package com.tdtech.cloudcmd.im.jingxin.client.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class WsRegister {
    private String module = "app";
    private String notifyType = "register";
    private String commId;
    private String userId;
    private String token;
}
