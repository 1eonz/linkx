package com.chinasoft.cloud.module.aiagent.controller.app.co;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "修改回复读取状态")
@Data
public class UpdateReplyReadStateCO {

    @Schema(description = "问答记录ID")
    @NotNull
    private Long id;

    @Schema(description = "前端当前读取到的回答字符位置")
    private Integer replyPosition;

    @Schema(description = "前端是否暂停回答")
    private Boolean replyPaused;
}
