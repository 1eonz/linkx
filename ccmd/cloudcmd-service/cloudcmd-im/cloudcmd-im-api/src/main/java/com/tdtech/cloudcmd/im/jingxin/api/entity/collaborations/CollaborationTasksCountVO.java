package com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Schema(description = "协同岗任务统计")
public class CollaborationTasksCountVO implements Serializable {
    @Schema(description = "待办数量")
    private Integer pendingCount = 0;

    @Schema(description = "跟踪数量")
    private Integer trackingCount = 0;

    @Schema(description = "办结数量")
    private Integer finishedCount = 0;

    @Schema(description = "无需处理数量")
    private Integer noNeedProcessCount = 0;
}