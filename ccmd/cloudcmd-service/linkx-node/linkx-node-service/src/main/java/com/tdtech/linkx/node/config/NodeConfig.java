package com.tdtech.linkx.node.config;

import io.netty.channel.nio.NioEventLoopGroup;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(NodeProperties.class)
public class NodeConfig {

    @Bean(destroyMethod = "shutdownGracefully")
    public NioEventLoopGroup nioEventLoopGroup() {
        return new NioEventLoopGroup();
    }
}
