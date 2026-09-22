package com.tdtech.cloudcmd.auth.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author zWX523748
 */

/**
 * @author zWX523748
 */
@Configuration
@EnableAsync
public class ThreadConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        return bizExecutor();
    }

    @Bean
    public Executor bizExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程池大小
        executor.setCorePoolSize(30);
        // 最大线程数
        executor.setMaxPoolSize(100);
        // 队列容量
        executor.setQueueCapacity(10000);
        // 活跃时间
        executor.setKeepAliveSeconds(30 * 60);
        // 线程名字前缀
        executor.setThreadNamePrefix("Biz-Service-Executor-");
        // setRejectedExecutionHandler：当pool已经达到max size的时候，如何处理新任务
        // CallerRunsPolicy：不在新线程中执行任务，而是由调用者所在的线程来执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

}