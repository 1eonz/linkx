package com.chinasoft.cloud.module.aiagent.dal.mysql;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import com.chinasoft.cloud.framework.mybatis.core.mapper.BaseMapperX;
import com.chinasoft.cloud.module.aiagent.dal.dataobj.AgentConfig;

@Mapper
public interface AgentConfigMapper extends BaseMapperX<AgentConfig> {

    @Select("""
        select * from ai_agent_config where deleted = 0 order by priority , id limit 1
        """)
    AgentConfig getOneByPriority();

}
