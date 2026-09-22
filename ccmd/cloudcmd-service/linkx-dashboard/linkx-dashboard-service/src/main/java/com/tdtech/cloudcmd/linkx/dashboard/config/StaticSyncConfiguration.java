package com.tdtech.cloudcmd.linkx.dashboard.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 统计同步相关配置。
 * <p>
 * 提供 {@code linkxDashboardTaskExecutorService}：统计同步任务专用线程池，
 * 供 5 张表的同步任务异步执行，避免阻塞调度线程。
 */
@Configuration
public class StaticSyncConfiguration {

    /**
     * 统计同步任务专用线程池。
     * <p>
     * 核心线程数 5 对应 5 张表的同步任务；队列容量 0 让任务直接走线程池，
     * 满载时由 CallerRunsPolicy 在调用者线程（调度线程）执行，起到背压作用避免任务堆积。
     */
    @Bean(name = "linkxDashboardTaskExecutorService")
    public ThreadPoolTaskExecutor linkxDashboardTaskExecutorService() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(0);
        executor.setKeepAliveSeconds(30);
        executor.setThreadNamePrefix("Linkx_Dashboard_Sync_");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}