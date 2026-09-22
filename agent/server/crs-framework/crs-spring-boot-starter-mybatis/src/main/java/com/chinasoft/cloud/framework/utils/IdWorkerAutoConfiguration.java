package com.chinasoft.cloud.framework.utils;

import java.net.UnknownHostException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class IdWorkerAutoConfiguration {

    @Bean
    public IdWorker idWorker(@Value("${pod.ip:127.0.0.1}") String podIp) throws UnknownHostException {
        log.info("initialize id worker with ip:{}", podIp);
        return new IdWorker(podIp);
    }

}
