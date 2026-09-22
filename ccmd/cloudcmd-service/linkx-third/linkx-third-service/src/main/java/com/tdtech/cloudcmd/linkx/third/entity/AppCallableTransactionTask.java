package com.tdtech.cloudcmd.linkx.third.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 南向应用事务任务关系表
 * </p>
 *
 * @author author
 * @since 2026-04-15
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_app_callable_transaction_task")
public class AppCallableTransactionTask implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

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
    private String taskStatus;

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
