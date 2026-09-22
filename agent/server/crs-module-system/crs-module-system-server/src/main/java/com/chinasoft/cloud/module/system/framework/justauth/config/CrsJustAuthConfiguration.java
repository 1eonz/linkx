package com.chinasoft.cloud.module.system.framework.justauth.config;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import com.chinasoft.cloud.module.system.framework.justauth.core.AuthRequestFactory;
import com.xkcoding.justauth.autoconfigure.JustAuthProperties;
import com.xkcoding.justauth.support.cache.RedisStateCache;

import me.zhyd.oauth.cache.AuthStateCache;
import me.zhyd.oauth.enums.AuthResponseStatus;
import me.zhyd.oauth.exception.AuthException;
import me.zhyd.oauth.request.AuthRequest;

/**
 * JustAuth 配置类 TODO 芋艿：等 justauth 1.4.1 版本发布！！！
 *
 * @author 芋道源码
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({JustAuthProperties.class})
public class CrsJustAuthConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "justauth", value = {"enabled"}, havingValue = "true", matchIfMissing = true)
    public AuthRequestFactory authRequestFactory(JustAuthProperties properties, AuthStateCache authStateCache) {
        return new AuthRequestFactory(properties, authStateCache);
    }

    @Bean
    @ConditionalOnProperty(prefix = "justauth", value = {"enabled"}, havingValue = "false", matchIfMissing = true)
    public AuthRequestFactory emptyAuthRequestFactory(JustAuthProperties properties, AuthStateCache authStateCache) {
        return new AuthRequestFactory(properties, authStateCache) {
            @Override
            public List<String> oauthList() {
                throw new AuthException(AuthResponseStatus.UNSUPPORTED);
            }

            @Override
            public AuthRequest get(String source) {
                throw new AuthException(AuthResponseStatus.UNSUPPORTED);
            }
        };
    }

    @Bean
    public AuthStateCache authStateCache(RedisTemplate<String, String> justAuthRedisCacheTemplate,
        JustAuthProperties justAuthProperties) {
        return new RedisStateCache(justAuthRedisCacheTemplate, justAuthProperties.getCache());
    }

}
