package com.tdtech.cloudcmd.im.openapi.controller.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 开放API虚拟用户视图对象
 * 对外仅暴露虚拟用户基础信息，不含智能体绑定关系等内部字段
 */
@Data
@Schema(description = "虚拟用户信息")
public class ImVirtualUserVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "ID", example = "1")
    private Long id;

    @Schema(description = "用户名称", example = "智能助手")
    private String userName;

    @Schema(description = "通讯号码", example = "13800138000")
    private String contactNumber;

    @Schema(description = "应用ID", example = "app_123456")
    private String appId;

    @Schema(description = "应用密钥", example = "secret_abcdef")
    private String appSecret;

    @Schema(description = "是否默认用户", example = "0")
    private Integer defaultUser;

    @Schema(description = "创建时间")
    private Date createdAt;
}