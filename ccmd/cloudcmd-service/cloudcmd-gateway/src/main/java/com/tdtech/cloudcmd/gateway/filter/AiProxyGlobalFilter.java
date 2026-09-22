package com.tdtech.cloudcmd.gateway.filter;

import java.net.URI;
import java.util.Locale;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;

import com.tdtech.cloudcmd.base.api.service.GlobalsRpcService;

import reactor.core.publisher.Mono;

@Component
public class AiProxyGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AiProxyGlobalFilter.class);

    private static final String GROUP_AI_HOST = "GROUP_AI_HOST";
    private static final String AI_PATH_PREFIX = "/XA-ics-agent";
    private static final String DESKTOP_AI_PATH_PREFIX = "/linkx/desktop/XA-ics-agent";
    private static final String H5_AI_PATH_PREFIX = "/linkx/h5portal/XA-ics-agent";
    private static final String ADMIN_AI_PATH_PREFIX = "/linkx/admin/XA-ics-agent";

    @DubboReference
    private GlobalsRpcService globalsRpcService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestPath = exchange.getRequest().getURI().getRawPath();
        if (!isAiProxyPath(requestPath)) {
            return chain.filter(exchange);
        }

        URI targetBaseUri = getTargetBaseUri(exchange);
        if (Objects.isNull(targetBaseUri)) {
            return Mono.empty();
        }

        String normalizedPath = normalizeAiPath(requestPath);
        URI targetUri = buildTargetUri(targetBaseUri, normalizedPath, exchange);
        if (isWebSocket(exchange)) {
            targetUri = switchToWebSocketScheme(targetUri);
        }

        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, targetUri);
        long startTime = System.currentTimeMillis();
        log.info("ai proxy route path:{} target:{} ws:{}", requestPath, maskTarget(targetUri), isWebSocket(exchange));
        URI finalTargetUri = targetUri;
        return chain.filter(exchange).doFinally(signalType -> log.info("ai proxy response path:{} target:{} status:{} cost:{}ms",
            requestPath, maskTarget(finalTargetUri), exchange.getResponse().getStatusCode(),
            System.currentTimeMillis() - startTime));
    }

    @Override
    public int getOrder() {
        // 设置过滤器执行顺序，值越大优先级越低
        return 10001;
    }

    /**
     * 获取目标基础URI：从全局配置中读取GROUP_AI_HOST并验证有效性
     * @param exchange 服务器Web交换对象
     * @return 目标基础URI，如果配置无效则返回null
     */
    private URI getTargetBaseUri(ServerWebExchange exchange) {
        // 从全局配置服务获取AI主机地址
        String groupAiHost = globalsRpcService.getGlobalsValueByName(GROUP_AI_HOST);
        if (StringUtils.isBlank(groupAiHost)) {
            log.warn("GROUP_AI_HOST is blank, ai proxy rejected");
            exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
            return null;
        }
        try {
            URI uri = URI.create(groupAiHost.trim());
            String scheme = StringUtils.lowerCase(uri.getScheme());
            // 验证协议是否为HTTP或HTTPS
            if (!Objects.equals("http", scheme) && !Objects.equals("https", scheme)) {
                log.warn("GROUP_AI_HOST scheme is not supported:{}", uri.getScheme());
                exchange.getResponse().setStatusCode(HttpStatus.BAD_GATEWAY);
                return null;
            }
            // 验证主机名不为空
            if (StringUtils.isBlank(uri.getHost())) {
                log.warn("GROUP_AI_HOST host is blank");
                exchange.getResponse().setStatusCode(HttpStatus.BAD_GATEWAY);
                return null;
            }
            return uri;
        } catch (IllegalArgumentException ex) {
            log.warn("GROUP_AI_HOST is invalid", ex);
            exchange.getResponse().setStatusCode(HttpStatus.BAD_GATEWAY);
            return null;
        }
    }

    /**
     * 检查是否为AI代理路径：判断请求路径是否匹配AI代理前缀
     * @param path 请求路径
     * @return true-是AI代理路径，false-不是AI代理路径
     */
    private boolean isAiProxyPath(String path) {
        return pathStartsWith(path, AI_PATH_PREFIX) || pathStartsWith(path, DESKTOP_AI_PATH_PREFIX)
            || pathStartsWith(path, H5_AI_PATH_PREFIX) || pathStartsWith(path, ADMIN_AI_PATH_PREFIX);
    }

    /**
     * 检查路径是否以前缀开头：支持精确匹配或前缀+"/"匹配
     * @param path 请求路径
     * @param prefix 路径前缀
     * @return true-路径匹配前缀，false-路径不匹配前缀
     */
    private boolean pathStartsWith(String path, String prefix) {
        return Objects.equals(path, prefix) || StringUtils.startsWith(path, prefix + "/");
    }

    /**
     * 标准化AI代理路径：移除路径前缀，保留实际请求路径
     * @param path 原始请求路径
     * @return 标准化后的路径（移除代理前缀）
     */
    private String normalizeAiPath(String path) {
        // 检查并移除桌面端AI路径前缀
        if (pathStartsWith(path, DESKTOP_AI_PATH_PREFIX)) {
            return normalizeWithoutProxyPrefix(StringUtils.removeStart(path, DESKTOP_AI_PATH_PREFIX));
        }
        // 检查并移除H5端AI路径前缀
        if (pathStartsWith(path, H5_AI_PATH_PREFIX)) {
            return normalizeWithoutProxyPrefix(StringUtils.removeStart(path, H5_AI_PATH_PREFIX));
        }
        // 检查并移除管理端AI路径前缀
        if (pathStartsWith(path, ADMIN_AI_PATH_PREFIX)) {
            return normalizeWithoutProxyPrefix(StringUtils.removeStart(path, ADMIN_AI_PATH_PREFIX));
        }
        // 检查并移除标准AI路径前缀
        if (pathStartsWith(path, AI_PATH_PREFIX)) {
            return normalizeWithoutProxyPrefix(StringUtils.removeStart(path, AI_PATH_PREFIX));
        }
        return path;
    }

    /**
     * 标准化无代理前缀的路径：确保路径不为空
     * @param path 移除前缀后的路径
     * @return 标准化路径，如果为空则返回"/"
     */
    private String normalizeWithoutProxyPrefix(String path) {
        return StringUtils.defaultIfBlank(path, "/");
    }

    /**
     * 构建目标URI：将基础URI和标准化路径拼接，保留原始查询参数
     * @param targetBaseUri 目标基础URI
     * @param normalizedPath 标准化后的请求路径
     * @param exchange 服务器Web交换对象
     * @return 完整的目标URI
     */
    private URI buildTargetUri(URI targetBaseUri, String normalizedPath, ServerWebExchange exchange) {
        String targetPath = joinPath(targetBaseUri.getRawPath(), normalizedPath);
        return UriComponentsBuilder.fromUri(targetBaseUri)
            .replacePath(targetPath)
            .replaceQuery(exchange.getRequest().getURI().getRawQuery())
            .build(true)
            .toUri();
    }

    /**
     * 拼接路径：处理基础路径和请求路径的连接，确保格式正确
     * @param basePath 基础路径
     * @param requestPath 请求路径
     * @return 拼接后的完整路径
     */
    private String joinPath(String basePath, String requestPath) {
        String normalizedBasePath = StringUtils.defaultIfBlank(basePath, "");
        // 如果基础路径是"/"，则视为空路径
        if (StringUtils.equals(normalizedBasePath, "/")) {
            normalizedBasePath = "";
        }
        // 确保请求路径以"/"开头
        if (!StringUtils.startsWith(requestPath, "/")) {
            requestPath = "/" + requestPath;
        }
        // 移除基础路径末尾的"/"（如果存在）
        if (StringUtils.endsWith(normalizedBasePath, "/")) {
            normalizedBasePath = StringUtils.removeEnd(normalizedBasePath, "/");
        }
        return normalizedBasePath + requestPath;
    }

    /**
     * 检查是否为WebSocket请求：通过HTTP头部的Upgrade字段判断
     * @param exchange 服务器Web交换对象
     * @return true-是WebSocket请求，false-不是WebSocket请求
     */
    private boolean isWebSocket(ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        String upgrade = headers.getUpgrade();
        if (StringUtils.equalsIgnoreCase(upgrade, "websocket")) {
            return true;
        }
        return headers.getConnection().stream()
            .anyMatch(value -> StringUtils.equalsIgnoreCase(value, HttpHeaders.UPGRADE));
    }

    /**
     * 切换WebSocket协议：将HTTP/HTTPS协议转换为WS/WSS协议
     * @param targetUri 原始目标URI
     * @return 转换为WebSocket协议的目标URI
     */
    private URI switchToWebSocketScheme(URI targetUri) {
        String scheme = StringUtils.lowerCase(targetUri.getScheme(), Locale.ROOT);
        String wsScheme = Objects.equals("https", scheme) ? "wss" : "ws";
        return UriComponentsBuilder.fromUri(targetUri).scheme(wsScheme).build(true).toUri();
    }

    /**
     * 掩码目标地址：用于日志记录，隐藏敏感信息
     * @param uri 目标URI
     * @return 格式化的目标地址字符串
     */
    private String maskTarget(URI uri) {
        return uri.getScheme() + "://" + uri.getAuthority() + uri.getRawPath();
    }
}
