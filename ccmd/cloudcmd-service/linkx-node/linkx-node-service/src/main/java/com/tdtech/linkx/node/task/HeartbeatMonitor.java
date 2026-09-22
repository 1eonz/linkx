package com.tdtech.linkx.node.task;

import com.tdtech.linkx.node.entity.PeerNodeStatus;
import com.tdtech.linkx.node.enums.NodeStatusEnum;
import com.tdtech.linkx.node.enums.PeerNodeTypeEnum;
import com.tdtech.linkx.node.service.IPeerNodeStatusService;
import com.tdtech.linkx.node.ws.server.SessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class HeartbeatMonitor {

    private static final long HEARTBEAT_TIMEOUT_MS = 30000;

    private final IPeerNodeStatusService peerNodeStatusService;
    private final SessionManager sessionManager;

    @Scheduled(fixedRate = 5000)
    public void checkClientHeartbeat() {
        List<PeerNodeStatus> clientList = peerNodeStatusService.listByPeerType(PeerNodeTypeEnum.SERVER);
        LocalDateTime now = LocalDateTime.now();

        for (PeerNodeStatus status : clientList) {
            if (!isAuthenticated(status.getStatus())) {
                continue;
            }

            LocalDateTime lastSeen = status.getLastSeen();
            if (lastSeen == null) {
                continue;
            }

            if (isHeartbeatTimeout(lastSeen, now)) {
                handleClientTimeout(status);
            }
        }
    }

    @Scheduled(fixedRate = 5000)
    public void checkServerHeartbeat() {
        List<PeerNodeStatus> servers = peerNodeStatusService.listByPeerType(PeerNodeTypeEnum.CLIENT);
        LocalDateTime now = LocalDateTime.now();

        for (PeerNodeStatus status : servers) {
            if (!isAuthenticated(status.getStatus())) {
                continue;
            }

            LocalDateTime lastSeen = status.getLastSeen();
            if (lastSeen == null) {
                continue;
            }

            if (isHeartbeatTimeout(lastSeen, now)) {
                handleServerTimeout(status);
            }
        }
    }

    private void handleClientTimeout(PeerNodeStatus clientStatus) {
        log.warn("Client heartbeat timeout: peerId={}, lastSeen={}", clientStatus.getPeerId(), clientStatus.getLastSeen());
        peerNodeStatusService.updateStatus(clientStatus.getPeerId(), NodeStatusEnum.RECONNECTING.getCode());
        sessionManager.unregister(clientStatus.getPeerId());
    }

    private void handleServerTimeout(PeerNodeStatus serverStatus) {
        log.warn("Server heartbeat timeout: peerId={}, lastSeen={}", serverStatus.getPeerId(), serverStatus.getLastSeen());
        peerNodeStatusService.updateStatus(serverStatus.getPeerId(), NodeStatusEnum.RECONNECTING.getCode());
    }

    private boolean isHeartbeatTimeout(LocalDateTime lastSeen, LocalDateTime now) {
        return Duration.between(lastSeen, now).toMillis() > HEARTBEAT_TIMEOUT_MS;
    }

    private boolean isAuthenticated(int statusCode) {
        return NodeStatusEnum.AUTHENTICATED.getCode() == statusCode;
    }
}
