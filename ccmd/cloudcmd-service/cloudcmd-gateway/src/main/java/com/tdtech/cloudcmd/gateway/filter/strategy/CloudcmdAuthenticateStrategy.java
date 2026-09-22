package com.tdtech.cloudcmd.gateway.filter.strategy;

import static com.tdtech.cloudcmd.gateway.constant.GatewayConstant.ACCESS_TMP_TOKEN_USER_KEY;
import static com.tdtech.cloudcmd.gateway.constant.GatewayConstant.ACCESS_TOKEN_REQUEST_LATEST_TIME_KEY;
import static com.tdtech.cloudcmd.gateway.constant.GatewayConstant.ACCESS_TOKEN_REQUEST_LATEST_TIME_PERIOD;
import static com.tdtech.cloudcmd.gateway.constant.GatewayConstant.ACCESS_TOKEN_USER_KEY;
import static com.tdtech.cloudcmd.gateway.constant.GatewayConstant.HEADER_AUTHORIZATION;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.gateway.constant.GatewayConstant;
import com.tdtech.cloudcmd.gateway.enums.AuthEnums;
import com.tdtech.cloudcmd.gateway.exception.AuthException;
import com.tdtech.cloudcmd.gateway.props.AuthProperties;
import com.tdtech.cloudcmd.redis.RedisUtil;
import com.tdtech.cloudcmd.util.DateFormatUtil;
import com.tdtech.cloudcmd.util.StringUtils;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class CloudcmdAuthenticateStrategy extends AlterRequestStrategy{


    @Resource
    private AuthProperties authProperties;
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private RedisUtil redisUtil;

    @Resource(name = "blockTaskExecutorService")
    private ExecutorService executorService;

    @Getter
    @Value("${cloudcmd.network.internal.ip:0}")
    private String internalIp;

    public Mono<Void> exchange(ServerWebExchange exchange, GatewayFilterChain chain){
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        if (isSkip(path)) {
            return chain.filter(exchange.mutate().request(alterRequest(exchange)).build());
        }

        return Mono.fromCompletionStage(CompletableFuture.runAsync(() -> {
                    String authorization = request.getHeaders().getFirst(HEADER_AUTHORIZATION);
                    log.debug("authorization is:{}", authorization);
                    if (StringUtils.isBlank(authorization)) {
//                        log.warn("gateway warn ,authorization is blank");
                        throw new AuthException(AuthEnums.AUTH_TOKEN_NOT_FIND.getMsg());
                    } else {
                        String[] auths = authorization.split(" ");
                        if (auths.length != 2) {
                            throw new AuthException(AuthEnums.AUTH_TOKEN_NOT_FIND.getMsg());
                        } else {
                            // log.info("auth type is:{}", auths[0]);
                            String headerToken = auths[1];
                            String loginStr =
                                    redisUtil.get(ACCESS_TOKEN_USER_KEY.replace("{access_token}", headerToken), String.class);
                            if (StringUtils.isNullBlank(loginStr)) {
                                loginStr = redisUtil.get(ACCESS_TMP_TOKEN_USER_KEY.replace("{access_token}", headerToken),
                                        String.class);
                                if (StringUtils.isNullBlank(loginStr) || !isTemp(path)) {
                                    throw new AuthException(AuthEnums.AUTH_TOKEN_TIMEOUT.getMsg());
                                }
                            } else {
                                // 将最新请求时间存入缓存
                                this.saveTokenRequestTime(headerToken);
                            }
                            JSONObject json = JSONObject.parseObject(loginStr);
                            String userId = json.getString("userId");
                            if (StringUtils.isNullBlank(userId)) {
                                throw new AuthException(AuthEnums.AUTH_TOKEN_INVALID.getMsg());
                            }
                        }
                    }
                }, executorService))
                .then(Mono.defer(() -> exchange.getSession()
                        .flatMap(session -> chain.filter(exchange.mutate().request(alterRequest(exchange)).build()))))
                .onErrorResume(AuthException.class, e -> {
                    log.error("auth exception {} {} {}",exchange.getRequest().getMethod(), exchange.getRequest().getURI(),e.getMessage());
                    return unAuth(exchange.getResponse(), e.getMessage());
                });
    }

    private boolean isSkip(String path) {
        return authProperties.getSkipUrl().stream().anyMatch(a -> Pattern.compile(a).matcher(path).matches());
    }

    private Mono<Void> unAuth(ServerHttpResponse resp, String msg) {
        resp.setStatusCode(HttpStatus.UNAUTHORIZED);
        resp.getHeaders().add(GatewayConstant.CONTENT_TYPE, GatewayConstant.CONTENT_TYPE_JSON);
        String result = "";
        try {
            result = objectMapper.writeValueAsString(R.failure(HttpStatus.UNAUTHORIZED.value(), msg));
        } catch (JsonProcessingException e) {
            log.error("error", e);
        }
        DataBuffer buffer = resp.bufferFactory().wrap(result.getBytes(StandardCharsets.UTF_8));
        return resp.writeWith(Flux.just(buffer));
    }

    private boolean isTemp(String path) {
        return authProperties.getTempUrl().stream().anyMatch(a -> Pattern.compile(a).matcher(path).matches());
    }

    protected void saveTokenRequestTime(String accessToken) {
        String now = DateFormatUtil.format(new Date());
        long expireTime = 24L;
        String expireTimeStr = redisUtil.get(ACCESS_TOKEN_REQUEST_LATEST_TIME_PERIOD, String.class);
        if (!StringUtils.isNullBlank(expireTimeStr)) {
            expireTime = Long.parseLong(expireTimeStr);
        }
        log.debug("key:{}", ACCESS_TOKEN_REQUEST_LATEST_TIME_KEY.replace("{access_token}", accessToken));
        redisUtil.set(ACCESS_TOKEN_REQUEST_LATEST_TIME_KEY.replace("{access_token}", accessToken), now,
                expireTime * 60L * 60L, TimeUnit.SECONDS);
    }
}
