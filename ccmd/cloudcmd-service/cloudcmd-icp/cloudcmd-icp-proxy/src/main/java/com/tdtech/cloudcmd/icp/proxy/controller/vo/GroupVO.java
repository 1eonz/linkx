package com.tdtech.cloudcmd.icp.proxy.controller.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class GroupVO {

    @NotBlank
    @Schema(description = "群组名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "group001")
    private String name;

    @NotNull
    @Schema(description = "群组用途（0：常规通信；1：位置共享；2：会议）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private int purpose;

    @NotNull
    @Schema(description = "群组类型（1：普通组；9：动态组）", requiredMode = Schema.RequiredMode.REQUIRED, example = "9")
    private int category;

    @NotBlank
    @Schema(description = "群组号", requiredMode = Schema.RequiredMode.REQUIRED, example = "10089102")
    private String group;

    @Schema(description = "群组优先级（1~15）", requiredMode = Schema.RequiredMode.REQUIRED, example = "15")
    private int priority;
}
