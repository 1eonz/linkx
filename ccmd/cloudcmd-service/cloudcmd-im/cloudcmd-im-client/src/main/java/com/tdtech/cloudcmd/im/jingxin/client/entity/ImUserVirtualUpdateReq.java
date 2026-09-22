package com.tdtech.cloudcmd.im.jingxin.client.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 虚拟用户更新请求
 */
@Data
@Schema(description = "虚拟用户更新请求")
public class ImUserVirtualUpdateReq implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "虚拟用户名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "智能助手")
    @NotBlank(message = "虚拟用户名称不能为空")
    @Size(max = 100, message = "虚拟用户名称最大长度要小于100")
    private String userName;

    @Schema(description = "通讯号码（警信后台开户账号）", example = "13800138000")
    private String contactNumber;

    @Schema(description = "应用ID（警信后台开户获取）", requiredMode = Schema.RequiredMode.REQUIRED, example = "app_123456")
    @NotBlank(message = "应用ID不能为空")
    @Size(max = 64, message = "应用ID最大长度要小于64")
    private String appId;

    @Schema(description = "应用密钥（警信后台开户获取）", requiredMode = Schema.RequiredMode.REQUIRED, example = "secret_abcdef")
    @NotBlank(message = "应用密钥不能为空")
    @Size(max = 255, message = "应用密钥最大长度要小于255")
    private String appSecret;

    @Schema(description = "备注", example = "这是一个智能助手")
    private String remark;

    @Schema(description = "创建人ID", example = "1111")
    @NotNull(message = "创建人ID不能为null")
    private Long createdBy;

    @Schema(description = "是否默认入群用户，用于一键建群拉默认智能体。0：不；1：要", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "是否默认入群用户不能为空")
    private Integer defaultUser;

    @Schema(description = "虚拟用户类型。1：智能体设备（智能体）；2：三方平台（双向）。不传默认1", example = "1")
    private Integer virtualType;
}