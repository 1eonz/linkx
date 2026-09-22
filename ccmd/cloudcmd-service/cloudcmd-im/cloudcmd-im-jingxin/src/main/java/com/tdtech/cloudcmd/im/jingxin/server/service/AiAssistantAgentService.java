package com.tdtech.cloudcmd.im.jingxin.server.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.cloudcmd.im.jingxin.api.entity.virtualUser.AiAssistantAgentDto;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImUserVirtualRespVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.dto.AiAssistantAgent;

import java.util.List;

public interface AiAssistantAgentService extends IService<AiAssistantAgent> {

    /**
     * 新增智能体和虚拟用户绑定关系。
     */
    AiAssistantAgent create(AiAssistantAgent aiAssistantAgent);

    /**
     * 根据主键查询绑定关系详情。
     */
    AiAssistantAgent detail(Long id);

    /**
     * 分页查询智能体和虚拟用户绑定关系。
     */
    Page<AiAssistantAgent> page(Long current, Long size, Long virtualUserId, Long agentId, Long createdUserId);

    /**
     * 按条件查询智能体和虚拟用户绑定关系列表。
     */
    List<AiAssistantAgent> list(Long virtualUserId, Long agentId, Long createdUserId);

    /**
     * 更新智能体和虚拟用户绑定关系。
     */
    boolean update(AiAssistantAgentDto aiAssistantAgent);

    /**
     * 根据主键删除绑定关系。
     */
    boolean deleteById(Long id);

    /**
     * 批量删除绑定关系。
     */
    boolean deleteBatch(List<Long> ids);

    int updateByPrimaryKey(AiAssistantAgent aiAssistantAgent);

    int batchInsert(List<AiAssistantAgent> list);

    int deleteByPrimaryKeyIn(List<Long> list);

    List<ImUserVirtualRespVO> selectBinsUser(List<Long> ids);
}
