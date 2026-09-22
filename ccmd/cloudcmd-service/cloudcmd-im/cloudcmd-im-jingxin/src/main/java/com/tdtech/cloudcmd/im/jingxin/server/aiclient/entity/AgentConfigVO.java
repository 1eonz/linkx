package com.tdtech.cloudcmd.im.jingxin.server.aiclient.entity;

import lombok.Data;

@Data
public class AgentConfigVO {

    private Long id;

    private String name;

    /**
     * 是否接收IM消息。0：不接收；1：接收
     */
    private Integer receiveIm;

    /**
     * 智能体作用域。0：所有；1：仅AI智能体问答；2：仅IM
     */
    private Integer scope;
}