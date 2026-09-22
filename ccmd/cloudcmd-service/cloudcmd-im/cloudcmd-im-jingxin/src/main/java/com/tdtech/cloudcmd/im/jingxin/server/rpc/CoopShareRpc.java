package com.tdtech.cloudcmd.im.jingxin.server.rpc;

import com.tdtech.cloudcmd.im.jingxin.api.CoopShareRpcApi;
import com.tdtech.cloudcmd.im.jingxin.server.service.IUserCoopRecievedService;
import com.tdtech.cloudcmd.im.jingxin.server.service.IUserCoopSharedService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import javax.annotation.Resource;

/**
 * 协同岗分享 RPC 实现
 * 供 linkx-node 通过 Dubbo 调用
 */
@Slf4j
@DubboService
public class CoopShareRpc implements CoopShareRpcApi {

    @Resource
    private IUserCoopRecievedService coopRecievedService;
    @Resource
    private IUserCoopSharedService coopSharedService;

    @Override
    public void receiveShare(Long coopUserId, String originPeerId, String originPeerName, Long targetOrgId,
                             String coopUserName, String iconUrl, Long orgId, String orgName) {
        coopRecievedService.receiveCoopUser(coopUserId, originPeerId, originPeerName, targetOrgId,
                coopUserName, iconUrl, orgId, orgName);
        log.info("receiveShare: coopUserId={}, originPeerId={}, originPeerName={}", coopUserId, originPeerId, originPeerName);
    }

    @Override
    public void receiveUnshare(Long coopUserId, String originPeerId, Long targetOrgId) {
        coopRecievedService.cancelReceived(coopUserId, originPeerId);
        coopSharedService.cascadeUnshare(coopUserId);
        log.info("receiveUnshare: coopUserId={}, originPeerId={}", coopUserId, originPeerId);
    }

    @Override
    public void recordIndirectShare(Long coopUserId, String fromPeerId, String targetPeerId, Long targetOrgId) {
        coopSharedService.recordIndirectShare(coopUserId, fromPeerId, targetPeerId, targetOrgId);
        log.info("recordIndirectShare: coopUserId={}, fromPeerId={}, targetPeerId={}", coopUserId, fromPeerId, targetPeerId);
    }
}
