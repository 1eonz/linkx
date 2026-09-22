package com.tdtech.cloudcmd.im.jingxin.api;

/**
 * 协同岗分享 RPC 接口
 * 供 linkx-node 通过 Dubbo 调用，处理协同岗分享/取消分享/间接通知
 */
public interface CoopShareRpcApi {

    /**
     * 接收协同岗分享（写 tb_user_coop_recieved）
     *
     * @param coopUserId     协同岗ID
     * @param originPeerId   来源节点ID
     * @param originPeerName 来源节点名称（间接分享时为中间方名称，直接分享为 null）
     * @param targetOrgId    目标组织ID
     * @param coopUserName   协同岗名称
     * @param iconUrl        图标相对路径
     * @param orgId          协同岗所属组织ID
     * @param orgName        协同岗所属组织名称
     */
    void receiveShare(Long coopUserId, String originPeerId, String originPeerName, Long targetOrgId,
                      String coopUserName, String iconUrl, Long orgId, String orgName);

    /**
     * 取消接收协同岗分享（逻辑删除 + 级联取消下级）
     *
     * @param coopUserId   协同岗ID
     * @param originPeerId 来源节点ID
     * @param targetOrgId  目标组织ID
     */
    void receiveUnshare(Long coopUserId, String originPeerId, Long targetOrgId);

    /**
     * 记录间接分享链路（写 tb_user_coop_shared）
     *
     * @param coopUserId   协同岗ID
     * @param fromPeerId   中间方节点ID
     * @param targetPeerId 最终目标节点ID
     * @param targetOrgId  目标组织ID
     */
    void recordIndirectShare(Long coopUserId, String fromPeerId, String targetPeerId, Long targetOrgId);
}
