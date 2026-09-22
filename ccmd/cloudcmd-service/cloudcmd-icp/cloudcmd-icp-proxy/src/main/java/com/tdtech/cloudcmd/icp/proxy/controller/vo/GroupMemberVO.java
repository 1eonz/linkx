package com.tdtech.cloudcmd.icp.proxy.controller.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class GroupMemberVO {

    @NotNull
    @Schema(description = "群组号码", requiredMode = Schema.RequiredMode.REQUIRED, example = "100012")
    private Long group;

    @NotBlank
    @Schema(description = "群组成员的isdn", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000122")
    private String isdn;

    @NotNull
    @Schema(description = "成员类型。1：用户， 2：群组", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private int membertype;

    @Schema(description = "用户优先级（1~15）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private int userpriority;
}
