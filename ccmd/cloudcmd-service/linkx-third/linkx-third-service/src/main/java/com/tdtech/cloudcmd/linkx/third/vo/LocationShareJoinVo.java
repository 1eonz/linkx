package com.tdtech.cloudcmd.linkx.third.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@Schema(description = "加入位置共享请求")
public class LocationShareJoinVo {

    @NotNull
    @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1002")
    private Long userId;

    @Schema(description = "用户ISDN")
    private String isdn;
}
