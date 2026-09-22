package com.tdtech.cloudcmd.linkx.third.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tdtech.cloudcmd.linkx.third.entity.LocationShareAction;
import com.tdtech.cloudcmd.linkx.third.entity.LocationShareMember;
import com.tdtech.cloudcmd.linkx.third.mapper.LocationShareActionMapper;
import com.tdtech.cloudcmd.linkx.third.mapper.LocationShareMemberMapper;
import com.tdtech.cloudcmd.linkx.third.mapper.LocationSharedToMapper;
import com.tdtech.cloudcmd.linkx.third.service.LocationShareService;
import com.tdtech.cloudcmd.linkx.third.vo.LocationShareCreateVo;
import com.tdtech.cloudcmd.linkx.third.vo.LocationShareDetailVo;
import com.tdtech.cloudcmd.linkx.third.vo.LocationShareExitVo;
import com.tdtech.cloudcmd.linkx.third.vo.LocationShareJoinVo;
import com.tdtech.cloudcmd.linkx.third.vo.LocationShareMemberVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationShareServiceImpl implements LocationShareService {

    private final LocationShareActionMapper actionMapper;
    private final LocationShareMemberMapper memberMapper;
    private final LocationSharedToMapper sharedToMapper;

    @Transactional
    public Long createLocationShare(LocationShareCreateVo vo) {
        LocationShareAction action = new LocationShareAction()
            .setUserId(vo.getUserId())
            .setUdcGroup(vo.getUdcGroup())
            .setStatus(1)
            .setGmtCreated(LocalDateTime.now());
        actionMapper.insert(action);

        log.info("Location share created: actionId={}, userId={}, udcGroup={}", action.getId(), vo.getUserId(), vo.getUdcGroup());
        return action.getId();
    }

    public LocationShareDetailVo getMyLocationShare(Long shareId) {
        LocationShareAction action = actionMapper.selectOne(
            Wrappers.lambdaQuery(LocationShareAction.class)
                .eq(LocationShareAction::getId, shareId)
                .orderByDesc(LocationShareAction::getGmtCreated)
                .last("LIMIT 1"));

        if (action == null) {
            return null;
        }

        LocationShareDetailVo detail = new LocationShareDetailVo();
        detail.setId(action.getId());
        detail.setUserId(action.getUserId());
        detail.setUdcGroup(action.getUdcGroup());
        detail.setStatus(action.getStatus());
        detail.setGmtCreated(action.getGmtCreated());
        detail.setCloseTime(action.getCloseTime());

        List<LocationShareMember> members = memberMapper.selectList(
            Wrappers.lambdaQuery(LocationShareMember.class)
                .eq(LocationShareMember::getLocationShareActionId, action.getId())
                .isNull(LocationShareMember::getExitTime));

        detail.setMembers(members.stream().map(m -> {
            LocationShareMemberVo memberVo = new LocationShareMemberVo();
            memberVo.setId(m.getId());
            memberVo.setUserId(m.getUserId());
            memberVo.setIsdn(m.getIsdn());
            memberVo.setJoinTime(m.getJoinTime());
            memberVo.setGisShareStartTime(m.getGisShareStartTime());
            memberVo.setGisShareEndTime(m.getGisShareEndTime());
            memberVo.setExitType(m.getExitType());
            memberVo.setExitTime(m.getExitTime());
            return memberVo;
        }).collect(Collectors.toList()));

        return detail;
    }

    @Transactional
    public void joinLocationShare(Long shareId, LocationShareJoinVo vo) {
        LocationShareAction action = actionMapper.selectById(shareId);
        if (action == null || action.getStatus() != 1) {
            throw new RuntimeException("位置共享不存在或已结束");
        }

        LocationShareMember existing = memberMapper.selectOne(
            Wrappers.lambdaQuery(LocationShareMember.class)
                .eq(LocationShareMember::getLocationShareActionId, shareId)
                .eq(LocationShareMember::getUserId, vo.getUserId())
                .isNull(LocationShareMember::getExitTime));

        if (existing != null) {
            log.info("User already in location share: shareId={}, userId={}", shareId, vo.getUserId());
            return;
        }

        LocationShareMember member = new LocationShareMember()
            .setLocationShareActionId(shareId)
            .setUserId(vo.getUserId())
            .setIsdn(vo.getIsdn())
            .setJoinTime(LocalDateTime.now());
        memberMapper.insert(member);

        log.info("User joined location share: shareId={}, userId={}", shareId, vo.getUserId());
    }

    @Transactional
    public void exitLocationShare(Long shareId, LocationShareExitVo vo) {
        LocationShareAction action = actionMapper.selectById(shareId);
        // 只有与创建人相关的退出操作才记录
        if (action != null && Objects.equals(action.getUserId(), vo.getUserId())) {
            action.setStatus(0);
            action.setCloseTime(LocalDateTime.now());
            actionMapper.updateById(action);
        }
    }

}
