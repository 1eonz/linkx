package com.chinasoft.cloud.module.aiagent.service;

import com.chinasoft.cloud.framework.common.pojo.PageResult;
import com.chinasoft.cloud.module.aiagent.controller.admin.qo.AgentConfigQO;
import com.chinasoft.cloud.module.aiagent.controller.admin.vo.AiAssistantAgentDto;
import com.chinasoft.cloud.module.aiagent.dal.dataobj.AgentConfig;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface AiAgentConfigService {
    Long create(AgentConfig agentConfig, AiAssistantAgentDto aiAssistantAgentDto);

    void updateById(AgentConfig agentConfig, AiAssistantAgentDto aiAssistantAgentDto);

    void deleteById(Long id);

    void deleteAgentConfig(AgentConfig agentConfig);

    PageResult<AgentConfig> listPaged(AgentConfigQO qo);

    AgentConfig getById(Long id);

    AgentConfig getOneByPriority();

    Long countByCategoryId(Long categoryId);

    void template(HttpServletResponse response) throws IOException;

    List<String> importAgentsAndAttachmentsFromExcel(MultipartFile file) throws IOException;
}
