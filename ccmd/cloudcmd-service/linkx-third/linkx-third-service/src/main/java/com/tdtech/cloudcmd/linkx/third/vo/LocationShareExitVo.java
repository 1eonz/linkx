package com.tdtech.cloudcmd.linkx.third.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@Schema(description = "退出位置共享请求")
public class LocationShareExitVo {

    @NotNull
    @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1002")
    private Long userId;

    @Schema(description = "退出方式。0：手工退出；1：共享时间结束；2：用户状态异常")
    private int exitType = 0;

    @Schema(description = "退出描述")
    private String exitDesc;
}
