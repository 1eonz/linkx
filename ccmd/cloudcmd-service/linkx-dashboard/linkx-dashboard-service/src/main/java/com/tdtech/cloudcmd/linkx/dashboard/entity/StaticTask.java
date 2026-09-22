package com.tdtech.cloudcmd.linkx.dashboard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 协同任务统计明细表
 * TableName：tb_static_task
 */
@TableName("tb_static_task")
@Data
public class StaticTask implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long taskId;

    private Long fromUserId;

    private String fromUserName;

    private Long fromUserDepartmentId;

    private String fromUserDepartmentName;

    private Integer status;

    private Date msgSentTime;

    private Long msgSeqid;

    private Long postId;

    private String postName;

    private Date createTime;

    private Date expiredTime;

    private Date responseTime;

    private Long responseUserId;

    private String responseUserName;

    private Date ignoreTime;

    private Long ignoreUserId;

    private String ignoreUserName;

    private Date trackTime;

    private Long trackUserId;

    private String trackUserName;

    private Date finishTime;

    private Long finishUserId;

    private String finishUserName;

    private Date taskUpdatedTime;

    private Date syncCreatedTime;

    private Date syncUpdatedTime;
}