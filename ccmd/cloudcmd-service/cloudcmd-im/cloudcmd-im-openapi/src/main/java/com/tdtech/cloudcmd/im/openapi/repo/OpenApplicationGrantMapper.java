package com.tdtech.cloudcmd.im.openapi.repo;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统应用授权信息 Mapper（linkx_open 库）
 */
@Mapper
@DS("linkx_open")
public interface OpenApplicationGrantMapper extends BaseMapper<OpenApplicationGrant> {
}