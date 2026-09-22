package com.tdtech.cloudcmd.admin.resource.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.admin.resource.entity.UserCommonAppDO;
import org.apache.ibatis.annotations.Param;

/**
 * @author lsc
 * @date 2025/7/17
 **/
@Mapper
public interface UserCommonAppMapper extends BaseMapper<UserCommonAppDO> {

    List<UserCommonAppDO> listByUserId(@Param("userId") String userId, @Param("terminalType") Integer terminalType);

    Integer saveInitStatus(@Param("userId") String userId, @Param("terminalType") Integer terminalType);

    String getInitStatus(@Param("userId") String userId, @Param("terminalType") Integer terminalType);
}