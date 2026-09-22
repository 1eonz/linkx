package com.chinasoft.cloud.module.aiagent.framework.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

import com.chinasoft.cloud.framework.security.config.AuthorizeRequestsCustomizer;
import com.chinasoft.cloud.module.aiagent.enums.ApiConstants;

@Configuration(proxyBeanMethods = false, value = "aiAgentSecurityConfiguration")
public class SecurityConfiguration {

    @Bean("aiAgentAuthorizeRequestsCustomizer")
    public AuthorizeRequestsCustomizer authorizeRequestsCustomizer() {
        return new AuthorizeRequestsCustomizer() {

            @Override
            public void customize(
                AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
                // TODO 芋艿：这个每个项目都需要重复配置，得捉摸有没通用的方案
                // Swagger 接口文档
                registry.requestMatchers("/v3/api-docs/**").permitAll().requestMatchers("/webjars/**").permitAll()
                    .requestMatchers("/swagger-ui").permitAll().requestMatchers("/swagger-ui/**").permitAll();
                // Druid 监控
                registry.requestMatchers("/druid/**").permitAll();
                // Spring Boot Actuator 的安全配置
                registry.requestMatchers("/actuator").permitAll().requestMatchers("/actuator/**").permitAll();
                // RPC 服务的安全配置
                registry.requestMatchers(ApiConstants.PREFIX + "/**").permitAll();
                // AI
                registry.requestMatchers("/deepseek-zjk/**").permitAll();
                registry.requestMatchers("/app-api/deepseek-zjk/**").permitAll();
                registry.requestMatchers("/proxy/ai/v1/deepseek-zjk/**").permitAll();
                registry.requestMatchers("/app-api/proxy/ai/v1/deepseek-zjk/**").permitAll();
                registry.requestMatchers("/proxy/ai/v1/aiagent/management/**").permitAll();
                // AI 附件配置接口
                registry.requestMatchers("/proxy/ai/v1/aiagent/attachment-config/**").permitAll();
            }
        };
    }

}
