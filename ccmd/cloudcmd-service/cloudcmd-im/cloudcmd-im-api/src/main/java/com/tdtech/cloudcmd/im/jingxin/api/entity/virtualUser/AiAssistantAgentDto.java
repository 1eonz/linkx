package com.tdtech.cloudcmd.im.jingxin.api.entity.virtualUser;

import lombok.Data;

import java.io.Serializable;

@Data
public class AiAssistantAgentDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long virtualUserId;

    private Long agentId;

    private Long createdUserId;
}