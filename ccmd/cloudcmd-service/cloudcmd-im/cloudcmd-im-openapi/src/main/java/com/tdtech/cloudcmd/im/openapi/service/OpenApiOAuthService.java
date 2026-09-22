package com.tdtech.cloudcmd.im.openapi.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.im.openapi.controller.constant.BusinessScopeEnum;
import com.tdtech.cloudcmd.im.openapi.controller.entity.Token;
import com.tdtech.cloudcmd.im.openapi.repo.OpenApplicationGrant;
import com.tdtech.cloudcmd.im.openapi.repo.OpenApplicationGrantMapper;
import com.tdtech.cloudcmd.redis.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author ly
 * @date 2025/8/25
 **/
@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class OpenApiOAuthService {
    private static final SecureRandom numberGenerator = new SecureRandom();
    private static final String TOKEN_REDIS_PATTERN = "cloudcmd:openapi:token:%s";

    private final RedisUtil redisUtil;
    private final OpenApplicationGrantMapper OpenApplicationGrantMapper;

    public OpenApplicationGrant findConfig(@NotBlank String clientId, @NotBlank String clientSecret) {
        OpenApplicationGrant grant = OpenApplicationGrantMapper
                .selectOne(Wrappers.lambdaQuery(OpenApplicationGrant.class).eq(OpenApplicationGrant::getClientId, clientId)
                        .eq(OpenApplicationGrant::getClientSecret, clientSecret).eq(OpenApplicationGrant::getStatus, 1));
        // expired 为 null 表示永久有效；非 null 时需校验是否已过期
        if (grant != null && grant.getExpired() != null
                && grant.getExpired().before(new Date())) {
            log.warn("oauth login client is expired, clientId: {}, expired: {}", clientId, grant.getExpired());
            throw new BusinessException("application grant expired, expired=" + grant.getExpired());
        }
        return grant;
    }

    public void remove(@NotBlank String token) {
        var key = String.format(TOKEN_REDIS_PATTERN, token);
        redisUtil.del(key, token);
    }

    private List<String> getScope(OpenApplicationGrant clientConf) {
        switch (clientConf.getClientType()) {
            case "1": {
                return Arrays.stream(BusinessScopeEnum.values()).map(BusinessScopeEnum::getCode)
                        .collect(Collectors.toList());
            }
            case "2": {
                return List.of(BusinessScopeEnum.THIRD_PARTY_TASKS.getCode());
            }
            case "0": {
                return List.of(BusinessScopeEnum.COLLABORATIVE_STATISTICS.getCode());
            }
            case "3": {
                return List.of(BusinessScopeEnum.POLICE_TICKET.getCode());
            }
            case "4": {
                return List.of(BusinessScopeEnum.GROUP.getCode());
            }
            case "5": {
                return List.of(BusinessScopeEnum.AGENT_USER.getCode());
            }
            default: {
                return List.of();
            }
        }
    }

    public Token create(OpenApplicationGrant clientConf) {
        Token token =
                Token.builder().accessToken(getSecureRandom()).tokenType("Bearer").scope(getScope(clientConf))
                        .clientId(clientConf.getClientId()).build();
        var key = String.format(TOKEN_REDIS_PATTERN, token.getAccessToken());
        if (clientConf.getTokenTime() != null && clientConf.getTokenTime() != -1L) {
            token.setExpireIn(clientConf.getTokenTime().intValue());
            redisUtil.set(key, token, clientConf.getTokenTime(), TimeUnit.SECONDS);
        } else {
            redisUtil.set(key, token);
        }
        return token;
    }

    private String getSecureRandom() {
        byte[] randomBytes = new byte[24];
        numberGenerator.nextBytes(randomBytes);
        StringBuilder hexString = new StringBuilder();
        for (byte randomByte : randomBytes) {
            String hex = Long.toHexString(0xff & randomByte);
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public Token getToken(@NotBlank String token) {
        var key = String.format(TOKEN_REDIS_PATTERN, token);
        return redisUtil.get(key, Token.class);
    }
}