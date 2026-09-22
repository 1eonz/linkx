package com.tdtech.cloudcmd.gateway.filter;

import javax.annotation.Resource;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.tdtech.cloudcmd.gateway.filter.strategy.CloudcmdAuthenticateStrategy;
import com.tdtech.cloudcmd.gateway.filter.strategy.NoAuthenticateStrategy;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * @author zWX523748
 */
@Component
@Slf4j
public class CloudcmdGlobalFilter implements GlobalFilter, Ordered {

    private static final String AI_PATH_PREFIX = "/XA-ics-agent";
    private static final String DESKTOP_AI_PATH_PREFIX = "/linkx/desktop/XA-ics-agent";
    private static final String H5_AI_PATH_PREFIX = "/linkx/h5portal/XA-ics-agent";
    private static final String ADMIN_AI_PATH_PREFIX = "/linkx/admin/XA-ics-agent";

    @Resource
    private CloudcmdAuthenticateStrategy cloudcmdAuthenticateStrategy;
    @Resource
    private NoAuthenticateStrategy noAuthenticateStrategy;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var request = exchange.getRequest();
        if (isAiProxyPath(request.getURI().getPath())) {
            return noAuthenticateStrategy.exchange(exchange, chain);
        }
        var appKey = request.getHeaders().getFirst("x-cloudcmd-appkey");
        if (appKey == null || appKey.isBlank()) {
            return noAuthenticateStrategy.exchange(exchange, chain);
        }
        switch (appKey){
            case "CDC-2000":{
                return cloudcmdAuthenticateStrategy.exchange(exchange, chain);
            }
            case "CDC-1000":
            case "CAPP-2000":
            case "CAPP-1000":
            default:{
                return noAuthenticateStrategy.exchange(exchange, chain);
            }
        }
    }

    @Override
    public int getOrder() {
        return -1;
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
        return prefix.equals(path) || path.startsWith(prefix + "/");
    }
}
