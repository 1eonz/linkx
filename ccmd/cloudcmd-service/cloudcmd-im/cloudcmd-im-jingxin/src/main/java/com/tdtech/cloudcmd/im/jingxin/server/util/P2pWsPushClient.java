package com.tdtech.cloudcmd.im.jingxin.server.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 调用 linkx-node 的 WS 推送接口客户端
 * 通过 K8s service name 内网直连 linkx-node-service，不走 APISIX
 */
@Slf4j
@Component
public class P2pWsPushClient {

    /**
     * linkx-node 在 K8s 中的 service name
     */
    private static final String LINKX_NODE_SERVICE = "linkx-node-service";

    /**
     * linkx-node HTTP 端口
     */
    private static final int LINKX_NODE_PORT = 8080;

    private final RestTemplate restTemplate;

    public P2pWsPushClient(@Qualifier("nodeDispatchRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 推送协同岗分享消息
     */
    public boolean pushShare(String peerId, Long coopUserId, String originPeerId,
                             String originPeerName, Long targetOrgId, boolean isIndirect) {
        String url = String.format("http://%s:%d/node/v1/p2p/%s/push/share",
                LINKX_NODE_SERVICE, LINKX_NODE_PORT, peerId);

        Map<String, Object> body = new HashMap<>();
        body.put("coopUserId", coopUserId);
        body.put("originPeerId", originPeerId);
        body.put("originPeerName", originPeerName);
        body.put("targetOrgId", targetOrgId);
        body.put("isIndirect", isIndirect);

        return doPost(url, body, "pushShare");
    }

    /**
     * 推送取消协同岗分享消息
     */
    public boolean pushUnshare(String peerId, Long coopUserId, String originPeerId, Long targetOrgId) {
        String url = String.format("http://%s:%d/node/v1/p2p/%s/push/unshare",
                LINKX_NODE_SERVICE, LINKX_NODE_PORT, peerId);

        Map<String, Object> body = new HashMap<>();
        body.put("coopUserId", coopUserId);
        body.put("originPeerId", originPeerId);
        body.put("targetOrgId", targetOrgId);

        return doPost(url, body, "pushUnshare");
    }

    private boolean doPost(String url, Map<String, Object> body, String action) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            restTemplate.postForEntity(url, entity, String.class);
            log.info("{}: success, url={}", action, url);
            return true;
        } catch (Exception e) {
            log.error("{}: failed, url={}", action, url, e);
            return false;
        }
    }
}
