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
 * 北向应用使用记录表
 *
 * @author wb
 * @since 2026-05-09
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_user_app_used")
public class UserAppUsed implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 应用ID
     */
    private Long appId;

    /**
     * 创建人ID
     */
    private Long userId;

    /**
     * 打开的端侧。1：app；2：BS PC；3：CS PC
     */
    private Integer client;

    /**
     * 应用打开时间
     */
    private LocalDateTime time;

    /**
     * 创建时间
     */
    private LocalDateTime gmtCreated;
}
