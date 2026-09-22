package com.tdtech.cloudcmd.cagent.service;

import java.time.Duration;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.tdtech.cloudcmd.cagent.remote.RemoteClient;
import com.tdtech.cloudcmd.redis.RedisUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * 用户权限校验
 *
 * @author : mWX556161
 * @date : 2020-05-13 16:32
 */
@Slf4j
@Component
public class AuthManager {

    private static final String TOKEN_KEY = "icp-x:auth:token:{access_token}";

    private static final String ACCESS_TOKEN_TIMEOUT_KEY = "icp-x:auth:timeout:token:{access_token}";
    @Resource
    private RedisUtil redisUtil;
    @Resource
    private RemoteClient remoteClient;

    public UserInfo getLoginInfo(String token) {
        return getUserInfo(token);
    }

    public UserInfo getRedisInfo(String token) {
        return getUserInfo(token);
    }

    private UserInfo getUserInfo(String token) {
        var auth = redisUtil.get(TOKEN_KEY.replace("{access_token}", token), UserInfo.class);
        if (auth == null) {
            auth = remoteClient.auth(token);
            if (auth != null) {
                redisUtil.set(TOKEN_KEY.replace("{access_token}", token), auth, Duration.ofSeconds(60L));
            }
        }
        return auth;
    }
}
