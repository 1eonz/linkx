package com.tdtech.cloudcmd.im.jingxin.api.entity.tasks;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.beans.BeanUtils;

import lombok.Data;

/**
 * @author ly
 * @date 2025/8/26 16:39
 */
@Data
public class TasksVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @Schema(description = "id")
    private Long id;

    /**
     * 业务系统生成的任务编号
     */
    @Schema(description = "业务系统生成的任务编号")
    private String number;

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
    @Schema(description = "URL打开方式。1-普通H5;2-全屏H5;3-警信后台配置的H5小程序;4-协同小程序")
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
     * 执行人。多个用英文逗号分割
     */
    @Schema(description = "执行人。多个用英文逗号分割")
    private List<TasksExecutors> executors;

    /**
     * 任务开始时间
     */
    @Schema(description = "任务开始时间")
    private Date startTime;

    /**
     * 任务结束时间
     */
    @Schema(description = "任务结束时间")
    private Date endTime;

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
     * 操作时间
     */
    @Schema(description = "操作时间")
    private Date operateTime;

    /**
     * 修改时间
     */
    @Schema(description = "修改时间")
    private Date updateTime;

    /**
     * 是否已经收藏
     */
    @Schema(description = "是否已经收藏：true已收藏，false未收藏")
    private boolean isFavorite = false;

    /**
     * 任务类型。0：外部系统提交过来的任务（默认）；1：系统内部生成的任务
     */
    @Schema(description = "任务类型。0：外部系统提交过来的任务（默认）；1：系统内部生成的任务")
    private Integer type;

    /**
     * 关联附件
     */
    @Schema(description = "关联附件")
    private List<TasksAttachmentVO> attachments;

    public static TasksVO fromTasksView(Tasks tasks) {
        TasksVO vo = new TasksVO();
        BeanUtils.copyProperties(tasks, vo);
        vo.setSystem(tasks.getSystemName());

        TasksExecutors creator = TasksExecutors.builder().name(tasks.getCreatorName()).idCard(tasks.getCreatorIdCard())
            .department(tasks.getCreatorDepartment()).departmentId(tasks.getCreatorDepartmentId())
            .departmentCode(tasks.getCreatorDepartmentCode()).build();
        vo.setCreator(creator);
        return vo;
    }
}