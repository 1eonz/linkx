package com.tdtech.cloudcmd.gateway.config;

import java.net.UnknownHostException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.tdtech.cloudcmd.util.IdWorker;

/**
 * 服务公告配置类
 * 
 * @author zWX523748
 */
@Configuration
public class ServiceConfig {

    @Bean
    public IdWorker idWorker(@Value("${pod.ip}") String podIp) throws UnknownHostException {
        return new IdWorker(podIp);
    }

    @Bean(name = "blockTaskExecutorService")
    public ExecutorService blockTaskExecutorService() {
        ThreadGroup group = new ThreadGroup("blockTaskExecutorService");
        return new ThreadPoolExecutor(8, 100, 10L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(200),
            r -> new Thread(group, r), new ThreadPoolExecutor.CallerRunsPolicy());
    }
}
