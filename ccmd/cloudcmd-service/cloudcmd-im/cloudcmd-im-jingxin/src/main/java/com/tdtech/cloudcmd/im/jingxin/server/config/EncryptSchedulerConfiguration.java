package com.tdtech.cloudcmd.im.jingxin.server.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@Slf4j
public class EncryptSchedulerConfiguration {
    @Bean(name = "imEncryptTaskExecutorService")
    public ThreadPoolTaskExecutor groupAITaskExecutorService() {
        var taskExecutor = new ThreadPoolTaskExecutor();
        // 核心线程池大小
        taskExecutor.setCorePoolSize(10);
        // 最大线程数
        taskExecutor.setMaxPoolSize(100);
        // 队列容量
        taskExecutor.setQueueCapacity(0);
        // 活跃时间
        taskExecutor.setKeepAliveSeconds(30);
        // 线程名字前缀
        taskExecutor.setThreadNamePrefix("IM_Encrypt_");
        // setRejectedExecutionHandler：当pool已经达到max size的时候，如何处理新任务
        // CallerRunsPolicy：不在新线程中执行任务，而是由调用者所在的线程来执行
        taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        taskExecutor.initialize();
        return taskExecutor;
    }
}