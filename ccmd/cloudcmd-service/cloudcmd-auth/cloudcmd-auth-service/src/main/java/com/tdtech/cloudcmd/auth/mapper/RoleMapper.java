package com.tdtech.cloudcmd.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.auth.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 角色信息表 Mapper 接口
 * </p>
 *
 * @author zhuangzl
 * @since 2020-06-01
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    List<Role> getRoleListByUserId(@Param("userId") Long userId);

    Role selectRoleByApplicationAndUserId(@Param("userId") Long userId, @Param("roleId") Long roleId);

    Long selectUserRoleIdByUserId(@Param("userId") Long userId);
}
