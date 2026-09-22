package com.tdtech.linkx.node.task;

import com.tdtech.cloudcmd.util.json.JsonUtil;
import com.tdtech.linkx.node.config.NodeProperties;
import com.tdtech.linkx.node.entity.PeerNodeServer;
import com.tdtech.linkx.node.entity.PeerNodeStatus;
import com.tdtech.linkx.node.enums.NodeStatusEnum;
import com.tdtech.linkx.node.service.IPeerNodeServerService;
import com.tdtech.linkx.node.service.IPeerNodeStatusService;
import com.tdtech.linkx.node.util.PeerIdUtil;
import com.tdtech.linkx.node.ws.WsMessage;
import com.tdtech.linkx.node.ws.client.P2pWebSocketClient;
import com.tdtech.linkx.node.ws.server.SessionManager;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class P2pNodeLifecycleManager implements ApplicationListener<ApplicationReadyEvent> {

    private final NodeProperties nodeProperties;
    private final IPeerNodeServerService serverService;
    private final P2pWebSocketClient wsClient;
    private final SessionManager sessionManager;
    private final IPeerNodeStatusService peerNodeStatusService;

    private volatile boolean initialized = false;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (initialized) {
            return;
        }
        initialized = true;
        log.info("=== P2P Node Lifecycle Manager Starting ===");

        wsClient.disconnectAll();
        String localIp = nodeProperties.getLocal().getIp();
        String vip = nodeProperties.getLocal().getVip();
        if (vip != null && !vip.isEmpty() && (!vip.equals(localIp))) {
            localIp = vip;
            log.info("HA environment detected, using VIP as local IP: vip={}", vip);
        }
        int localPort = nodeProperties.getLocal().getPort();
        if (localIp == null || localIp.isEmpty()) {
            localIp = "127.0.0.1";
        }

        String localPeerId = PeerIdUtil.generatePeerId(localIp, localPort);
        wsClient.setLocalNode(localPeerId, localIp, localPort);

        log.info("Local node info: peerId={}, ip={}, port={}", localPeerId, localIp, localPort);
        connectToServers();

        log.info("=== P2P Node Lifecycle Manager Started ===");
    }

    private void connectToServers() {
        List<PeerNodeServer> servers = serverService.listActiveServers();
        String localPeerId = wsClient.getLocalPeerId();

        for (PeerNodeServer server : servers) {
            try {
                if (localPeerId != null && server.getPeerId().equals(localPeerId)) {
                    log.info("Skip connecting to self: peerId={}", server.getPeerId());
                    continue;
                }

                wsClient.connectToServer(server, sessionId -> {
                    log.info("Connected to server: peerId={}, sessionId={}", server.getPeerId(), sessionId);
                });
            } catch (Exception e) {
                log.error("Failed to connect to server: peerId={}", server.getPeerId(), e);
            }
        }
    }

    @PreDestroy
    public void onShutdown() {
        log.info("=== P2P Node Lifecycle Manager Stopping ===");
        broadcastDisconnect("服务正在关闭");
        wsClient.disconnectAll();
        log.info("=== P2P Node Lifecycle Manager Stopped ===");
    }

    private void broadcastDisconnect(String reason) {
        WsMessage<Object> disconnect = WsMessage.disconnect(reason);
        String json = JsonUtil.toJsonStr(disconnect);
        TextWebSocketFrame frame = new TextWebSocketFrame(json);

        for (Channel channel : sessionManager.getAllChannels()) {
            if (channel.isActive()) {
                channel.writeAndFlush(frame.copy());
            }
        }
        log.info("Broadcast disconnect message to {} clients", sessionManager.getAuthenticatedCount());
    }

    @Scheduled(fixedDelayString = "${p2p.client.reconnect-interval}")
    public void reconnectTask() {
        log.debug("start reconnect task....");
        List<PeerNodeServer> servers = serverService.listActiveServers();
        for (PeerNodeServer server : servers) {
            handleServerReconnect(server);
        }
    }

    private void handleServerReconnect(PeerNodeServer server) {
        String peerId = server.getPeerId();
        if (wsClient.isActive(peerId)) {
            log.debug("Skip reconnect for peerId={}, already connected", peerId);
            return;
        }

        PeerNodeStatus status = peerNodeStatusService.getByPeerId(peerId);

        if (status == null) {
            doFirstTimeConnect(peerId, server);
            return;
        }

        if (shouldSkipByRetryStrategy(status, peerId)) {
            return;
        }

        handleReconnectByStatus(status, peerId, server);
    }

    private void doFirstTimeConnect(String peerId, PeerNodeServer server) {
        log.info("First time connect: peerId={}", peerId);
        peerNodeStatusService.updateStatus(peerId, NodeStatusEnum.CONNECTING.getCode());
        doReconnect(server);
    }

    private boolean shouldSkipByRetryStrategy(PeerNodeStatus status, String peerId) {
        int retryStrategy = status.getRetryStrategy() != null ? status.getRetryStrategy() : 0;

        if (retryStrategy == 2) {
            log.debug("Skip reconnect for peerId={}, retryStrategy=2 (no retry)", peerId);
            return true;
        }

        return false;
    }

    private void handleReconnectByStatus(PeerNodeStatus status, String peerId, PeerNodeServer server) {
        int statusCode = status.getStatus();

        if (statusCode == NodeStatusEnum.DISCONNECTED.getCode()) {
            doFirstTimeConnect(peerId, server);
            return;
        }

        if (statusCode == NodeStatusEnum.DISCONNECTED.getCode() ||
                statusCode == NodeStatusEnum.RECONNECTING.getCode() ||
                statusCode == NodeStatusEnum.CONNECTING.getCode() ||
                statusCode == NodeStatusEnum.AUTH_FAILED.getCode()) {

            log.info("Attempting to reconnect: peerId={}, status={}, retryStrategy={}",
                    peerId, statusCode, status.getRetryStrategy());
            doReconnect(server);
        }
    }

    private void doReconnect(PeerNodeServer server) {
        try {
            wsClient.connectToServer(server, sessionId -> {
                log.info("Reconnected to server: peerId={}, sessionId={}", server.getPeerId(), sessionId);
            });
        } catch (Exception e) {
            log.error("Reconnect failed: peerId={}", server.getPeerId(), e);
        }
    }
}
