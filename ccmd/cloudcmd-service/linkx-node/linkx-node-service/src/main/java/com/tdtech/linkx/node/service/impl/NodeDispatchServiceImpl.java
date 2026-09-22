package com.tdtech.linkx.node.service.impl;

import com.tdtech.linkx.node.config.NodeProperties;
import com.tdtech.linkx.node.dto.OpenDataGrantDTO;
import com.tdtech.linkx.node.dto.ProxyResponse;
import com.tdtech.linkx.node.entity.PeerNodeClient;
import com.tdtech.linkx.node.entity.PeerNodeServer;
import com.tdtech.linkx.node.service.INodeDispatchService;
import com.tdtech.linkx.node.service.IPeerNodeClientService;
import com.tdtech.linkx.node.service.IPeerNodeGrantPermissionService;
import com.tdtech.linkx.node.service.IPeerNodeGrantService;
import com.tdtech.linkx.node.service.IPeerNodeServerService;
import com.tdtech.linkx.node.util.P2pHttpUtil;
import com.tdtech.linkx.node.util.P2pJwtUtil;
import com.tdtech.linkx.node.util.PeerIdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 节点间数据透传服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NodeDispatchServiceImpl implements INodeDispatchService {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final NodeProperties nodeProperties;
    private final IPeerNodeServerService peerNodeServerService;
    private final IPeerNodeClientService peerNodeClientService;
    private final IPeerNodeGrantService peerNodeGrantService;
    private final IPeerNodeGrantPermissionService peerNodeGrantPermissionService;
    private final P2pJwtUtil p2pJwtUtil;
    private final RestTemplate proxyRestTemplate;

    @Override
    public ResponseEntity<byte[]> dispatch(String peerId, String originUri, String queryString) {
        // 双向查找对端节点：先查 Server 表（本端作为客户端），再查 Client 表（本端作为服务端）
        // 支持两种数据流向：客户端→服务端拿数据，服务端→客户端拿数据
        PeerNodeServer server = peerNodeServerService.getByPeerId(peerId);
        String targetIp;
        if (server != null) {
            if (!isServerAccessible(server)) {
                log.warn("dispatch: peerId={} not accessible (server), deleted={}, authorized={}, expiredIn={}",
                        peerId, server.getDeleted(), server.getAuthorized(), server.getExpiredIn());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("节点未授权或已过期".getBytes());
            }
            targetIp = server.getIp();
        } else {
            // Server 表找不到，查 Client 表（对端是本端的客户端）
            PeerNodeClient client = peerNodeClientService.getByPeerId(peerId);
            if (client == null) {
                log.warn("dispatch: peerId={} not found in server/client table", peerId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("节点不存在".getBytes());
            }
            if (!isClientAccessible(client)) {
                log.warn("dispatch: peerId={} not accessible (client), deleted={}, grant={}, expiredIn={}",
                        peerId, client.getDeleted(), client.getGrant(), client.getExpiredIn());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("节点未授权或已过期".getBytes());
            }
            targetIp = client.getIp();
        }

        if (!StringUtils.hasText(targetIp)) {
            log.warn("dispatch: peerId={} has no ip", peerId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("节点IP为空".getBytes());
        }

        String localPeerId = getLocalPeerId();
        String token = p2pJwtUtil.generateDispatchToken(localPeerId);

        // 构建对端 proxy URL
        String proxyUrl = nodeProperties.getLocal().buildProxyUrl(targetIp, localPeerId, originUri);
        if (StringUtils.hasText(queryString)) {
            proxyUrl = proxyUrl + "?" + queryString;
        }

        // 调对端 proxy
        Map<String, String> headers = new HashMap<>();
        headers.put(AUTH_HEADER, BEARER_PREFIX + token);
        ProxyResponse response = P2pHttpUtil.getForProxy(proxyUrl, headers, "dispatch");

        if (response == null) {
            log.warn("dispatch: proxy call failed, peerId={}, url={}", peerId, proxyUrl);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("对端不可达".getBytes());
        }

        // 透传响应
        HttpHeaders responseHeaders = buildResponseHeaders(response);
        return ResponseEntity.status(response.getStatusCode()).headers(responseHeaders).body(response.getBody());
    }

    @Override
    public ResponseEntity<byte[]> proxy(String callerPeerId, String originUri, String queryString,
                                        HttpServletRequest request) {
        // P2P JWT 校验
        String authHeader = request.getHeader(AUTH_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("proxy: missing or invalid Authorization header, callerPeerId={}", callerPeerId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("缺少Authorization头".getBytes());
        }
        String token = authHeader.substring(BEARER_PREFIX.length());
        if (!p2pJwtUtil.validateToken(token)) {
            log.warn("proxy: token invalid, callerPeerId={}", callerPeerId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token无效或已过期".getBytes());
        }
        String tokenPeerId = p2pJwtUtil.getPeerIdFromToken(token);
        if (!callerPeerId.equals(tokenPeerId)) {
            log.warn("proxy: peerId mismatch, path={}, token={}", callerPeerId, tokenPeerId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("peerId不匹配".getBytes());
        }

        // 匹配 originURI 对应的权限项
        String permission = peerNodeGrantPermissionService.matchPermission(originUri);
        if (permission == null) {
            log.warn("proxy: originUri={} not in permission table", originUri);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("URI不在允许访问的权限项中".getBytes());
        }

        // 校验调用方 peerId 在 tb_peer_node_grant 中是否有对应权限项的授权
        // grant 表记录方向取决于谁发起授权（grantServer/grantClient 保存 from=本端,to=对端），
        // 同步到对端后方向不变，因此两端库里的记录方向可能不一致。
        // 双向查找：from=caller,to=local 或 from=local,to=caller，任一方向有权限即放行。
        String localPeerId = getLocalPeerId();
        OpenDataGrantDTO grantDTO = peerNodeGrantService.getGrantDTO(callerPeerId, localPeerId);
        if (!hasPermission(grantDTO, permission)) {
            OpenDataGrantDTO reverseDTO = peerNodeGrantService.getGrantDTO(localPeerId, callerPeerId);
            if (!hasPermission(reverseDTO, permission)) {
                log.warn("proxy: callerPeerId={} has no permission={}, originUri={}, localPeerId={}, " +
                                "grantDTO={}, reverseDTO={}",
                        callerPeerId, permission, originUri, localPeerId, grantDTO, reverseDTO);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("未授权访问该数据".getBytes());
            }
        }

        // 通过 K8s service name 直连微服务
        String targetService = resolveTargetService(originUri);
        if (targetService == null) {
            log.warn("proxy: no route for originUri={}", originUri);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("未找到匹配的微服务路由".getBytes());
        }

        String internalUrl = "http://" + targetService + ":8080" + originUri;
        if (StringUtils.hasText(queryString)) {
            internalUrl = internalUrl + "?" + queryString;
        }

        // 调内部微服务（不带用户 token）
        try {
            ResponseEntity<byte[]> internalResponse = proxyRestTemplate.exchange(
                    internalUrl, HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), byte[].class);
            log.info("proxy: callerPeerId={}, originUri={}, statusCode={}",
                    callerPeerId, originUri, internalResponse.getStatusCode());
            return internalResponse;
        } catch (Exception e) {
            log.error("proxy: internal call failed, url={}", internalUrl, e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("内部微服务调用失败".getBytes());
        }
    }

    /**
     * 校验 server 是否可访问（授权期内可查）。
     * 有效 = (deleted=0) OR (deleted=1 AND authorized=1 AND expired_in IS NOT NULL AND expired_in > now())
     */
    private boolean isServerAccessible(PeerNodeServer server) {
        if (server.getDeleted() == null || server.getDeleted() == 0) {
            return true;
        }
        // 逻辑删除后，授权期内可查
        if (server.getAuthorized() == null || server.getAuthorized() != 1) {
            return false;
        }
        if (server.getExpiredIn() == null) {
            return false;
        }
        return server.getExpiredIn().isAfter(LocalDateTime.now());
    }

    /**
     * 校验 client 是否可访问（授权期内可查）。
     * 有效 = (deleted=0) OR (deleted=1 AND grant=1 AND expired_in IS NOT NULL AND expired_in > now())
     */
    private boolean isClientAccessible(PeerNodeClient client) {
        if (client.getDeleted() == null || client.getDeleted() == 0) {
            return true;
        }
        // 逻辑删除后，授权期内可查
        if (client.getGrant() == null || client.getGrant() != 1) {
            return false;
        }
        if (client.getExpiredIn() == null) {
            return false;
        }
        return client.getExpiredIn().isAfter(LocalDateTime.now());
    }

    /**
     * 校验调用方是否有对应权限项的授权。
     */
    private boolean hasPermission(OpenDataGrantDTO dto, String permission) {
        if (dto == null) {
            return false;
        }
        Integer value;
        switch (permission) {
            case "org":
                value = dto.getOrg();
                break;
            case "dashboard":
                value = dto.getDashboard();
                break;
            case "coopuser":
                value = dto.getCoopUser();
                break;
            case "h5":
                value = dto.getH5();
                break;
            default:
                return false;
        }
        return value != null && value == 1;
    }

    /**
     * 根据 originURI 前缀匹配目标微服务 K8s service name。
     */
    private String resolveTargetService(String originUri) {
        if (nodeProperties.getProxy() == null || nodeProperties.getProxy().getRoutes() == null) {
            return null;
        }
        for (NodeProperties.ProxyConfig.Route route : nodeProperties.getProxy().getRoutes()) {
            if (originUri.startsWith(route.getPrefix())) {
                return route.getService();
            }
        }
        return null;
    }

    private String getLocalPeerId() {
        NodeProperties.LocalNode local = nodeProperties.getLocal();
        return PeerIdUtil.generatePeerId(local.getEffectiveIp(), local.getPort());
    }

    private HttpHeaders buildResponseHeaders(ProxyResponse response) {
        HttpHeaders headers = new HttpHeaders();
        if (response.getHeaders() != null) {
            response.getHeaders().forEach((key, values) -> {
                if (values != null && !values.isEmpty()) {
                    // 跳过 Transfer-Encoding、Content-Length 等，由 ResponseEntity 自动处理
                    if ("transfer-encoding".equalsIgnoreCase(key)
                            || "content-length".equalsIgnoreCase(key)) {
                        return;
                    }
                    headers.put(key, values);
                }
            });
        }
        return headers;
    }
}
