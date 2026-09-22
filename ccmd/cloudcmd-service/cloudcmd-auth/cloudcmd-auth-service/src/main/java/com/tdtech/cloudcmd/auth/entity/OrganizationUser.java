package com.tdtech.cloudcmd.auth.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 组织用户关联表
 *
 * @author system
 * @since 2024-01-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class OrganizationUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    private Long id;

    /**
     * 应用ID。引用自linkx_auth.tb_application.id
     */
    private Long applicationId;

    /**
     * 用户面ID、管理面ID等
     */
    private Long userId;

    /**
     * 警信组织部门ID
     */
    private Long imOrgId;

    /**
     * 授权用户ID
     */
    private Long grantUserId;

    /**
     * 授权时间
     */
    private LocalDateTime grantTime;

    /**
     * 创建时间
     */
    private LocalDateTime gmtCreated;

    /**
     * 最后修改时间
     */
    private LocalDateTime gmtModified;

}
