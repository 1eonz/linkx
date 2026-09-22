package com.chinasoft.cloud.module.aiagent.controller.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "AI回答轮询结果")
@Data
public class AskDetailVO {

    @Schema(description = "问答记录ID")
    private Long id;

    @Schema(description = "回答内容")
    private String reply;

    @Schema(description = "当前回答字符位置，-1 表示全部回答完成")
    private Integer replyPosition;

    @Schema(description = "回答是否暂停")
    private Boolean replyPaused;

    @Schema(description = "智能体名称")
    private String agentName;

    @Schema(description = "提问人名称")
    private String userName;

    @Schema(description = "响应时间")
    private LocalDateTime answerTime;

    @Schema(description = "提问时间")
    private LocalDateTime queryTime;
}