package com.tdtech.linkx.node.service.impl;

import com.tdtech.linkx.node.dto.OpenDataGrantDTO;
import com.tdtech.linkx.node.service.IPeerNodeGrantService;
import com.tdtech.linkx.node.service.IWsAuthGrantMessageHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * WebSocket 开放数据授权消息处理器实现
 */
@Slf4j
@Service
public class WsAuthGrantMessageHandlerImpl implements IWsAuthGrantMessageHandler {

    private final IPeerNodeGrantService peerNodeGrantService;

    public WsAuthGrantMessageHandlerImpl(@Lazy IPeerNodeGrantService peerNodeGrantService) {
        this.peerNodeGrantService = peerNodeGrantService;
    }

    @Override
    public void handleAuthGrant(String peerId, Map<String, Object> payload) {
        String grantFromPeerId = (String) payload.get("grantFromPeerId");
        String grantToPeerId = (String) payload.get("grantToPeerId");
        String permission = (String) payload.get("permission");

        if (grantFromPeerId == null || grantToPeerId == null) {
            log.warn("handleAuthGrant: missing grantFromPeerId or grantToPeerId, payload={}", payload);
            return;
        }

        OpenDataGrantDTO dto = parsePermission(permission);

        // 保存授权数据：from=对端, to=本端（被动接收，不再通知对端）
        peerNodeGrantService.saveOrUpdateGrantFromRemote(grantFromPeerId, grantToPeerId, dto);
        log.info("handleAuthGrant: saved grant from={}, to={}, permission={}",
                grantFromPeerId, grantToPeerId, permission);
    }

    @Override
    public void handleAuthRevoke(String peerId, Map<String, Object> payload) {
        String grantFromPeerId = (String) payload.get("grantFromPeerId");
        String grantToPeerId = (String) payload.get("grantToPeerId");

        if (grantFromPeerId == null || grantToPeerId == null) {
            log.warn("handleAuthRevoke: missing grantFromPeerId or grantToPeerId, payload={}", payload);
            return;
        }

        peerNodeGrantService.revokeGrantFromRemote(grantFromPeerId, grantToPeerId);
        log.info("handleAuthRevoke: revoked grant from={}, to={}", grantFromPeerId, grantToPeerId);
    }

    /**
     * 将 permission 字符串（如 "org/dashboard/coopuser"）解析为 OpenDataGrantDTO
     */
    private OpenDataGrantDTO parsePermission(String permission) {
        OpenDataGrantDTO dto = new OpenDataGrantDTO();
        if (permission == null || permission.isEmpty()) {
            return dto;
        }
        String[] parts = permission.split("/");
        for (String part : parts) {
            switch (part.trim().toLowerCase()) {
                case "org":
                    dto.setOrg(1);
                    break;
                case "dashboard":
                    dto.setDashboard(1);
                    break;
                case "coopuser":
                    dto.setCoopUser(1);
                    break;
                case "h5":
                    dto.setH5(1);
                    break;
                default:
                    log.warn("parsePermission: unknown permission item={}, skip", part);
            }
        }
        return dto;
    }
}
