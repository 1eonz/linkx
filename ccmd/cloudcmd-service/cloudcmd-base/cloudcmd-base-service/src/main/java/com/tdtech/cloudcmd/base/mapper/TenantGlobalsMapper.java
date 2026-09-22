package com.tdtech.cloudcmd.base.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.base.entity.Globals;
import com.tdtech.cloudcmd.base.entity.TenantGlobals;


/**
 * @author hks
 * @date 2024/7/2
 */
@Mapper
public interface TenantGlobalsMapper extends BaseMapper<TenantGlobals> {
    List<Globals> selectGlobalsByOrganizationId(@Param("organizationId") Long organizationId);
}
