package com.chinasoft.cloud.module.aiagent.controller.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserAgentRecordVO {

    @Schema(description = "智能体ID")
    private Long agentId;

    @Schema(description = "智能体配置ID")
    private Long agentConfigId;

    @Schema(description = "智能体名称")
    private String agentName;

    @Schema(description = "智能体token")
    private String token;

    @Schema(description = "智能体头像")
    private String avatar;

    @Schema(description = "智能体描述")
    private String desc;

    @Schema(description = "智能体优先级")
    private Integer priority;

    @Schema(description = "是否受限")
    private Integer isRestricted;

    @Schema(description = "最近使用时间")
    private LocalDateTime latestTime;

    @Schema(description = "最近一次问答记录ID")
    private Long latestRecordId;

    @Schema(description = "最近一次提问内容")
    private String latestQueryContent;

    @Schema(description = "最近一次回答内容")
    private String latestResponseContent;

    @Schema(description = "最近一次回答位置")
    private Integer latestReplyPosition;

    @Schema(description = "最近一次回答是否暂停")
    private Boolean latestReplyPaused;
}
