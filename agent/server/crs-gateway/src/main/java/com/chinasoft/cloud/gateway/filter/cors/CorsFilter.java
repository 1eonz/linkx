package com.chinasoft.cloud.gateway.filter.cors;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

@Component
public class CorsFilter implements WebFilter {

    private static final String ALL = "*";
    private static final String MAX_AGE = "3600L";
    private static final String ACCESS_CONTROL_ALLOW_PRIVATE_NETWORK = "Access-Control-Allow-Private-Network";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // 非跨域请求，直接放行
        ServerHttpRequest request = exchange.getRequest();
        if (!CorsUtils.isCorsRequest(request)) {
            return chain.filter(exchange);
        }

        // 设置跨域响应头
        ServerHttpResponse response = exchange.getResponse();
        addCorsHeaders(request, response);
        if (request.getMethod() == HttpMethod.OPTIONS) {
            response.setStatusCode(HttpStatus.OK);
            return Mono.empty();
        }
        return chain.filter(exchange);
    }

    private void addCorsHeaders(ServerHttpRequest request, ServerHttpResponse response) {
        HttpHeaders requestHeaders = request.getHeaders();
        HttpHeaders responseHeaders = response.getHeaders();
        String origin = requestHeaders.getOrigin();
        if (origin == null) {
            return;
        }

        // 携带凭证时不能返回 *，需要回写实际 Origin
        responseHeaders.set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
        responseHeaders.set(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, Boolean.TRUE.toString());
        responseHeaders.set(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, ALL);
        responseHeaders.set(HttpHeaders.ACCESS_CONTROL_MAX_AGE, MAX_AGE);
        String requestAllowHeaders = requestHeaders.getFirst(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
        responseHeaders.set(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                requestAllowHeaders == null || requestAllowHeaders.isBlank() ? ALL : requestAllowHeaders);
        responseHeaders.set(ACCESS_CONTROL_ALLOW_PRIVATE_NETWORK, Boolean.TRUE.toString());
    }

}
