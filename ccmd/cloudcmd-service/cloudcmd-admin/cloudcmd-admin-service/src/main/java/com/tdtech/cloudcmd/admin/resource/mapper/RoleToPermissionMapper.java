package com.tdtech.cloudcmd.admin.resource.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.admin.resource.entity.RoleToPermission;

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
     * 查询下级组织ID
     * 
     * @param organizationId
     * @return
     */
    List<Long> getLowerOrganizations(@Param("organizationId") Long organizationId);
}
