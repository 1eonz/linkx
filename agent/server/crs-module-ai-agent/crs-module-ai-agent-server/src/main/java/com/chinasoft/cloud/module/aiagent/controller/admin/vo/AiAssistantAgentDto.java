package com.chinasoft.cloud.module.aiagent.controller.admin.vo;

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