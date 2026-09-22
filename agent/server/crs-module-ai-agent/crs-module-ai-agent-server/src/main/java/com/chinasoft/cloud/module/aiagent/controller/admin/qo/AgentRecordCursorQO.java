package com.chinasoft.cloud.module.aiagent.controller.admin.qo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 智能体记录游标分页查询参数。
 * <p>
 * 用于统计定时任务增量拉取，配合 (time, id) 双游标避免同秒多记录漏数据。
 */
@Schema(description = "智能体记录游标分页查询")
@Data
public class AgentRecordCursorQO {

    @Schema(description = "智能体配置ID（人员核查传 -1）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long agentConfigId;

    @Schema(description = "游标：上次拉取的最后一条记录的 time。首次拉取传 null")
    private LocalDateTime timeAfter;

    @Schema(description = "游标：上次拉取的最后一条记录的 id。首次拉取传 null，需与 timeAfter 同时传")
    private Long idAfter;

    @Schema(description = "每页大小，默认 10，最大 100")
    private Integer size;
}