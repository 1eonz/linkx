package com.tdtech.cloudcmd.icp.proxy.ws;

import com.tdtech.cloudcmd.icp.proxy.ws.client.ReconnectableWebSocketClient;
import com.tdtech.cloudcmd.icp.proxy.ws.client.WebSocketClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WsClientAutoConfiguration {

    @Bean(name = "webSocketClient")
    public WebSocketClient webSocketClient(ApplicationEventPublisher publisher,
        ObserverbleWebSocketMessageHandler messageHandler) {
        return new ReconnectableWebSocketClient(publisher, () -> messageHandler);
    }


}
