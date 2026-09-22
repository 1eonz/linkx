package com.tdtech.cloudcmd.linkx.third.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * 北向应用分组信息表
 *
 * @author wb
 * @since 2026-05-09
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_app_group")
public class AppGroup implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 分组名称
     */
    private String name;

    /**
     * 分组类型。1：系统级；2：用户级
     */
    private Integer type;

    /**
     * 分组排序。用户级需要支持排序,越小越靠前
     */
    private Integer sort;

    /**
     * 创建人ID
     */
    private Long createUser;

    /**
     * 是否删除。0：否（default）；1：是
     */
    private Integer isDeleted;

    /**
     * 创建时间
     */
    private LocalDateTime gmtCreated;
}
