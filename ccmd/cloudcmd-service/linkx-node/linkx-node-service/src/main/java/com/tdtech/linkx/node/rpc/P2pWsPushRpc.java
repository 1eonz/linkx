package com.tdtech.linkx.node.rpc;

import com.tdtech.linkx.node.api.P2pWsPushRpcApi;
import com.tdtech.linkx.node.service.IP2pWsPushService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import javax.annotation.Resource;

/**
 * P2P WebSocket 消息推送 RPC 实现
 * 供 cloudcmd-im-jingxin 通过 Dubbo 调用
 */
@Slf4j
@DubboService
public class P2pWsPushRpc implements P2pWsPushRpcApi {

    @Resource
    private IP2pWsPushService p2pWsPushService;

    @Override
    public boolean pushShare(String peerId, Long coopUserId, String originPeerId, String targetPeerId, Long targetOrgId,
                             String coopUserName, String iconUrl, Long orgId, String orgName) {
        return p2pWsPushService.pushShare(peerId, coopUserId, originPeerId, targetPeerId, targetOrgId,
                coopUserName, iconUrl, orgId, orgName);
    }

    @Override
    public boolean pushUnshare(String peerId, Long coopUserId, String originPeerId, Long targetOrgId) {
        return p2pWsPushService.pushUnshare(peerId, coopUserId, originPeerId, targetOrgId);
    }
}
