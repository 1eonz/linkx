package com.chinasoft.cloud.module.aiagent.controller.app.co;

import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "提问")
@Data
public class AskAgentCO {

    @Schema(description = "用户名")
    private String userName;

    @Schema(description = "身份证号")
    private String userID;

    @Schema(description = "问题")
    private String content;

    @Schema(description = "智能体配置ID")
    private Long agent;

    @Schema(description = "部门编码")
    private String departmentCode;

    @Schema(description = "部门ID")
    private String departmentId;

    @Schema(description = "部门")
    private String departmentName;

    @Schema(description = "审核人")
    private String approver;

    @Schema(description = "WebSocket 会话ID")
    private String wsSessionId;

    @Schema(description = "文件上传会话ID，用于关联上传的文件")
    private String sessionId;

    @Schema(description = "提问类型：1-智能体ai助手提问，2-@群ai助手提问，3-单聊智能体；不传默认1")
    private Integer askType;

    @Schema(description = "IM会话ID，仅 IM 透传场景(askType=2/3)记录，用于按会话拉取上下文")
    private Long imSessionId;

    @Schema(description = "推送 IM 智能体所需的特殊字段，键值由调用方按场景约定；仅 askType=2/3 透传场景使用")
    private JSONObject imExtra;
}