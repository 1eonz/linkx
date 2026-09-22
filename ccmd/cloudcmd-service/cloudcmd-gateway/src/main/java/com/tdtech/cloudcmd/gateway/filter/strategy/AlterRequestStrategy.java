package com.tdtech.cloudcmd.gateway.filter.strategy;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import com.tdtech.cloudcmd.gateway.constant.GatewayConstant;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AlterRequestStrategy {

    protected abstract String getInternalIp();

    protected ServerHttpRequest alterRequest(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        String hostAddress = request.getHeaders().getFirst("X-Real-IP") == null
                ? request.getRemoteAddress().getAddress().getHostAddress() : request.getHeaders().getFirst("X-Real-IP");
        log.debug("[request is :{}] url is :{},remote ip is {}", request.getId(), request.getURI(), hostAddress);
        if (hostAddress != null && hostAddress.equals(getInternalIp())) {
            log.debug("this is external request");
            return request.mutate().header(GatewayConstant.HEADER_NETWORK, "1")
                    .header(GatewayConstant.HEADER_REMOTE, hostAddress).build();
        } else {
            return request.mutate().header(GatewayConstant.HEADER_NETWORK, "0")
                    .header(GatewayConstant.HEADER_REMOTE, hostAddress).build();
        }
    }

}
