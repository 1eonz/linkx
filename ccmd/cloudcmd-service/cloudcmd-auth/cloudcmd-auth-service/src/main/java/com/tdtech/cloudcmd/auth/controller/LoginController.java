package com.tdtech.cloudcmd.auth.controller;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttributes;

import com.tdtech.cloudcmd.auth.constant.AuthBaseConstant;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.constant.HeaderConstants;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * @author zhuangzl
 * @date 2020-05-20 17:21
 */
@RestController
@RequestMapping("/auth/v1")
@SessionAttributes
@Slf4j
@Tag(name = "认证接口", description = "提供用户认证相关接口")
public class LoginController {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @RequestMapping("/heartbeat")
    @ResponseBody
    public R heartbeat(@RequestHeader(HeaderConstants.HEADER_TOKEN) String token) throws Exception {
        if (validateToken(token)) {
            return R.success();
        }
        return R.failure();
    }

    private boolean validateToken(String ssoToken) {
        String ssoTokenKey = AuthBaseConstant.TOKEN_TO_USER + ssoToken;
        String jsonStr = redisTemplate.opsForValue().get(ssoTokenKey);
        if (StringUtils.isEmpty(jsonStr)) {
            return false;
        }
        return true;
    }

}
