package com.chinasoft.cloud.module.aiagent.controller.admin.vo;

import com.chinasoft.cloud.module.aiagent.dal.dataobj.AgentRecord;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 智能体记录游标分页返回结果。
 * <p>
 * 包含本页记录列表及下次拉取所需的游标（lastTime/lastId）和是否还有更多的标记。
 */
@Schema(description = "智能体记录游标分页结果")
@Data
public class AgentRecordCursorVO {

    @Schema(description = "本页记录列表")
    private List<AgentRecord> list;

    @Schema(description = "本页最后一条记录的 time（作为下次游标，列表为空时为 null）")
    private LocalDateTime lastTime;

    @Schema(description = "本页最后一条记录的 id（作为下次游标，列表为空时为 null）")
    private Long lastId;

    @Schema(description = "是否还有更多数据")
    private Boolean hasMore;
}