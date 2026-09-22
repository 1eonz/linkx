package com.tdtech.cloudcmd.auth.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.auth.entity.Organization;
import com.tdtech.cloudcmd.auth.entity.Role;
import com.tdtech.cloudcmd.auth.entity.RoleToPermission;

/**
 * <p>
 * 角色-权限关联表 Mapper 接口
 * </p>
 *
 * @author zhuangzl
 * @since 2020-06-01
 */
@Mapper
public interface RoleToPermissionMapper extends BaseMapper<RoleToPermission> {

    /**
     * 根据角色ID查询功能权限
     * 
     * @param roles
     * @param applicationId
     * @return
     */
    List<String> queryPermission(@Param("roles") List<Role> roles, @Param("applicationId") Long applicationId);

    /**
     * 查询下级组织ID
     * 
     * @param organizationId
     * @return
     */
    List<Long> getLowerOrganizations(@Param("organizationId") Long organizationId);

    Organization getOrganization(@Param("id") Long orgId);
}
