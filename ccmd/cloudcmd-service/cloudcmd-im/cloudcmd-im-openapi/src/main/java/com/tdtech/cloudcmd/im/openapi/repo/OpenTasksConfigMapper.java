package com.tdtech.cloudcmd.im.openapi.repo;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务标准件配置 Mapper（linkx_open 库）
 */
@Mapper
@DS("linkx_open")
public interface OpenTasksConfigMapper extends BaseMapper<OpenTasksConfig> {
}