package com.tdtech.cloudcmd.im.jingxin.client;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WsClientScheduler {

    private final ObjectProvider<ImWsClient> imWsClients;

    @Scheduled(fixedDelay = 10000L, initialDelay = 20000L)
    public void scheduledTask() {
        imWsClients.forEach(wsc -> {
            try {
                wsc.keepAlive();
            } catch (Exception e) {
                log.error("{},error", wsc.getName(), e);
            }
        });
    }

}
