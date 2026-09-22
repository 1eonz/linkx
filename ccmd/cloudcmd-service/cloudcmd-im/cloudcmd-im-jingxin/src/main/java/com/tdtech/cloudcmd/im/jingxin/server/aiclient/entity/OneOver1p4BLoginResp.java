package com.tdtech.cloudcmd.im.jingxin.server.aiclient.entity;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class OneOver1p4BLoginResp {

    private String id;
    private String username;
    private String avatar;
    private Integer onlineStatus;
    private String token;

}
