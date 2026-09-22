package com.tdtech.cloudcmd.cagent.remote;

import javax.annotation.Resource;

import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import com.tdtech.cloudcmd.util.json.JsonUtil;

@Configuration
public class ScheduledTask {
    private static final String[] channels =
        new String[] {"cloudcmd-cagent", "invalid_token", "messageToCagent", "sysMessageToCagent", "adminTOCagent"};
    @Resource
    private StreamBridge streamBridge;
    @Resource
    private RemoteClient remoteClient;

    @Scheduled(initialDelay = 10000L, fixedDelay = 5000L)
    public void scheduledTask() {
        for (var channel : channels) {
            var array = remoteClient.pullMsg(channel);
            for (var json : array) {
                streamBridge.send(channel, JsonUtil.toJsonStr(json));
            }
        }
    }
}
