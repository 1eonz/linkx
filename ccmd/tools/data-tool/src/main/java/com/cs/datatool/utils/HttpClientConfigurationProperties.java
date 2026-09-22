package com.cs.datatool.utils;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "cloudcmd.httpclient")
public class HttpClientConfigurationProperties {

    private Duration connectTimeout = Duration.ofSeconds(10L);
    private Duration defaultReadTimeOut = Duration.ofSeconds(30L);

    private Integer coreThreadPoolSize = 1;
    private Integer maxThreadPoolSize = 200;
    private Integer threadQueueSize = 0;
    private Integer threadKeepAliveSeconds = 30;

}
