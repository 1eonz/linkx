package com.tdtech.linkx.node.service;

import java.util.Map;

/**
 * 协同岗分享消息处理器
 * 接收方节点收到 WS share/unshare 消息后调用（接收方仅写本地表，不再调警信）
 */
public interface ICoopShareMessageHandler {

    /**
     * 处理 share 消息：写入 tb_user_coop_recieved
     *
     * @param peerId 发送方节点ID
     * @param payload 消息内容
     */
    void handleShare(String peerId, Map<String, Object> payload);

    /**
     * 处理 unshare 消息：逻辑删除 tb_user_coop_recieved
     *
     * @param peerId 发送方节点ID
     * @param payload 消息内容
     */
    void handleUnshare(String peerId, Map<String, Object> payload);
}
