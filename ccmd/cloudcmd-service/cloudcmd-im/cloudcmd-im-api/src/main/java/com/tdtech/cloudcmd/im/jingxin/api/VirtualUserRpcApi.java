package com.tdtech.cloudcmd.im.jingxin.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.im.jingxin.api.entity.virtualUser.AiAssistantAgentDto;
import com.tdtech.cloudcmd.im.jingxin.api.entity.virtualUser.ImUserVirtualVO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.virtualUser.VirtualUserBindAgentVO;

import java.util.List;

public interface VirtualUserRpcApi {

    List<ImUserVirtualVO> listVirtualUsers(String userName);

    List<ImUserVirtualVO> selectBinsUser(List<Long> ids);

    Page<VirtualUserBindAgentVO> page(Long current, Long size, Long virtualUserId, Long agentId, Long createdUserId);

    boolean update(AiAssistantAgentDto aiAssistantAgent);

    AiAssistantAgentDto create(AiAssistantAgentDto aiAssistantAgent);
}
