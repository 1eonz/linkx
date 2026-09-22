package com.tdtech.cloudcmd.im.jingxin.api.entity.tasks;

import java.io.Serializable;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author ly
 * @date 2025/8/26 16:39
 */
@Data
public class TasksUpdateReq implements Serializable {

    /**
     * 名称 最小长度:2 最大长度:64
     */
    @Schema(description = "名称 最小长度:2 最大长度:64")
    private String name;

    /**
     * 任务内容 最小长度:2 最大长度:512
     */
    @Schema(description = "任务内容 最小长度:2 最大长度:512")
    private String content;

    /**
     * 是否为紧急任务。默认为0（不紧急）
     */
    @Schema(description = "是否为紧急任务。默认为0（不紧急）")
    private Integer urgent;

    /**
     * 详情地址
     */
    @Schema(description = "详情地址")
    private String url;

    /**
     * 审批页面地址
     */
    @Schema(description = "审批页面地址")
    private String approvalUrl;

    /**
     * 任务签收类型。0:不涉及(默认)。1:会签，2:或签
     */
    @Schema(description = "任务签收类型。0:不涉及(默认)。1:会签，2:或签")
    private Integer approvalType;

    /**
     * 任务签收类型。0:不涉及(默认)。1:会签，2:或签
     */
    @Schema(description = "执行人信息")
    private List<TasksExecutors> executors;

    /**
     * 任务开始时间
     */
    @Schema(description = "任务开始时间")
    private Long startTime;

    /**
     * 任务结束时间
     */
    @Schema(description = "任务结束时间")
    private Long endTime;

    /**
     * 任务扩展描述
     */
    @Schema(description = "任务扩展描述")
    private String extend;


    /**
     * 任务类型。0：外部系统提交过来的任务（默认）；1：系统内部生成的任务
     */
    @Schema(description = "任务类型。0：外部系统提交过来的任务（默认）；1：系统内部生成的任务")
    private Integer type;

}
