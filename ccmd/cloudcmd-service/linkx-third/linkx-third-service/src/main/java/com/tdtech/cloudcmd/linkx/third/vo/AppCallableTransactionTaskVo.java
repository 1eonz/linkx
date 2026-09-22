package com.tdtech.cloudcmd.linkx.third.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AppCallableTransactionTaskVo {
    /**
     * 用户ID
     */
    private Long appCallableId;

    /**
     * 数据表Name
     */
    private String appCallableTableName;

    /**
     * 数据表ID，对应的是业务数据的linkx_id
     */
    private Long appCallableTableId;

    /**
     * 任务标准件ID
     */
    private Long taskId;

    /**
     * 任务标准件ID
     */
    private String taskNo;

    /**
     * 任务状态
     */
    private Integer taskStatus;

    /**
     * 任务下发人ID
     */
    private Long createUserId;

    /**
     * 任务接收人ID
     */
    private Long toUserId;

    /**
     * 任务下发方式。1：人工下发；2：系统自动下发
     */
    private Integer taskCreateType;

    /**
     * 创建时间
     */
    private LocalDateTime gmtCreated;


}
