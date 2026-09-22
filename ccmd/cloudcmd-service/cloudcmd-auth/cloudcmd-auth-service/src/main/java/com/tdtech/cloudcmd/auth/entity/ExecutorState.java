package com.tdtech.cloudcmd.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author gWX386658
 * @date 2020-10-1 15:14:31
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_executor_state")
public class ExecutorState implements Serializable {

    public static final String ID = "id";
    public static final String EXECUTOR_ID = "executor_id";
    public static final String STATE = "state";
    public static final String GMT_CREATED = "gmt_created";
    public static final String GMT_MODIFIED = "gmt_modified";
    private static final long serialVersionUID = 1L;
    /**
     * 主键ID
     */
    private Long id;
    /**
     * 执行者ID
     */
    private Long executorId;
    /**
     * 状态值，默认1离线
     */
    private Integer state = 0;
    /**
     * 创建时间
     */
    private LocalDateTime gmtCreated;
    /**
     * 修改时间
     */
    private LocalDateTime gmtModified;
}
