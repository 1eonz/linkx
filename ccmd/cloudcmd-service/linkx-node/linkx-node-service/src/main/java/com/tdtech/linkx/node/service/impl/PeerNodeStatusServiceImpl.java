package com.tdtech.linkx.node.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.linkx.node.entity.PeerNodeStatus;
import com.tdtech.linkx.node.enums.PeerNodeTypeEnum;
import com.tdtech.linkx.node.mapper.PeerNodeStatusMapper;
import com.tdtech.linkx.node.service.IPeerNodeStatusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class PeerNodeStatusServiceImpl extends ServiceImpl<PeerNodeStatusMapper, PeerNodeStatus>
        implements IPeerNodeStatusService {

    @Override
    public PeerNodeStatus getByPeerId(String peerId) {
        return getOne(new LambdaQueryWrapper<PeerNodeStatus>()
                .eq(PeerNodeStatus::getPeerId, peerId));
    }

    @Override
    public List<PeerNodeStatus> listByPeerType(PeerNodeTypeEnum peerType) {
        return list(new LambdaQueryWrapper<PeerNodeStatus>()
                .eq(PeerNodeStatus::getPeerType, peerType.getCode()));
    }

    @Override
    public List<PeerNodeStatus> listAll() {
        return list();
    }

    @Override
    public void updateStatus(String peerId, Integer status) {
        PeerNodeStatus entity = getByPeerId(peerId);
        if (entity == null) {
            log.warn("updateStatus: peerId={} not found", peerId);
            return;
        }
        entity.setStatus(status);
        entity.setGmtModified(LocalDateTime.now());
        updateById(entity);
    }

    @Override
    public void updateSession(String peerId, String session) {
        PeerNodeStatus entity = getByPeerId(peerId);
        if (entity == null) {
            log.warn("updateSession: peerId={} not found", peerId);
            return;
        }
        entity.setSession(session);
        entity.setGmtModified(LocalDateTime.now());
        updateById(entity);
    }

    @Override
    public void updateLastSeen(String peerId) {
        PeerNodeStatus entity = getByPeerId(peerId);
        if (entity == null) {
            return;
        }
        entity.setLastSeen(LocalDateTime.now());
        entity.setGmtModified(LocalDateTime.now());
        updateById(entity);
    }

    @Override
    public void updateAuthFail(String peerId, String reason) {
        PeerNodeStatus entity = getByPeerId(peerId);
        if (entity == null) {
            return;
        }
        entity.setLastAuthFailReason(reason);
        entity.setGmtModified(LocalDateTime.now());
        updateById(entity);
    }

    @Override
    public void updateRetryStrategy(String peerId, Integer retryStrategy) {
        PeerNodeStatus entity = getByPeerId(peerId);
        if (entity == null) {
            return;
        }
        entity.setRetryStrategy(retryStrategy);
        entity.setGmtModified(LocalDateTime.now());
        updateById(entity);
    }

    @Override
    public void updateJwt(String peerId, String jwt) {
        PeerNodeStatus entity = getByPeerId(peerId);
        if (entity == null) {
            return;
        }
        entity.setJwt(jwt);
        entity.setGmtModified(LocalDateTime.now());
        updateById(entity);
    }

    @Override
    public void removeByPeerId(String peerId) {
        remove(new LambdaQueryWrapper<PeerNodeStatus>()
                .eq(PeerNodeStatus::getPeerId, peerId));
    }
}
