package com.tdtech.linkx.node.controller;

import com.tdtech.linkx.node.service.INodeDispatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 节点间数据透传接口。
 * - dispatch：内部微服务调用，转发请求到对端 proxy（不走 APISIX，内网直连）
 * - proxy：对端节点调用，代理请求到本端内部微服务（走 APISIX，P2P JWT 校验）
 */
@Slf4j
@RestController
@RequestMapping("/node/v1/p2p")
@RequiredArgsConstructor
@Tag(name = "节点数据透传", description = "P2P节点间 HTTP 数据透传接口")
public class NodeDispatchController {

    private final INodeDispatchService nodeDispatchService;

    @Operation(summary = "节点数据转发", description = "内部微服务调用，转发请求到对端 proxy")
    @GetMapping("/{peerId}/dispatch/**")
    public ResponseEntity<byte[]> dispatch(
            @PathVariable String peerId,
            HttpServletRequest request) {
        String originURI = extractOriginUri(request);
        String queryString = request.getQueryString();
        log.info("dispatch: peerId={}, originURI={}, queryString={}", peerId, originURI, queryString);
        return nodeDispatchService.dispatch(peerId, originURI, queryString);
    }

    @Operation(summary = "节点数据代理", description = "对端节点调用，代理请求到本端内部微服务")
    @GetMapping("/{peerId}/proxy/**")
    public ResponseEntity<byte[]> proxy(
            @PathVariable String peerId,
            HttpServletRequest request) {
        String originURI = extractOriginUri(request);
        String queryString = request.getQueryString();
        log.info("proxy: callerPeerId={}, originURI={}, queryString={}", peerId, originURI, queryString);
        return nodeDispatchService.proxy(peerId, originURI, queryString, request);
    }

    /**
     * 从 request URI 中提取 originURI。
     * URL 格式：/node/v1/p2p/{peerId}/{action}/originURI
     * action 是 dispatch 或 proxy，后面部分都是 originURI（可能含 /）。
     * 用 lastIndexOf 定位最后一个 action，避免 originURI 本身含 dispatch/proxy 字符串时误截。
     * 返回值统一以 / 开头，例如 /collaboration/v1/post/queryDepartment。
     */
    private String extractOriginUri(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        // 找最后一个 /dispatch/ 或 /proxy/ 的位置
        int dispatchIdx = requestUri.lastIndexOf("/dispatch/");
        int proxyIdx = requestUri.lastIndexOf("/proxy/");
        int actionIdx = Math.max(dispatchIdx, proxyIdx);
        if (actionIdx < 0) {
            return requestUri;
        }
        // actionIdx 指向 /dispatch/ 或 /proxy/ 的起始 /
        // originURI 从 action 名后的 / 开始
        int originStart = requestUri.indexOf('/', actionIdx + 1);
        if (originStart < 0) {
            return "/";
        }
        String originUri = requestUri.substring(originStart);
        // 兼容旧客户端可能传入的 %2F 编码（现在 NodeDispatchClient 已不再编码）
        try {
            originUri = java.net.URLDecoder.decode(originUri, "UTF-8");
        } catch (Exception e) {
            // 保持原样
        }
        if (!originUri.startsWith("/")) {
            originUri = "/" + originUri;
        }
        return originUri;
    }
}
