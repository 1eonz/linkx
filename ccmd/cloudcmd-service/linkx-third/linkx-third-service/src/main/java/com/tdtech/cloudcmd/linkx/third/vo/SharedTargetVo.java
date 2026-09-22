package com.tdtech.cloudcmd.linkx.third.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@Schema(description = "共享分发目标")
public class SharedTargetVo {

    @Schema(description = "目标对象类型。1：警信群组；2：警信用户", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private int sharedTargetType;

    @Schema(description = "目标对象会话ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2001")
    private Long sharedTargetSessionId;
}
