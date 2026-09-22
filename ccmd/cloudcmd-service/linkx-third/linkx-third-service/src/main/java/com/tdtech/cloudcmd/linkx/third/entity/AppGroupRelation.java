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
 * 北向应用分组信息关联表
 *
 * @author wb
 * @since 2026-05-09
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tr_app_group")
public class AppGroupRelation implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 应用分组ID
     */
    private Long appGroupId;

    /**
     * 应用ID
     */
    private Long appId;

    /**
     * 应用关联分组ID的人员ID
     */
    private Long createUserId;

    /**
     * 是否删除。0：否（default）；1：是
     */
    private Integer isDeleted;

    /**
     * 创建时间
     */
    private LocalDateTime gmtCreated;
}
