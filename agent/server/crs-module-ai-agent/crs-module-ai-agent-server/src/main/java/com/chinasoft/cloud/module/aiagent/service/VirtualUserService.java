package com.chinasoft.cloud.module.aiagent.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chinasoft.cloud.module.aiagent.controller.admin.vo.AiAssistantAgentDto;
import com.chinasoft.cloud.module.aiagent.controller.admin.vo.UserVirtualVO;
import com.chinasoft.cloud.module.aiagent.controller.admin.vo.VirtualUserBindAgentVO;

import java.util.List;

public interface VirtualUserService {

    List<UserVirtualVO> listVirtualUsers(String userName);

    List<UserVirtualVO> selectBinsUser(List<Long> ids);

    Page<VirtualUserBindAgentVO> page(Long current, Long size, Long virtualUserId, Long agentId, Long createdUserId);

    Boolean update(AiAssistantAgentDto aiAssistantAgent);

    AiAssistantAgentDto create(AiAssistantAgentDto aiAssistantAgent);
}
