package com.tdtech.cloudcmd.auth.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 执行者-用户关联信息表（1对1映射。为执行者直接创建用户）
 * </p>
 *
 * @author szc
 * @since 2020-08-03
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tr_executor_user")
public class ExecutorUser implements Serializable {

    public static final String ID = "id";
    public static final String EXECUTOR_ID = "executor_id";
    public static final String USER_ID = "user_id";
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
     * 资源关联的用户ID.( 资源可以开户，也可以不开户)
     */
    private Long userId;
    /**
     * 创建时间
     */
    private LocalDateTime gmtCreated;
    /**
     * 修改时间
     */
    private LocalDateTime gmtModified;

}
