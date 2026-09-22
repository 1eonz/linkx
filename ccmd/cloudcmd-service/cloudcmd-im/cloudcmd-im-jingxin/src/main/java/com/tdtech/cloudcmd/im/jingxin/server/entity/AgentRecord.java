package com.tdtech.cloudcmd.im.jingxin.server.entity;

import lombok.Data;

@Data
public class AgentRecord {
    private Long id;

    private String userName;

    private String identityCardNumber;

    private String queryContent;

    private Long time;

    private String agentName;

    private Long agentConfigId;
}
