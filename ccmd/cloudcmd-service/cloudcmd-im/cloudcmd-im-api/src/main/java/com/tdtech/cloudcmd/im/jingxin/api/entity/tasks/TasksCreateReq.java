package com.tdtech.cloudcmd.im.jingxin.api.entity.tasks;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author ly
 * @date 2025/8/26 16:39
 */
@Data
public class TasksCreateReq implements Serializable {

    /**
     * 业务系统生成的任务编号
     */
    @NotBlank(message = "number不能为空")
    @Schema(description = "业务系统生成的任务编号")
    private String number;

    /**
     * 名称 最小长度:2 最大长度:64
     */
    @Size(min = 2, max = 64, message = "长度必须在 2-64 个字符之间")
    @Schema(description = "名称 最小长度:2 最大长度:64")
    private String name;

    /**
     * 任务内容 最小长度:2 最大长度:512
     */
    @Schema(description = "任务内容 最小长度:2 最大长度:512")
    private String content;

    /**
     * 任务所属系统
     */
    @Schema(description = "任务所属系统")
    private String system;

    /**
     * 任务所属模块
     */
    @Schema(description = "任务所属模块")
    private String module;

    /**
     * 业务类型
     */
    @Schema(description = "业务类型")
    private String businessType;

    /**
     * 任务状态。业务系统任务状态文字描述
     */
    @NotBlank(message = "status不能为空")
    @Schema(description = "任务状态。业务系统任务状态文字描述")
    private String status;

    /**
     * 任务等级
     */
    @Schema(description = "任务等级")
    private String level;

    /**
     * 是否为紧急任务。默认为0（不紧急）
     */
    @Schema(description = "是否为紧急任务。默认为0（不紧急）")
    private Integer urgent;

    /**
     * 任务签收类型。0:不涉及(默认)。1:会签，2:或签
     */
    @Schema(description = "任务签收类型。0:不涉及(默认)。1:会签，2:或签")
    private Integer approvalType;

    /**
     * 详情地址
     */
    @Schema(description = "详情地址")
    private String url;

    /**
     * URL打开方式。1-普通H5;2-全屏H5;3-警信后台配置的H5小程序;4-协同小程序
     */
    @Schema(description = "URL打开方式。1-普通H5;2-全屏H5;3-警信后台配置的H5小程序;4-协同小程序。不传默认1")
    private Integer urlOpenType;

    /**
     * 审批页面地址
     */
    @Schema(description = "审批页面地址")
    private String approvalUrl;

    /**
     * 创建人
     */
    @Schema(description = "创建人")
    private TasksExecutors creator;

    /**
     * 执行人
     */
    @Schema(description = "执行人信息")
    private List<TasksExecutors> executors;

    /**
     * 任务开始时间
     */
    @NotNull(message = "任务开始时间不能为空")
    @Schema(description = "任务开始时间")
    private Long startTime;

    /**
     * 任务结束时间
     */
    @NotNull(message = "任务结束时间不能为空")
    @Schema(description = "任务结束时间")
    private Long endTime;

    /**
     * 任务完成时间
     */
    @Schema(description = "任务完成时间")
    private Date completeTime;

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
