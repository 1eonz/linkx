package com.tdtech.cloudcmd.im.jingxin.api.entity.tasks;

import java.io.Serializable;
import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author ly
 * @date 2025/8/26 16:39
 */
@Data
public class TasksProcessesCreateReq implements Serializable {

    /**
     * 任务编号
     */
    @NotBlank(message = "taskNumber不能为空")
    @Schema(description = "任务编号")
    private String taskNumber;

    /**
     * 任务处置动作。1：认领；2：转发；3：回退；4：处置；5：完成；
     * 
     */
    @NotNull(message = "action不能为空")
    @Schema(description = "任务处置动作。1：认领；2：转发；3：回退；4：处置；5：完成；")
    private Integer action;

    /**
     * 任务状态
     */
    @Schema(description = "任务状态")
    private String status;

    /**
     * 下一个处理人
     */
    @Schema(description = "下一个处理人信息")
    private List<TasksExecutors> nextExecutors;

    /**
     * 操作人
     */
    @Schema(description = "操作人")
    private String operatorName;

    /**
     * 操作人id
     */
    @Schema(description = "操作人id")
    private Long operatorId;

    /**
     * 备注
     */
    @Schema(description = "备注")
    private String remark;

}