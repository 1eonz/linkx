package com.tdtech.cloudcmd.icp.proxy.service;

import com.tdtech.cloudcmd.icp.proxy.controller.vo.GroupVO;
import com.tdtech.cloudcmd.icp.proxy.entity.Group;
import com.tdtech.cloudcmd.icp.proxy.repo.GroupMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class GroupService {

    private final GroupMapper groupMapper;

    public void create(GroupVO groupVO) {
        Group group = new Group()
            .setName(groupVO.getName())
            .setPurpose(groupVO.getPurpose())
            .setCategory(groupVO.getCategory())
            .setGroup(groupVO.getGroup())
            .setPriority(groupVO.getPriority())
            .setGrpstate(true)
            .setGmtCreated(LocalDateTime.now());
        groupMapper.insert(group);

        log.info("Group created: {}", groupVO.getGroup());
    }

    public void delete(Long groupId) {
        Group group = groupMapper.selectById(groupId);
        if (group == null) {
            log.warn("Group not found: {}", groupId);
            return;
        }

        groupMapper.deleteById(groupId);
        log.info("Group deleted: {}", groupId);
    }
}
