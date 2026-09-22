package com.tdtech.cloudcmd.im.jingxin.server.aiclient.entity;

import java.io.Serializable;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author ly
 * @date 2025/8/26 16:39
 */
@Data
public class AskAIReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 身份证号
     */
    private String userID;

    /**
     * 提问内容
     */
    private String content;

    /**
     * 智能体配置ID，不填默认为deepseek
     */
    private Long agent;

    private String departmentCode;

    private String departmentId;

    private String departmentName;

    /**
     * 提问类型：1-智能体ai助手提问，2-@群ai助手提问，3-单聊智能体；不传默认1
     */
    private Integer askType = 2;

    /**
     * IM会话ID，仅 IM 透传场景(askType=2/3)记录，用于按会话拉取上下文
     */
    private Long imSessionId;

    /**
     * 推送 IM 智能体所需的特殊字段，仅 askType=2/3 透传场景使用；
     * 有值时 agent 侧直接作为请求 body 发送，不再走模板解析
     */
    private Map<String, Object> imExtra;
}