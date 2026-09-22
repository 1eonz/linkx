package com.chinasoft.cloud.module.aiagent.controller.admin.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class UserVirtualVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID", example = "1")
    private Long id;

    @Schema(description = "虚拟用户名称", example = "智能助手")
    private String userName;

    @Schema(description = "通讯号码（警信后台开户账号）", example = "13800138000")
    private String contactNumber;

    @Schema(description = "应用ID（警信后台开户获取）", example = "app_123456")
    private String appId;

    @Schema(description = "应用密钥（警信后台开户获取）", example = "secret_abcdef")
    private String appSecret;

    @Schema(description = "备注", example = "这是一个智能助手")
    private String remark;

    @Schema(description = "创建人ID", example = "1")
    private Long createdBy;

    @Schema(description = "是否默认虚拟用户，用于一键建群拉默认智能体。0：不是；1：是默认", example = "0")
    private Integer defaultUser;

    @Schema(description = "虚拟用户类型。1：智能体设备（智能体）；2：三方平台（双向）", example = "1")
    private Integer virtualType;

    @Schema(description = "是否删除。0：未删除；1：删除", example = "0")
    private Integer deleted;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;

    @Schema(description = "用户关联的智能体ID", example = "1")
    private Long agentId;
}