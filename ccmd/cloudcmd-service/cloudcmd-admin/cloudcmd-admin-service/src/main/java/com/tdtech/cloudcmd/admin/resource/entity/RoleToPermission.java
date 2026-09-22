package com.tdtech.cloudcmd.admin.resource.entity;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 角色-权限关联表
 * </p>
 *
 * @author zhuangzl
 * @since 2020-06-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class RoleToPermission implements Serializable {

    public static final String ROLE_ID = "role_id";
    public static final String PERMISSION_ID = "permission_id";
    public static final String GMT_CREATED = "gmt_created";
    public static final String GMT_MODIFIED = "gmt_modified";
    private static final long serialVersionUID = 1L;
    private Long roleId;
    private Long permissionId;
    private Date gmtCreated;
    private Date gmtModified;

}
