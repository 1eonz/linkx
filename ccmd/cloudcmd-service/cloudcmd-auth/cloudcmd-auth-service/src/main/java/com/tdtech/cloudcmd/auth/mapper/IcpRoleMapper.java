package com.tdtech.cloudcmd.auth.mapper;

import com.tdtech.cloudcmd.auth.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IcpRoleMapper {

    List<Role> selectRolesWithOrgPriv();
}