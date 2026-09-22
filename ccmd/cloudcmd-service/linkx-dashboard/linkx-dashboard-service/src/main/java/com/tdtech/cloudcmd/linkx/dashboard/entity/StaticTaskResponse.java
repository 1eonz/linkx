package com.tdtech.cloudcmd.linkx.dashboard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 任务回复统计明细表
 * TableName：tb_static_task_response
 */
@TableName("tb_static_task_response")
@Data
public class StaticTaskResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long taskId;

    private Long responseUserId;

    private String responseUserName;

    private Long responseUserDepartmentId;

    private String responseUserDepartmentName;

    private Long responseCoopUserId;

    private String responseCoopUserName;

    private Date responseTime;

    private Long responseMsgSeqid;

    private Long responseId;

    private Date syncCreatedTime;

    private Date syncUpdatedTime;
}