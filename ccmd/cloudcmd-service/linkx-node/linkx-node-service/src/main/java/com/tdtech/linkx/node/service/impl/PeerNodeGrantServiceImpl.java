package com.tdtech.linkx.node.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.util.json.JsonUtil;
import com.tdtech.linkx.node.dto.OpenDataGrantDTO;
import com.tdtech.linkx.node.entity.PeerNodeGrant;
import com.tdtech.linkx.node.mapper.PeerNodeGrantMapper;
import com.tdtech.linkx.node.service.IP2pWsPushService;
import com.tdtech.linkx.node.service.IPeerNodeGrantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PeerNodeGrantServiceImpl extends ServiceImpl<PeerNodeGrantMapper, PeerNodeGrant>
        implements IPeerNodeGrantService {

    private final IP2pWsPushService p2pWsPushService;

    public PeerNodeGrantServiceImpl(IP2pWsPushService p2pWsPushService) {
        this.p2pWsPushService = p2pWsPushService;
    }

    @Override
    public List<PeerNodeGrant> listByFromPeerId(String fromPeerId) {
        return list(new LambdaQueryWrapper<PeerNodeGrant>()
                .eq(PeerNodeGrant::getFromPeerId, fromPeerId)
                .eq(PeerNodeGrant::getGrantType, 1));
    }

    @Override
    public List<PeerNodeGrant> listByToPeerId(String toPeerId) {
        return list(new LambdaQueryWrapper<PeerNodeGrant>()
                .eq(PeerNodeGrant::getToPeerId, toPeerId)
                .eq(PeerNodeGrant::getGrantType, 1));
    }

    @Override
    public PeerNodeGrant getByFromAndTo(String fromPeerId, String toPeerId) {
        return getOne(new LambdaQueryWrapper<PeerNodeGrant>()
                .eq(PeerNodeGrant::getFromPeerId, fromPeerId)
                .eq(PeerNodeGrant::getToPeerId, toPeerId)
                .eq(PeerNodeGrant::getGrantType, 1));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveGrant(String fromPeerId, String fromPeerName, String toPeerId, String toPeerName,
                           String permission, String grantData, Long grantUserId) {
        PeerNodeGrant existing = getByFromAndTo(fromPeerId, toPeerId);
        if (existing != null) {
            existing.setPermission(permission);
            existing.setGrantData(grantData);
            existing.setGrantDescription(null);
            updateById(existing);
            log.info("saveGrant: updated existing grant, from={}, to={}", fromPeerId, toPeerId);
            return;
        }
        PeerNodeGrant grant = new PeerNodeGrant();
        grant.setFromPeerId(fromPeerId);
        grant.setFromPeerName(fromPeerName);
        grant.setToPeerId(toPeerId);
        grant.setToPeerName(toPeerName);
        grant.setGrantType(1);
        grant.setPermission(permission);
        grant.setGrantData(grantData);
        grant.setGrantUserId(grantUserId);
        grant.setGmtCreated(LocalDateTime.now());
        save(grant);
        log.info("saveGrant: created new grant, from={}, to={}", fromPeerId, toPeerId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeGrant(String fromPeerId, String toPeerId) {
        PeerNodeGrant existing = getByFromAndTo(fromPeerId, toPeerId);
        if (existing == null) {
            log.warn("revokeGrant: grant not found, from={}, to={}", fromPeerId, toPeerId);
            return;
        }
        // 获取授权数据后撤销
        OpenDataGrantDTO dto = (existing.getGrantData() != null)
                ? JsonUtil.parseJson(existing.getGrantData(), OpenDataGrantDTO.class)
                : new OpenDataGrantDTO();
        doRevokeGrant(fromPeerId, toPeerId);
        // 通过 WSS 通知对端取消授权（事务外副作用，隔离异常）
        String permission = buildPermissionString(dto != null ? dto : new OpenDataGrantDTO());
        try {
            p2pWsPushService.pushAuthRevoke(toPeerId, fromPeerId, toPeerId, permission, null, null);
        } catch (Exception e) {
            log.error("revokeGrant: pushAuthRevoke failed, toPeerId={}", toPeerId, e);
        }
        log.info("revokeGrant: pushed auth revoke to peerId={}, from={}, to={}", toPeerId, fromPeerId, toPeerId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeGrantFromRemote(String fromPeerId, String toPeerId) {
        doRevokeGrant(fromPeerId, toPeerId);
        // 被动接收，不再通知对端
    }

    private void doRevokeGrant(String fromPeerId, String toPeerId) {
        PeerNodeGrant existing = getByFromAndTo(fromPeerId, toPeerId);
        if (existing == null) {
            log.warn("doRevokeGrant: grant not found, from={}, to={}", fromPeerId, toPeerId);
            return;
        }
        existing.setGrantType(0);
        updateById(existing);
        log.info("doRevokeGrant: revoked grant, from={}, to={}", fromPeerId, toPeerId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdateGrant(String fromPeerId, String toPeerId, OpenDataGrantDTO dto, Long userId) {
        doSaveOrUpdateGrant(fromPeerId, toPeerId, dto, userId);
        // 通过 WSS 通知对端保存授权数据（事务外副作用，隔离异常）
        try {
            notifyPeerGrantViaWs(fromPeerId, toPeerId, dto);
        } catch (Exception e) {
            log.error("saveOrUpdateGrant: notifyPeerGrantViaWs failed, toPeerId={}", toPeerId, e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdateGrantFromRemote(String fromPeerId, String toPeerId, OpenDataGrantDTO dto) {
        doSaveOrUpdateGrant(fromPeerId, toPeerId, dto, null);
        // 被动接收，不再通知对端，避免循环
    }

    private void doSaveOrUpdateGrant(String fromPeerId, String toPeerId, OpenDataGrantDTO dto, Long userId) {
        String grantData = JsonUtil.toJsonStr(dto);
        PeerNodeGrant existing = getByFromAndTo(fromPeerId, toPeerId);
        if (existing != null) {
            existing.setGrantData(grantData);
            existing.setGrantType(1);
            existing.setGrantUserId(userId);
            updateById(existing);
            log.info("doSaveOrUpdateGrant: updated existing grant, from={}, to={}", fromPeerId, toPeerId);
        } else {
            PeerNodeGrant grant = new PeerNodeGrant();
            grant.setFromPeerId(fromPeerId);
            grant.setToPeerId(toPeerId);
            grant.setGrantType(1);
            grant.setGrantData(grantData);
            grant.setGrantUserId(userId);
            grant.setGmtCreated(LocalDateTime.now());
            save(grant);
            log.info("doSaveOrUpdateGrant: created new grant, from={}, to={}", fromPeerId, toPeerId);
        }
    }

    /**
     * 通过 WSS 通知对端节点保存授权数据。
     */
    private void notifyPeerGrantViaWs(String fromPeerId, String toPeerId, OpenDataGrantDTO dto) {
        String permission = buildPermissionString(dto);
        p2pWsPushService.pushAuthGrant(toPeerId, fromPeerId, toPeerId, permission, null, null, null);
        log.info("notifyPeerGrantViaWs: pushed auth grant to peerId={}, permission={}", toPeerId, permission);
    }

    /**
     * 将 OpenDataGrantDTO 转换为 permission 字符串（如 "org/dashboard/coopuser"）
     */
    private String buildPermissionString(OpenDataGrantDTO dto) {
        List<String> parts = new ArrayList<>();
        if (dto.getOrg() != null && dto.getOrg() == 1) {
            parts.add("org");
        }
        if (dto.getDashboard() != null && dto.getDashboard() == 1) {
            parts.add("dashboard");
        }
        if (dto.getCoopUser() != null && dto.getCoopUser() == 1) {
            parts.add("coopuser");
        }
        if (dto.getH5() != null && dto.getH5() == 1) {
            parts.add("h5");
        }

        return String.join("/", parts);
    }

    @Override
    public OpenDataGrantDTO getGrantDTO(String fromPeerId, String toPeerId) {
        PeerNodeGrant grant = getByFromAndTo(fromPeerId, toPeerId);
        if (grant == null || grant.getGrantData() == null) {
            return new OpenDataGrantDTO();
        }
        OpenDataGrantDTO dto = JsonUtil.parseJson(grant.getGrantData(), OpenDataGrantDTO.class);
        return dto != null ? dto : new OpenDataGrantDTO();
    }

    @Override
    public Map<String, OpenDataGrantDTO> getGrantDTOMap(String fromPeerId, List<String> toPeerIds) {
        if (toPeerIds == null || toPeerIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<PeerNodeGrant> grants = list(new LambdaQueryWrapper<PeerNodeGrant>()
                .eq(PeerNodeGrant::getFromPeerId, fromPeerId)
                .in(PeerNodeGrant::getToPeerId, toPeerIds)
                .eq(PeerNodeGrant::getGrantType, 1));
        return grants.stream()
                .collect(Collectors.toMap(
                        PeerNodeGrant::getToPeerId,
                        g -> {
                            if (g.getGrantData() == null) {
                                return new OpenDataGrantDTO();
                            }
                            OpenDataGrantDTO dto = JsonUtil.parseJson(g.getGrantData(), OpenDataGrantDTO.class);
                            return dto != null ? dto : new OpenDataGrantDTO();
                        },
                        (a, b) -> a));
    }

    @Override
    public Map<String, OpenDataGrantDTO> getGrantDTOMapByTo(String toPeerId, List<String> fromPeerIds) {
        if (fromPeerIds == null || fromPeerIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<PeerNodeGrant> grants = list(new LambdaQueryWrapper<PeerNodeGrant>()
                .eq(PeerNodeGrant::getToPeerId, toPeerId)
                .in(PeerNodeGrant::getFromPeerId, fromPeerIds)
                .eq(PeerNodeGrant::getGrantType, 1));
        return grants.stream()
                .collect(Collectors.toMap(
                        PeerNodeGrant::getFromPeerId,
                        g -> {
                            if (g.getGrantData() == null) {
                                return new OpenDataGrantDTO();
                            }
                            OpenDataGrantDTO dto = JsonUtil.parseJson(g.getGrantData(), OpenDataGrantDTO.class);
                            return dto != null ? dto : new OpenDataGrantDTO();
                        },
                        (a, b) -> a));
    }
}
