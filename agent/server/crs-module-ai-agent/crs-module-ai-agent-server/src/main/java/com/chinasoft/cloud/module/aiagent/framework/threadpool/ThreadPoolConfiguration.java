package com.chinasoft.cloud.module.aiagent.framework.threadpool;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ThreadPoolConfiguration {

    @Bean
    public ThreadPoolTaskExecutor askPool() {
        var executorService = new ThreadPoolTaskExecutor();
        // 核心线程池大小
        executorService.setCorePoolSize(50);
        // 最大线程数
        executorService.setMaxPoolSize(600);
        // 队列容量
        executorService.setQueueCapacity(0);
        // 活跃时间
        executorService.setKeepAliveSeconds(30);
        // 线程名字前缀
        executorService.setThreadNamePrefix("Ask-Task-Thread-");
        // setRejectedExecutionHandler：当pool已经达到max size的时候，如何处理新任务
        // CallerRunsPolicy：不在新线程中执行任务，而是由调用者所在的线程来执行
        executorService.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executorService.initialize();
        return executorService;
    }

}
