package com.tdtech.cloudcmd.im.jingxin.server.entity.dto;

import lombok.Data;

@Data
public class GroupAiBindEntity {
    private Long id;

    private String userName;

    private String contactNumber;

    private String appId;

    private String appSecret;

    private Integer defaultUser;

    private Long agentId;
}
