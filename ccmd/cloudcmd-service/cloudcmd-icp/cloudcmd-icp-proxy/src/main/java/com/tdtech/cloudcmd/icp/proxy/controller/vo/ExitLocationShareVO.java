package com.tdtech.cloudcmd.icp.proxy.controller.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Schema(description = "退出位置共享统一请求")
public class ExitLocationShareVO {

    @NotNull
    @Schema(description = "位置共享ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "123")
    private Long shareId;

    @NotBlank
    @Schema(description = "动态群组ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "group001")
    private String groupId;

    @NotBlank
    @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1002")
    private String userId;

    @NotNull
    @Schema(description = "是否群主。1：群主；0：普通成员", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer isOwner;

    @Schema(description = "退出方式。0：手工退出；1：共享时间结束；2：用户状态异常", example = "0")
    private Integer exitType;

    @Schema(description = "退出描述")
    private String exitDesc;

    @Schema(description = "群主退出时设置的按钮状态", example = "true")
    private String status;
}
