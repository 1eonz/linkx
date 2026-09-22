package com.tdtech.cloudcmd.im.jingxin.api.entity.tasks;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author ly
 * @date 2025/8/26 16:39
 */
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_tasks")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Tasks implements Serializable {

    private Long id;

    /**
     * 业务系统生成的任务编号
     */
    private String number;

    /**
     * 名称 最小长度:2 最大长度:64
     */
    private String name;

    /**
     * 任务内容 最小长度:2 最大长度:512
     */
    private String content;

    /**
     * 任务所属系统
     */
    private String systemName;

    /**
     * 任务所属模块
     */
    private String module;

    /**
     * 业务类型
     */
    private String businessType;

    /**
     * 任务状态。业务系统任务状态文字描述
     */
    private String status;

    /**
     * 任务等级
     */
    private String level;

    /**
     * 是否为紧急任务。默认为0（不紧急）
     */
    private Integer urgent;

    /**
     * 任务签收类型。0:不涉及(默认)。1:会签，2:或签
     */
    private Integer approvalType;

    /**
     * 详情地址
     */
    private String url;

    /**
     * URL打开方式。1-普通H5;2-全屏H5;3-警信后台配置的H5小程序;4-协同小程序
     */
    private Integer urlOpenType;

    /**
     * 审批页面地址
     */
    private String approvalUrl;

    /**
     * 创建人姓名
     */
    private String creatorName;

    /**
     * 创建人身份证号
     */
    private String creatorIdCard;

    /**
     * 创建人所属部门
     */
    private String creatorDepartment;

    /**
     * 创建人所属部门ID
     */
    private String creatorDepartmentId;

    /**
     * 创建人所属部门编码
     */
    private String creatorDepartmentCode;

    /**
     * 任务开始时间
     */
    private Date startTime;

    /**
     * 任务结束时间
     */
    private Date endTime;

    /**
     * 任务完成时间
     */
    private Date completeTime;

    /**
     * 任务扩展描述
     */
    @TableField("`extend`")
    private String extend;

    /**
     * 操作时间
     */
    private Date operateTime;

    /**
     * 修改时间
     */
    private Date updateTime;

    /**
     * 任务类型。0：外部系统提交过来的任务（默认）；1：系统内部生成的任务
     */
    private Integer type;
}