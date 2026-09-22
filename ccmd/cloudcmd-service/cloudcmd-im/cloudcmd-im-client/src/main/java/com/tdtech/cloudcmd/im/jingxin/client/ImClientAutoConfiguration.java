package com.tdtech.cloudcmd.im.jingxin.client;

import com.tdtech.cloudcmd.msip.util.ReportUtil;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.tdtech.cloudcmd.im.jingxin.client.aop.RetryUnAuthAspect;
import com.tdtech.cloudcmd.im.jingxin.client.ai.AiAgentRecordClient;
import com.tdtech.cloudcmd.redis.RedisLockFactory;
import com.tdtech.cloudcmd.redis.RedisUtil;
import com.tdtech.cloudcmd.web.utils.HttpClient;

@Configuration
public class ImClientAutoConfiguration {

    @Bean
    public CachedImConfig cachedImConfig() {
        return new CachedImConfig();
    }

    @Bean
    public AiAgentRecordClient aiAgentRecordClient(CachedImConfig cachedImConfig, HttpClient httpClient) {
        return new AiAgentRecordClient(cachedImConfig, httpClient);
    }

    @Bean("groupSupportTaskExecutor")
    public ThreadPoolTaskExecutor groupSupportTaskExecutor() {
        var taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(10);
        taskExecutor.setMaxPoolSize(10);
        taskExecutor.setQueueCapacity(1000);
        taskExecutor.setThreadNamePrefix("im-group-support-");
        taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        taskExecutor.initialize();
        return taskExecutor;
    }

    @Primary
    @Bean("coopImHttpClient")
    @ConditionalOnProperty(value = "cloudcmd.im.jinxing.coop.httpclient.enabled",havingValue = "true")
    public ImHttpClient imHttpClient(HttpClient httpClient, RedisLockFactory redisLockFactory,
                                     CachedImConfig cachedImConfig, RedisUtil redisUtil, ReportUtil reportUtil,
                                     @Qualifier("groupSupportTaskExecutor") ThreadPoolTaskExecutor groupSupportTaskExecutor) {
        return new ImHttpClient(httpClient, cachedImConfig, redisLockFactory, redisUtil, reportUtil,
            ClientConfigGroup.coopConfigGroup(), groupSupportTaskExecutor);
    }

    @Bean("groupAIImHttpClient")
    @ConditionalOnProperty(value = "cloudcmd.im.jinxing.groupai.httpclient.enabled",havingValue = "true")
    public ImHttpClient groupAIImHttpClient(HttpClient httpClient, RedisLockFactory redisLockFactory,
        CachedImConfig cachedImConfig, RedisUtil redisUtil,ReportUtil reportUtil,
        @Qualifier("groupSupportTaskExecutor") ThreadPoolTaskExecutor groupSupportTaskExecutor) {
        return new ImHttpClient(httpClient, cachedImConfig, redisLockFactory, redisUtil, reportUtil,
            ClientConfigGroup.aiConfigGroup(), groupSupportTaskExecutor);
    }
    @Bean("oneO1p4BImHttpClient")
    @ConditionalOnProperty(value = "cloudcmd.im.jinxing.1o1p4B.httpclient.enabled",havingValue = "true")
    public ImHttpClient oneO1p4BImHttpClient(HttpClient httpClient, RedisLockFactory redisLockFactory,
        CachedImConfig cachedImConfig, RedisUtil redisUtil, ReportUtil reportUtil,
        @Qualifier("groupSupportTaskExecutor") ThreadPoolTaskExecutor groupSupportTaskExecutor) {
        return new ImHttpClient(httpClient, cachedImConfig, redisLockFactory, redisUtil, reportUtil,
            ClientConfigGroup.oneO1p4BConfigGroup(), groupSupportTaskExecutor);
    }

    @Bean("warningImHttpClient")
    @ConditionalOnProperty(value = "cloudcmd.im.jinxing.warning.httpclient.enabled",havingValue = "true")
    public ImHttpClient warningImHttpClient(HttpClient httpClient, RedisLockFactory redisLockFactory,
                                             CachedImConfig cachedImConfig, RedisUtil redisUtil, ReportUtil reportUtil,
                                             @Qualifier("groupSupportTaskExecutor") ThreadPoolTaskExecutor groupSupportTaskExecutor) {
        return new ImHttpClient(httpClient, cachedImConfig, redisLockFactory, redisUtil, reportUtil,
                ClientConfigGroup.warningConfigGroup(), groupSupportTaskExecutor);
    }

    @Bean("coopImWsClient")
    @ConditionalOnProperty(value = "cloudcmd.im.jinxing.coop.wsclient.enabled",havingValue = "true")
    public ImWsClient coopImWsClient(CachedImConfig cachedImConfig,
        @Qualifier("coopImHttpClient") ImHttpClient imHttpClient, StreamBridge streamBridge) {
        return new ImWsClient("coopImWsClient", cachedImConfig, imHttpClient, streamBridge);
    }

    @Bean("groupAIWsClient")
    @ConditionalOnProperty(value = "cloudcmd.im.jinxing.groupai.wsclient.enabled",havingValue = "true")
    public ImWsClient groupAIWsClient(CachedImConfig cachedImConfig,
        @Qualifier("groupAIImHttpClient") ImHttpClient imHttpClient, StreamBridge streamBridge) {
        return new ImWsClient("groupAIWsClient", cachedImConfig, imHttpClient, streamBridge);
    }

    @Bean("oneO1p4BWsClient")
    @ConditionalOnProperty(value = "cloudcmd.im.jinxing.1o1p4B.wsclient.enabled",havingValue = "true")
    public ImWsClient oneO1p4BWsClient(CachedImConfig cachedImConfig,
        @Qualifier("oneO1p4BImHttpClient") ImHttpClient imHttpClient, StreamBridge streamBridge) {
        return new ImWsClient("oneO1p4BWsClient", cachedImConfig, imHttpClient, streamBridge);
    }

    @Bean
    public RetryUnAuthAspect retryUnAuthAspect() {
        return new RetryUnAuthAspect();
    }

}