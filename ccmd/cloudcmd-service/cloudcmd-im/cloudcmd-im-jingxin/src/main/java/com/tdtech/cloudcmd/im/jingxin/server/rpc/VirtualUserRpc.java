package com.tdtech.cloudcmd.im.jingxin.server.rpc;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.im.jingxin.api.VirtualUserRpcApi;
import com.tdtech.cloudcmd.im.jingxin.api.entity.virtualUser.AiAssistantAgentDto;
import com.tdtech.cloudcmd.im.jingxin.api.entity.virtualUser.ImUserVirtualVO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.virtualUser.VirtualUserBindAgentVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.dto.AiAssistantAgent;
import com.tdtech.cloudcmd.im.jingxin.server.service.AiAssistantAgentService;
import com.tdtech.cloudcmd.im.jingxin.server.service.ImUserVirtualService;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@DubboService
public class VirtualUserRpc implements VirtualUserRpcApi {

    @Resource
    private AiAssistantAgentService aiAssistantAgentService;

    @Resource
    private ImUserVirtualService imUserVirtualService;

    @Override
    public List<ImUserVirtualVO> listVirtualUsers(String userName) {
        var data = imUserVirtualService.listVirtualUsers(userName);
        return BeanCopyUtils.copyList(data, ImUserVirtualVO::new);
    }

    @Override
    public List<ImUserVirtualVO> selectBinsUser(List<Long> agentIds) {
        var data = aiAssistantAgentService.selectBinsUser(agentIds);
        return BeanCopyUtils.copyList(data, ImUserVirtualVO::new);
    }

    @Override
    public Page<VirtualUserBindAgentVO> page(Long current, Long size, Long virtualUserId, Long agentId, Long createdUserId) {
        var data = aiAssistantAgentService.page(current, size, virtualUserId, agentId, createdUserId);
        Page<VirtualUserBindAgentVO> page = BeanCopyUtils.copyBean(data, Page::new);
        page.setRecords(BeanCopyUtils.copyList(data.getRecords(), VirtualUserBindAgentVO::new));
        return page;
    }

    @Override
    public boolean update(AiAssistantAgentDto aiAssistantAgent) {
        return aiAssistantAgentService.update(aiAssistantAgent);
    }

    @Override
    public AiAssistantAgentDto create(AiAssistantAgentDto aiAssistantAgent) {
        if (aiAssistantAgent.getCreatedUserId() == null) {
            aiAssistantAgent.setCreatedUserId(0L);
        }
        var data = aiAssistantAgentService.create(BeanCopyUtils.copyBean(aiAssistantAgent, AiAssistantAgent::new));
        return BeanCopyUtils.copyBean(data, AiAssistantAgentDto::new);
    }
}
