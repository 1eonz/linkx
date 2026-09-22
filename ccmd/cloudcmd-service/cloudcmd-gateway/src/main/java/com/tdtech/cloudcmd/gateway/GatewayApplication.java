package com.tdtech.cloudcmd.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import com.tdtech.cloudcmd.gateway.props.AuthProperties;
import com.tdtech.cloudcmd.i18n.CommonLocaleConfig;

/**
 * @author zWX523748
 */
@EnableDiscoveryClient
@SpringBootApplication(exclude = CommonLocaleConfig.class)
@RefreshScope
@EnableConfigurationProperties(AuthProperties.class)
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

}
