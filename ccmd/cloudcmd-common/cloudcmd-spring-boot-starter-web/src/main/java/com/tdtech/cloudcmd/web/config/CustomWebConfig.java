package com.tdtech.cloudcmd.web.config;

import java.net.UnknownHostException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tdtech.cloudcmd.util.IdWorker;
import com.tdtech.cloudcmd.web.filter.CloudcmdWebFilter;
import com.tdtech.cloudcmd.web.utils.ApplicationContextHolder;
import com.tdtech.cloudcmd.web.utils.HttpClient;
import com.tdtech.cloudcmd.web.utils.HttpClientConfigurationProperties;
import com.tdtech.cloudcmd.web.utils.JsonMapperHolder;

import lombok.extern.slf4j.Slf4j;

/**
 * @author zhuangzl
 * @date 2020-08-09 14:10
 */
@Configuration
@Slf4j
@EnableDiscoveryClient
@EnableConfigurationProperties({HttpClientConfigurationProperties.class, WebConfigurationProperties.class})
public class CustomWebConfig {

    @Bean
    public IdWorker idWorker(@Value("${pod.ip}") String podIp) throws UnknownHostException {
        return new IdWorker(podIp);
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public CloudcmdWebFilter cloudcmdWebFilter() {
        return new CloudcmdWebFilter();
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public FilterRegistrationBean<CloudcmdWebFilter> filterRegistrationBean(CloudcmdWebFilter cloudcmdWebFilter) {
        FilterRegistrationBean<CloudcmdWebFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(cloudcmdWebFilter);
        registration.addUrlPatterns("/*");
        registration.addInitParameter("paramName", "paramValue");
        registration.setName("cloudcmdWebFilter");
        registration.setOrder(1);
        return registration;
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public ApplicationContextHolder applicationContextHolder() {
        return new ApplicationContextHolder();
    }

    @Bean
    public HttpClient httpClient(ObjectMapper objectMapper, HttpClientConfigurationProperties properties) {
        return new HttpClient(objectMapper, properties);
    }

    @Bean
    public JsonMapperHolder.ObjectMapperHolderConfigurer objectMapperHolder(ObjectMapper objectMapper) {
        return new JsonMapperHolder.ObjectMapperHolderConfigurer(objectMapper);
    }

}
