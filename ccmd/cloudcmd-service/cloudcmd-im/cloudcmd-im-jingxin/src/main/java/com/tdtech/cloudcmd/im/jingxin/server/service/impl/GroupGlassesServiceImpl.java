package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import com.tdtech.cloudcmd.im.jingxin.client.ImHttpClient;
import com.tdtech.cloudcmd.im.jingxin.client.entity.GroupMembers;
import com.tdtech.cloudcmd.im.jingxin.client.entity.GroupVo;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CreateGroup;
import com.tdtech.cloudcmd.im.jingxin.server.service.GroupGlassesService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.CreateGroupMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;


/**
 * @author: S063874
 * @date: 2026-02-26 16:38
 */
@Slf4j
@Service
public class GroupGlassesServiceImpl implements GroupGlassesService {

    @Autowired
    private CreateGroupMapper createGroupMapper;

    @Resource
    @Qualifier("coopImHttpClient")
    private ImHttpClient imHttpClient;

    @Override
    public CreateGroup getGropInfoByGroupId(Long groupId) {
        CreateGroup createGroup = createGroupMapper.selectByGroupId(groupId);
        if(createGroup == null){
            log.info("群组暂未创建成功，从警信获取一下群组信息");
        }
        GroupVo groupVo = imHttpClient.queryGroupDetail(groupId);
        if(groupVo == null){
            log.info("从警信获取群消息失败，groupId:{}",groupId);
            return null;
        }
        createGroup = new CreateGroup();
        createGroup.setGroupId(groupId);
        createGroup.setGroupName(
                StringUtils.isNotBlank(groupVo.getName()) ? groupVo.getName() : groupVo.getUndefinedName());
        List<GroupMembers> groupMembers = groupVo.getGroupMembers();
        for (GroupMembers groupMember : groupMembers) {
            if (groupMember.getRole().equals(2)) {
                createGroup.setOwnerId(groupMember.getUserId() + "");
                createGroup.setOwnerName(groupMember.getName());
            }
        }
        return createGroup;
    }
}
