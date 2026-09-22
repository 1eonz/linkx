package com.tdtech.cloudcmd.admin.resource.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.admin.resource.entity.Role;

/**
 * <p>
 * 角色信息表 Mapper 接口
 * </p>
 *
 * @author zhuangzl
 * @since 2020-06-23
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    List<Role> selectByUserId(@Param("userId") Long userId);

    List<Role> selectByExecutorId(@Param("executorId") Long executorId);

    List<Role> roleByImIdCardNum(@Param("idCard") String idCard);

    List<Role> roleByImId(@Param("userId") Long userId);

}
