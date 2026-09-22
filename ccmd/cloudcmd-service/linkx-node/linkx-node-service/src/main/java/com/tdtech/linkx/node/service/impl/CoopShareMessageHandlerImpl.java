package com.tdtech.linkx.node.service.impl;

import com.tdtech.cloudcmd.im.jingxin.api.CoopShareRpcApi;
import com.tdtech.linkx.node.service.ICoopShareMessageHandler;
import com.tdtech.linkx.node.ws.client.P2pWebSocketClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 协同岗分享消息处理器实现
 * linkx-node 收到 WS share/unshare 消息后，通过 Dubbo 调用 cloudcmd-im-jingxin 写本地表
 */
@Slf4j
@Service
public class CoopShareMessageHandlerImpl implements ICoopShareMessageHandler {

    private final P2pWebSocketClient wsClient;

    @DubboReference
    private CoopShareRpcApi coopShareRpcApi;

    public CoopShareMessageHandlerImpl(@Lazy P2pWebSocketClient wsClient) {
        this.wsClient = wsClient;
    }

    @Override
    public void handleShare(String peerId, Map<String, Object> payload) {
        try {
            Long coopUserId = toLong(payload.get("coopUserId"));
            String originPeerId = (String) payload.get("originPeerId");
            String fromPeerName = (String) payload.get("fromPeerName");
            String targetPeerId = (String) payload.get("targetPeerId");
            Long targetOrgId = toLong(payload.get("targetOrgId"));
            // 详细信息（协同岗名称、图标、所属组织）
            String coopUserName = (String) payload.get("coopUserName");
            String iconUrl = (String) payload.get("iconUrl");
            Long orgId = toLong(payload.get("orgId"));
            String orgName = (String) payload.get("orgName");

            if (coopUserId == null || targetOrgId == null) {
                log.warn("handleShare: invalid payload, peerId={}, payload={}", peerId, payload);
                return;
            }

            String localPeerId = wsClient.getLocalPeerId();
            if (targetPeerId != null && !targetPeerId.equals(localPeerId)) {
                // 间接通知：消息目标不是本节点，通知原始归属方记录间接分享链路
                coopShareRpcApi.recordIndirectShare(coopUserId, peerId, targetPeerId, targetOrgId);
            } else {
                // 直接分享：消息目标就是本节点
                String recievedOriginPeerId = (originPeerId != null) ? originPeerId : peerId;
                boolean isIndirect = originPeerId != null && !originPeerId.equals(peerId);
                String recievedOriginPeerName = isIndirect ? fromPeerName : null;
                coopShareRpcApi.receiveShare(coopUserId, recievedOriginPeerId, recievedOriginPeerName, targetOrgId,
                        coopUserName, iconUrl, orgId, orgName);
            }
        } catch (Exception e) {
            log.error("handleShare: failed, peerId={}, payload={}", peerId, payload, e);
        }
    }

    @Override
    public void handleUnshare(String peerId, Map<String, Object> payload) {
        try {
            Long coopUserId = toLong(payload.get("coopUserId"));
            String originPeerId = (String) payload.get("originPeerId");
            Long targetOrgId = toLong(payload.get("targetOrgId"));

            if (coopUserId == null) {
                log.warn("handleUnshare: invalid payload, peerId={}, payload={}", peerId, payload);
                return;
            }

            // 直接取消分享时 originPeerId 为 null，按 share 时的写入逻辑兜底为 peerId（分享方）
            String recievedOriginPeerId = (originPeerId != null) ? originPeerId : peerId;
            coopShareRpcApi.receiveUnshare(coopUserId, recievedOriginPeerId, targetOrgId);
        } catch (Exception e) {
            log.error("handleUnshare: failed, peerId={}, payload={}", peerId, payload, e);
        }
    }

    private Long toLong(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        return Long.parseLong(obj.toString());
    }
}
