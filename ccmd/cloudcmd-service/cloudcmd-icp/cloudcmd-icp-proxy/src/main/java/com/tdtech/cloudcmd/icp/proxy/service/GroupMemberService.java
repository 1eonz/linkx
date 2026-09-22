package com.tdtech.cloudcmd.icp.proxy.service;

import com.tdtech.cloudcmd.icp.proxy.controller.vo.GroupMemberVO;
import com.tdtech.cloudcmd.icp.proxy.entity.GroupMember;
import com.tdtech.cloudcmd.icp.proxy.repo.GroupMemberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class GroupMemberService {

    private final GroupMemberMapper groupMemberMapper;

    public void addMember(GroupMemberVO groupMemberVO) {
        String group = String.valueOf(groupMemberVO.getGroup());

        GroupMember member = new GroupMember()
            .setGroup(groupMemberVO.getGroup())
            .setIsdn(groupMemberVO.getIsdn())
            .setMembertype(groupMemberVO.getMembertype())
            .setUserpriority(groupMemberVO.getUserpriority());
        groupMemberMapper.insert(member);

        log.info("Group member added: group={}, isdn={}", group, groupMemberVO.getIsdn());
    }

    public void deleteMember(Long groupId, String isdns) {
        if (isdns == null || isdns.isEmpty()) {
            return;
        }

        List<String> isdnList = Arrays.stream(isdns.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        if (!isdnList.isEmpty()) {
            int deleted = groupMemberMapper.deleteByGroupIdAndIsdns(groupId, isdnList);
            log.info("Group members deleted: group={}, isdns={}, count={}", groupId, isdnList, deleted);
        }
    }
}
