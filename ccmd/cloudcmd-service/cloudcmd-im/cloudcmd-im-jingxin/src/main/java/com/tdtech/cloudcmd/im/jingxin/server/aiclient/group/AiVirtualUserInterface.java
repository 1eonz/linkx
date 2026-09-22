package com.tdtech.cloudcmd.im.jingxin.server.aiclient.group;

import com.tdtech.cloudcmd.im.jingxin.server.aiclient.entity.AiVirtualUser;

import java.io.Serializable;
import java.util.List;

public interface AiVirtualUserInterface {

    AiVirtualUser getUserByClientId(Serializable id);

    /**
     * 按 appId 查询虚拟用户（包含未绑定智能体的）。
     * 与 getUserByClientId 的区别：不附带 agent_id is not null 过滤，
     * 未绑定智能体的虚拟用户（如三方平台双向虚拟用户）也能查到，用于发送消息场景。
     */
    AiVirtualUser getUserByAppIdIncludeUnbound(Serializable appId);

    List<AiVirtualUser> getAllUser();

    AiVirtualUser getDefaultUser();

    /**
     * 查询所有默认虚拟用户（已绑定智能体），用于多默认用户场景的负载均衡
     */
    List<AiVirtualUser> getDefaultUsers();
}