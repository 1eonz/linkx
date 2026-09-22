package com.chinasoft.cloud.module.aiagent.dal.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chinasoft.cloud.module.aiagent.dal.dataobj.AgentAttachmentConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI智能体文件上传接口配置 Mapper
 */
@Mapper
public interface AgentAttachmentConfigMapper extends BaseMapper<AgentAttachmentConfig> {
}
