package com.tdtech.cloudcmd.im.jingxin.server.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.im.jingxin.server.entity.TasksConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务标准件配置 Mapper（跨库访问 linkx_open.tb_tasks_config）
 */
@Mapper
public interface TasksConfigMapper extends BaseMapper<TasksConfig> {
}
