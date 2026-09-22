package com.tdtech.cloudcmd.linkx.dashboard.config;

import com.tdtech.cloudcmd.util.nodedispatch.NodeDispatchClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * 节点数据转发客户端配置。
 * 通过 K8s service name 内网直连 linkx-node-service，不走 APISIX。
 * 连接 3s，读取 10s。
 */
@Configuration
public class NodeDispatchConfig {

    @Bean(name = "nodeDispatchRestTemplate")
    public RestTemplate nodeDispatchRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(10000);
        return new RestTemplate(factory);
    }

    @Bean
    public NodeDispatchClient nodeDispatchClient(
            @Qualifier("nodeDispatchRestTemplate") RestTemplate restTemplate) {
        return new NodeDispatchClient(restTemplate);
    }
}
