package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import com.tdtech.cloudcmd.im.jingxin.server.aiclient.entity.AiVirtualUser;
import com.tdtech.cloudcmd.im.jingxin.server.aiclient.group.AiVirtualUserInterface;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.ImUserVirtualMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.Serializable;
import java.util.List;

@Service
public class GroupAiVirtualUserImpl implements AiVirtualUserInterface {

    @Resource
    private ImUserVirtualMapper imUserVirtualMapper;

    @Override
    public AiVirtualUser getUserByClientId(Serializable id) {
        // 过滤了所有没绑定agent的虚拟用户，群组AI只需要绑定了agent的虚拟用户
        return imUserVirtualMapper.selectByClientId(id);
    }

    @Override
    public AiVirtualUser getUserByAppIdIncludeUnbound(Serializable appId) {
        // 不要求已绑定agent，发送消息场景下未绑定智能体的虚拟用户也可用
        return imUserVirtualMapper.selectAiVirtualUserIncludeUnboundByAppId(appId);
    }

    @Override
    public List<AiVirtualUser> getAllUser() {
        // 过滤了所有没绑定agent的虚拟用户，群组AI只需要绑定了agent的虚拟用户
        return imUserVirtualMapper.selectAll();
    }

    @Override
    public AiVirtualUser getDefaultUser() {
        // 过滤了所有没绑定agent的虚拟用户，群组AI只需要绑定了agent的虚拟用户
        return imUserVirtualMapper.selectDefaultUser();
    }

    @Override
    public List<AiVirtualUser> getDefaultUsers() {
        return imUserVirtualMapper.selectDefaultUsers();
    }
}