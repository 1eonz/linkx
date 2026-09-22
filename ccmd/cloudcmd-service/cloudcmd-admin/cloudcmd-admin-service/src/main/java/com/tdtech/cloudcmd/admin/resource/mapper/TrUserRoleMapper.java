package com.tdtech.cloudcmd.admin.resource.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.admin.resource.entity.TrUserRole;

/**
 * <p>
 * 用户-角色信息表（无勤务时，后台设定；有勤务时，根据排班设定）--角色与执行者关联还是与用户关联后续再看，现在先跟用户关联。 Mapper 接口
 * </p>
 *
 * @author mWX556161
 * @since 2020-06-23
 */
@Mapper
public interface TrUserRoleMapper extends BaseMapper<TrUserRole> {
    int countUserByRoleId(@Param("roleId") Long roleId);

    int count(@Param("roleId") Long roleId);
}
