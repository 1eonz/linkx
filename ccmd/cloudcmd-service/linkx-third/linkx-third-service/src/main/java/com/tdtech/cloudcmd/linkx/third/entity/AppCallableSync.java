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
 * 南向应用数据同步信息表
 * </p>
 *
 * @author author
 * @since 2026-04-15
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_app_callable_sync")
public class AppCallableSync implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * tb_app_callable.id
     */
    private String appCallableId;

    /**
     * 接口执行频率,(单位: 秒)
     */
    private Integer frequency;

    /**
     * 接口任务执行状态。0：未启动；1：运行中；2：正常结束；3：异常结束
     */
    private Integer running;

    /**
     * 任务开始时间
     */
    private LocalDateTime startTime;

    /**
     * 任务结束时间
     */
    private LocalDateTime endTime;

    /**
     * 执行结果（接口调用或数据库同步的结果信息）
     */
    private String result;

    /**
     * 执行器类型。1：forward；2：backward
     */
    private Integer executorType;

    /**
     * 程序执行过程中的临时数据记录（辅助版本升级场景）
     */
    private String extendsData;

    /**
     * 创建时间
     */
    private LocalDateTime gmtCreated;


}