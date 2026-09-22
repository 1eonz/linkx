package com.tdtech.cloudcmd.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.support.GatewayToStringStyler;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PreflightGlobalFilter extends AbstractGatewayFilterFactory<Object> {

    @Override
    public GatewayFilter apply(Object config) {
        return new GatewayFilter() {

            @Override
            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
                log.debug("PreflightGlobalFilter filter start:{} {}",exchange.getRequest().getMethod(),exchange.getRequest().getURI());
                if (Objects.equals(exchange.getRequest().getMethod(), HttpMethod.OPTIONS)) {
                    log.debug("options request:{}", exchange.getRequest());
                    exchange.getResponse().setStatusCode(HttpStatus.OK);
                    return Mono.empty();
                } else {
                    return chain.filter(exchange);
                }
            }

            @Override
            public String toString() {
                return GatewayToStringStyler.filterToStringCreator(PreflightGlobalFilter.this).toString();
            }
        };
    }

}
