package com.tdtech.cloudcmd.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.auth.entity.OrganizationRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrganizationRoleMapper extends BaseMapper<OrganizationRole> {

    void deleteByRoleId(@Param("roleId") Long roleId);

    void insertBatch(@Param("orgRoles") List<OrganizationRole> orgRoles);

    List<OrganizationRole> selectByRoleIds(@Param("roleIds") List<Long> roleIds);

    void deleteByRoleIds(@Param("roleIds") List<Long> roleIds);
}