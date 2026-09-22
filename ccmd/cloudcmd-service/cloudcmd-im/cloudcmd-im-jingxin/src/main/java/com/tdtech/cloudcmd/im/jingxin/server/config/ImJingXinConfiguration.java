package com.tdtech.cloudcmd.im.jingxin.server.config;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;


import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class ImJingXinConfiguration {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean(name = "groupAITaskExecutorService")
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
        taskExecutor.setThreadNamePrefix("Group_AI");
        // setRejectedExecutionHandler：当pool已经达到max size的时候，如何处理新任务
        // CallerRunsPolicy：不在新线程中执行任务，而是由调用者所在的线程来执行
        taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        taskExecutor.initialize();
        return taskExecutor;
    }

    @Bean(name = "groupInfoExecutorService")
    public ThreadPoolTaskExecutor groupInfoExecutorService() {
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
        taskExecutor.setThreadNamePrefix("Group_Info");
        // setRejectedExecutionHandler：当pool已经达到max size的时候，如何处理新任务
        // CallerRunsPolicy：不在新线程中执行任务，而是由调用者所在的线程来执行
        taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        taskExecutor.initialize();
        return taskExecutor;
    }

    @Bean(name = "collaborationPostExecutorService")
    public ThreadPoolTaskExecutor collaborationPostExecutorService() {
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
        taskExecutor.setThreadNamePrefix("CollaborationPost");
        // setRejectedExecutionHandler：当pool已经达到max size的时候，如何处理新任务
        // CallerRunsPolicy：不在新线程中执行任务，而是由调用者所在的线程来执行
        taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        taskExecutor.initialize();
        return taskExecutor;
    }

    @Bean(name = "warningMessageExecutorService")
    public ThreadPoolTaskExecutor warningMessageExecutorService() {
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
        taskExecutor.setThreadNamePrefix("warningMessage");
        // setRejectedExecutionHandler：当pool已经达到max size的时候，如何处理新任务
        // CallerRunsPolicy：不在新线程中执行任务，而是由调用者所在的线程来执行
        taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        taskExecutor.initialize();
        return taskExecutor;

    }
    @Bean(name = "creatGroupExecutorService")
    public ThreadPoolTaskExecutor creatGroupExecutorService() {
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
        taskExecutor.setThreadNamePrefix("creatGroup");
        // setRejectedExecutionHandler：当pool已经达到max size的时候，如何处理新任务
        // CallerRunsPolicy：不在新线程中执行任务，而是由调用者所在的线程来执行
        taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        taskExecutor.initialize();
        return taskExecutor;
    }

    @Bean(name = "userNodeQueryExecutorService")
    public ThreadPoolTaskExecutor userNodeQueryExecutorService() {
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
        taskExecutor.setThreadNamePrefix("UserNodeQuery");
        // setRejectedExecutionHandler：当pool已经达到max size的时候，如何处理新任务
        // CallerRunsPolicy：不在新线程中执行任务，而是由调用者所在的线程来执行
        taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        taskExecutor.initialize();
        return taskExecutor;
    }
}