package com.tdtech.cloudcmd.admin.resource.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * <p>
 * 用户-角色信息表（无勤务时，后台设定；有勤务时，根据排班设定）--角色与执行者关联还是与用户关联后续再看，现在先跟用户关联。
 * </p>
 *
 * @author mWX556161
 * @since 2020-06-23
 */
@Data
@ToString
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class TrUserRole implements Serializable {

    public static final String ID = "id";
    public static final String USER_ID = "user_id";
    public static final String ROLE_ID = "role_id";
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
    private Long userId;
    /**
     * 角色ID
     */
    private Long roleId;
    /**
     * 创建时间
     */
    private LocalDateTime gmtCreated;
    /**
     * 修改时间
     */
    private LocalDateTime gmtModified;

}
