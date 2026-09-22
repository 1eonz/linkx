package com.tdtech.cloudcmd.gateway.filter.strategy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class NoAuthenticateStrategy extends AlterRequestStrategy{

    @Getter
    @Value("${cloudcmd.network.internal.ip:0}")
    private String internalIp;
    public Mono<Void> exchange(ServerWebExchange exchange, GatewayFilterChain chain){
        return chain.filter(exchange.mutate().request(alterRequest(exchange)).build());
    }
}
