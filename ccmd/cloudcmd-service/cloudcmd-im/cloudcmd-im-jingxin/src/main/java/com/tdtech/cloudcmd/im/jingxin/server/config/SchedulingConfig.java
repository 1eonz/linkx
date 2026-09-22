package com.tdtech.cloudcmd.im.jingxin.server.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 定时任务配置类
 * 配置多线程定时任务，避免任务相互阻塞
 *
 * @author s063874
 * @date 2026/4/28
 **/
@Slf4j
@Configuration
@EnableScheduling
public class SchedulingConfig implements SchedulingConfigurer {

    /**
     * 定时任务线程池大小
     * 根据定时任务数量设置，建议 >= 定时任务数量
     */
    private static final int POOL_SIZE = 10;

    /**
     * 线程名称前缀
     */
    private static final String THREAD_NAME_PREFIX = "im-scheduler-";

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(POOL_SIZE, new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, THREAD_NAME_PREFIX + counter.getAndIncrement());
                thread.setDaemon(true);
                thread.setUncaughtExceptionHandler((t, e) ->
                        log.error("uncaught exception in scheduler thread {}", t.getName(), e));
                return thread;
            }
        });
        taskRegistrar.setScheduler(executor);
    }
}