package com.chinasoft.cloud.module.aiagent.controller.admin.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class VirtualUserBindAgentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /**
     * 虚拟用户id
     */
    private Long virtualUserId;

    /**
     * AI智能体ID
     */
    private Long agentId;

    /**
     * 创建人ID
     */
    private Long createdUserId;
}
