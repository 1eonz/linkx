package com.tdtech.linkx.node.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * 节点数据透传相关配置。
 */
@Configuration
public class NodeDispatchConfig {

    /**
     * proxy 内部调用微服务的 RestTemplate。
     * K8s service name 通过 CoreDNS 解析，无需 @LoadBalanced。
     * 连接 3s，读取 10s。
     */
    @Bean
    public RestTemplate proxyRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(10000);
        return new RestTemplate(factory);
    }
}
