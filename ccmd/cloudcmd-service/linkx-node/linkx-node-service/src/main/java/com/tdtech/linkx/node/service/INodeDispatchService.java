package com.tdtech.linkx.node.service;

import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;

/**
 * 节点间数据透传服务。
 */
public interface INodeDispatchService {

    /**
     * dispatch：本端微服务调用，转发请求到对端 proxy。
     *
     * @param peerId       目标节点 peerId（对端）
     * @param originUri    原始业务 URI（如 /collaboration/post/queryDepartment）
     * @param queryString  原始 querystring（不含 ?）
     * @return 对端响应（透传）
     */
    ResponseEntity<byte[]> dispatch(String peerId, String originUri, String queryString);

    /**
     * proxy：对端节点调用，代理请求到本端内部微服务。
     *
     * @param callerPeerId 调用方 peerId（path 中携带）
     * @param originUri    原始业务 URI
     * @param queryString  原始 querystring（不含 ?）
     * @param request      原始请求（用于提取 Authorization header）
     * @return 内部微服务响应（透传）
     */
    ResponseEntity<byte[]> proxy(String callerPeerId, String originUri, String queryString,
                                  HttpServletRequest request);
}
