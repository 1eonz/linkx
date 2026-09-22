package com.tdtech.cloudcmd.cagent.service;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

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

    public UserInfo getLoginInfo(String token) {
        if (StringUtils.isBlank(token)) {
            return null;
        }
        UserInfo info = redisUtil.get(TOKEN_KEY.replace("{access_token}", token), UserInfo.class);
        if (info == null) {
            info = redisUtil.get(ACCESS_TOKEN_TIMEOUT_KEY.replace("{access_token}", token), UserInfo.class);
        }
        if (info == null) {
            return null;
        }
        return info;
    }

    public UserInfo getRedisInfo(String token) {
        if (StringUtils.isBlank(token)) {
            return null;
        }
        UserInfo info = redisUtil.get(TOKEN_KEY.replace("{access_token}", token), UserInfo.class);
        if (info == null) {
            info = redisUtil.get(ACCESS_TOKEN_TIMEOUT_KEY.replace("{access_token}", token), UserInfo.class);
        }
        return info;
    }
}
