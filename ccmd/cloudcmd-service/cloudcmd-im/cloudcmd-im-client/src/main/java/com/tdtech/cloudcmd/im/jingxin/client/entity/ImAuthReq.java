package com.tdtech.cloudcmd.im.jingxin.client.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ImAuthReq {

    private String clientId;
    private String clientSecret;
    private String grantType = "client_credentials";
    private String scope = "all";
    private String state;

}
