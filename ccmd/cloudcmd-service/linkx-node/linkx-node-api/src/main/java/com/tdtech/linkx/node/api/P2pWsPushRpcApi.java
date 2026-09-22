package com.tdtech.linkx.node.api;

/**
 * P2P WebSocket 消息推送 RPC 接口
 * 供 cloudcmd-im-jingxin 通过 Dubbo 调用，由 linkx-node 通过 WSS 通道推送到对端节点
 */
public interface P2pWsPushRpcApi {

    /**
     * 推送协同岗分享消息给指定节点
     *
     * @param peerId       目标节点ID（WS 发送目标）
     * @param coopUserId   协同岗ID
     * @param originPeerId 协同岗原始归属节点ID（本节点的协同岗传 null）
     * @param targetPeerId 实际接收方节点ID（区分分享给目标还是间接通知原始归属方）
     * @param targetOrgId  目标组织ID
     * @param coopUserName 协同岗名称
     * @param iconUrl      图标相对路径
     * @param orgId        协同岗所属组织ID
     * @param orgName      协同岗所属组织名称
     * @return true=推送成功（节点在线）；false=节点离线，消息已暂存 Redis
     */
    boolean pushShare(String peerId, Long coopUserId, String originPeerId, String targetPeerId, Long targetOrgId,
                      String coopUserName, String iconUrl, Long orgId, String orgName);

    /**
     * 推送取消协同岗分享消息给指定节点
     *
     * @param peerId       目标节点ID
     * @param coopUserId   协同岗ID
     * @param originPeerId 协同岗原始归属节点ID
     * @param targetOrgId  目标组织ID
     * @return true=推送成功；false=节点离线
     */
    boolean pushUnshare(String peerId, Long coopUserId, String originPeerId, Long targetOrgId);
}
