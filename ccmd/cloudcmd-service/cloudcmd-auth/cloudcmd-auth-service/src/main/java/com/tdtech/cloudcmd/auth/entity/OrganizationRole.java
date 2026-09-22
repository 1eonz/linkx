package com.tdtech.cloudcmd.auth.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 角色-组织数据权限关联表
 */
@Data
@Accessors(chain = true)
@TableName("tb_organization_role")
public class OrganizationRole implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long roleId;
    
    private Long imOrgId;
    
    private Long grantUserId;
    
    private Date grantTime;
    
    private Date gmtCreated;
    
    private Date gmtModified;
}