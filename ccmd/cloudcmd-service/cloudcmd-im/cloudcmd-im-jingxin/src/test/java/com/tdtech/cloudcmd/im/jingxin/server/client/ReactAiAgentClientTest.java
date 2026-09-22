package com.tdtech.cloudcmd.im.jingxin.server.client;

import com.tdtech.cloudcmd.im.jingxin.client.CachedImConfig;
import com.tdtech.cloudcmd.im.jingxin.server.aiclient.ReactAiAgentClient;
import com.tdtech.cloudcmd.im.jingxin.server.aiclient.entity.AskAIReq;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
public class ReactAiAgentClientTest {
    public static void main(String[] args) throws Exception {
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
        var reactAiAgentClient = new ReactAiAgentClient(new CachedImConfig() {
            @Override
            public String getConfig(String key) {
                return "http://10.28.64.80:30019/";
            }
        }, taskExecutor);
        AskAIReq req = new AskAIReq();
        req.setUserName("群AI助手");
        req.setUserID("26765031121925");
        req.setContent("123");
        reactAiAgentClient.askAI(req, answer -> log.info("{}", answer));
        Thread.sleep(30000L);
        taskExecutor.shutdown();
    }
}
