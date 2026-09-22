package com.tdtech.linkx.node.service;

import java.util.Map;

/**
 * WebSocket 开放数据授权消息处理器
 * 处理对端节点通过 WSS 发送的 auth/unauth 消息
 */
public interface IWsAuthGrantMessageHandler {

    /**
     * 处理授权通知消息（subtype=auth）
     *
     * @param peerId  发送方节点ID（WSS通道上的对端peerId）
     * @param payload 消息体
     */
    void handleAuthGrant(String peerId, Map<String, Object> payload);

    /**
     * 处理取消授权通知消息（subtype=unauth）
     *
     * @param peerId  发送方节点ID
     * @param payload 消息体
     */
    void handleAuthRevoke(String peerId, Map<String, Object> payload);
}
