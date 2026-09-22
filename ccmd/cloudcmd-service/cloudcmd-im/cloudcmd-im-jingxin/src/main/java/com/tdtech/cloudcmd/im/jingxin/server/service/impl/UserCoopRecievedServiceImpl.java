package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.im.jingxin.server.entity.UserCoopRecieved;
import com.tdtech.cloudcmd.im.jingxin.server.service.IUserCoopRecievedService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.UserCoopRecievedMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 协同岗接收信息 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCoopRecievedServiceImpl extends ServiceImpl<UserCoopRecievedMapper, UserCoopRecieved>
        implements IUserCoopRecievedService {

    @Override
    @Transactional
    public void receiveCoopUser(Long coopUserId, String originPeerId, String originPeerName, Long targetOrgId,
                                String coopUserName, String iconUrl, Long orgId, String orgName) {
        // 查询是否已存在有效记录
        UserCoopRecieved existing = getByCoopUserAndOrigin(coopUserId, originPeerId);
        if (existing != null && existing.getStatus() != null && existing.getStatus() == 1) {
            // 已存在有效记录，更新详细信息（协同岗信息可能变更）
            existing.setCoopUserName(coopUserName);
            existing.setIconUrl(iconUrl);
            existing.setOrgId(orgId);
            existing.setOrgName(orgName);
            existing.setTargetOrgId(targetOrgId);
            updateById(existing);
            log.info("receiveCoopUser: already received, updated detail, coopUserId={}, originPeerId={}", coopUserId, originPeerId);
            return;
        }

        if (existing != null) {
            // 已取消的记录重新激活
            existing.setStatus(1);
            existing.setTargetOrgId(targetOrgId);
            existing.setCoopUserName(coopUserName);
            existing.setIconUrl(iconUrl);
            existing.setOrgId(orgId);
            existing.setOrgName(orgName);
            existing.setReceivedTime(LocalDateTime.now());
            existing.setCancelTime(null);
            updateById(existing);
            log.info("receiveCoopUser: re-activated, coopUserId={}, originPeerId={}", coopUserId, originPeerId);
        } else {
            UserCoopRecieved record = new UserCoopRecieved();
            record.setCoopUserId(coopUserId);
            record.setOriginPeerId(originPeerId);
            record.setOriginPeerName(originPeerName);
            record.setTargetOrgId(targetOrgId);
            record.setCoopUserName(coopUserName);
            record.setIconUrl(iconUrl);
            record.setOrgId(orgId);
            record.setOrgName(orgName);
            record.setReceivedTime(LocalDateTime.now());
            record.setStatus(1);
            save(record);
            log.info("receiveCoopUser: saved, coopUserId={}, originPeerId={}", coopUserId, originPeerId);
        }
    }

    @Override
    @Transactional
    public void cancelReceived(Long coopUserId, String originPeerId) {
        UserCoopRecieved existing = getByCoopUserAndOrigin(coopUserId, originPeerId);
        if (existing == null) {
            log.info("cancelReceived: not found, coopUserId={}, originPeerId={}", coopUserId, originPeerId);
            return;
        }
        existing.setStatus(0);
        existing.setCancelTime(LocalDateTime.now());
        updateById(existing);
        log.info("cancelReceived: cancelled, coopUserId={}, originPeerId={}", coopUserId, originPeerId);
    }

    @Override
    public List<UserCoopRecieved> listActiveReceived() {
        return list(new LambdaQueryWrapper<UserCoopRecieved>()
                .eq(UserCoopRecieved::getStatus, 1));
    }

    @Override
    public UserCoopRecieved getByCoopUserAndOrigin(Long coopUserId, String originPeerId) {
        LambdaQueryWrapper<UserCoopRecieved> wrapper = new LambdaQueryWrapper<UserCoopRecieved>()
                .eq(UserCoopRecieved::getCoopUserId, coopUserId);
        if (originPeerId == null) {
            wrapper.isNull(UserCoopRecieved::getOriginPeerId);
        } else {
            wrapper.eq(UserCoopRecieved::getOriginPeerId, originPeerId);
        }
        return getOne(wrapper);
    }

    @Override
    public UserCoopRecieved getActiveByCoopUserId(Long coopUserId) {
        return getOne(new LambdaQueryWrapper<UserCoopRecieved>()
                .eq(UserCoopRecieved::getCoopUserId, coopUserId)
                .eq(UserCoopRecieved::getStatus, 1)
                .last("LIMIT 1"));
    }
}
