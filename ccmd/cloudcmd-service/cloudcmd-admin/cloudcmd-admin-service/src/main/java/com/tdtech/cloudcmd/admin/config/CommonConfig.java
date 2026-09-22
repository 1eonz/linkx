package com.tdtech.cloudcmd.admin.config;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author mWX556161
 * @date 2020/6/8 17:43
 */
@Configuration
@EnableConfigurationProperties({HeadShotPersistConfigurationProperties.class,BaseMapPersistConfigurationProperties.class})
public class CommonConfig {

    @Bean
    @Primary
    public ThreadPoolTaskExecutor customThreadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程池大小
        executor.setCorePoolSize(5);
        // 最大线程数
        executor.setMaxPoolSize(100);
        // 队列容量
        executor.setQueueCapacity(0);
        // 活跃时间
        executor.setKeepAliveSeconds(30);
        // 线程名字前缀
        executor.setThreadNamePrefix("Async-Task-Thread-");
        // setRejectedExecutionHandler：当pool已经达到max size的时候，如何处理新任务
        // CallerRunsPolicy：不在新线程中执行任务，而是由调用者所在的线程来执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
