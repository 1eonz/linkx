package com.tdtech.linkx.node.api;

import com.tdtech.linkx.node.dto.DispatchResponseDTO;

import java.util.Map;

/**
 * P2P 数据流量 RPC 接口
 */
public interface P2PDataFlowRpcApi {
    /**
     * 获取节点信息列表, 供当前服务器客户端获取和本局点有关联的所有P2P节点列表
     *
     * @param grant 开放数据授权
     * @return 开放数据授权节点信息列表
     */
    Map<String, Object> listNodes(String grant);

    /**
     * dispatch：对端节点调用，代理请求到本端内部微服务。
     *
     * @param peerId      目标节点 peerId（对端）
     * @param originUri   原始业务 URI（如 /collaboration/post/queryDepartment）
     * @param queryString 原始 querystring（不含 ?）
     * @return 内部微服务响应（透传，DTO 承载状态码与响应体）
     */
    DispatchResponseDTO dispatch(String peerId, String originUri, String queryString);

    /**
     * proxy：本端微服务调用，转发请求到对端 proxy。
     *
     * @param callerPeerId        调用方 peerId（path 中携带）
     * @param originUri           原始业务 URI
     * @param queryString         原始 querystring（不含 ?）
     * @param authorizationHeader Authorization header
     * @return 对端响应（透传，DTO 承载状态码与响应体）
     */
//    DispatchResponseDTO proxy(String callerPeerId, String originUri, String queryString,
//                              String authorizationHeader);

    /**
     * 获取节点状态
     *
     * @param peerId 节点 peerId
     * @return 节点状态
     */
    Integer getPeerNodeStatus(String peerId);
}