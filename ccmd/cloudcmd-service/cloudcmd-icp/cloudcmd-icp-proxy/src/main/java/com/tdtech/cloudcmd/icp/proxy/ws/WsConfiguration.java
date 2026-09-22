package com.tdtech.cloudcmd.icp.proxy.ws;

import com.tdtech.cloudcmd.icp.proxy.conf.IcpProperties;
import com.tdtech.cloudcmd.icp.proxy.service.AuthService;
import com.tdtech.cloudcmd.icp.proxy.service.CameraLevelService;
import com.tdtech.cloudcmd.icp.proxy.service.CameraService;
import com.tdtech.cloudcmd.icp.proxy.service.DepartmentService;
import com.tdtech.cloudcmd.icp.proxy.service.GisService;
import com.tdtech.cloudcmd.icp.proxy.service.OnlineStatusService;
import com.tdtech.cloudcmd.icp.proxy.service.UserService;
import com.tdtech.cloudcmd.icp.proxy.ws.client.WebSocketClient;
import com.tdtech.cloudcmd.icp.proxy.ws.event.HttpAuthSucceedEvent;
import com.tdtech.cloudcmd.icp.proxy.ws.event.WsAuthSucceedEvent;
import com.tdtech.cloudcmd.icp.proxy.ws.event.WsHandshakeFinishedEvent;
import com.tdtech.cloudcmd.icp.proxy.ws.protocal.CmdTypeEnum;
import com.tdtech.cloudcmd.util.IdWorker;
import com.tdtech.cloudcmd.util.json.JsonUtil;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.HashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WsConfiguration {

    private final ScheduledExecutorService taskScheduler =
        new ScheduledThreadPoolExecutor(1, runnable -> new Thread(runnable, "websocket-login-thread"));

    private final ObserverbleWebSocketMessageHandler messageHandler;
    private final WebSocketClient webSocketClient;
    private final ApplicationEventPublisher publisher;
    private final IcpProperties properties;
    private final IdWorker idWorker;

    private final AuthService authService;
    private final GisService gisService;
    private final OnlineStatusService onlineStatusService;
    private final CameraService cameraService;
    private final CameraLevelService cameraLevelService;
    private final DepartmentService departmentService;
    private final UserService userService;

    @PreDestroy
    public void preDestroy() {
        taskScheduler.shutdownNow();
        disConnect();
    }

    public void disConnect() {
        if (webSocketClient.isActive()) {
            webSocketClient.closeChannel();
        }
    }

    @PostConstruct
    public void postConstruct() {
        messageHandler.register(CmdTypeEnum.GIS_NOTIFY, 1L, gisService::handleWsNotify);
        messageHandler.register(CmdTypeEnum.RESOURCE_NOTIFY, 2L, onlineStatusService::handleWsNotify);
        messageHandler.register(CmdTypeEnum.COMMON_NOTIFY, 3L, cameraService::handleWsNotify);
        messageHandler.register(CmdTypeEnum.COMMON_NOTIFY, 7L, cameraService::handleGisNotify);
        messageHandler.register(CmdTypeEnum.COMMON_NOTIFY, 4L, cameraLevelService::handleWsNotify);
        messageHandler.register(CmdTypeEnum.COMMON_NOTIFY, 5L, departmentService::handleWsNotify);
        messageHandler.register(CmdTypeEnum.COMMON_NOTIFY, 6L, userService::handleWsNotify);
    }

    /**
     * reconnect ws after http session changed
     */
    @Async
    @EventListener(HttpAuthSucceedEvent.class)
    public void httpLoginHandler() {
        log.info("on reauth, reconnect ws connection now...");
        if (webSocketClient.isActive()) {
            // 关链接 等重连 然后重新订阅
            webSocketClient.closeChannel();
        }
        webSocketClient.connect(properties.getWsUri());
    }

    /**
     * send ws login message after ws handshake
     */
    @Async
    @EventListener(WsHandshakeFinishedEvent.class)
    public void wsLogin(WsHandshakeFinishedEvent event) {
        var sessionStr = authService.getSessionStr();
        // websocket auth
        HashMap<String, String> param = new HashMap<>();
        param.put("cmd", CmdTypeEnum.REGISTER_EVENTS.cmd());
        param.put("isdn", properties.getIcpAccount());
        param.put("session", sessionStr);
        Channel channel = event.getSource();
        log.info("ws login:{}", param);
        channel.writeAndFlush(new TextWebSocketFrame(JsonUtil.toJsonStr(param)));
        final AtomicBoolean flag = new AtomicBoolean(false);
        var consumerId = idWorker.nextId();

        // register callback
        messageHandler.register(CmdTypeEnum.REGISTER_EVENTS, consumerId, payload -> {
            try {
                log.debug("websocket register result:{}", payload);
                RegisterEventResp resp = JsonUtil.convert(payload, RegisterEventResp.class);
                if (resp.rsp != 0) {
                    // response failed close connection and wait for retry
                    log.error("web socket login failed:{}", payload);
                    channel.close();
                } else {
                    WsConfiguration.this.publisher.publishEvent(new WsAuthSucceedEvent());
                }
            } catch (Throwable e) {
                // exception then close connection and wait for retry
                log.error("", e);
                if (channel.isActive()) {
                    channel.close();
                }
            } finally {
                messageHandler.unregister(CmdTypeEnum.REGISTER_EVENTS, consumerId);
            }
            flag.set(true);
        });

        // register out of time check
        taskScheduler.schedule(() -> {
            if (!flag.get()) {
                log.error("web socket login out of time,now closing connection and wait for retry");
                if (channel.isActive()) {
                    channel.close();
                }
            }
        }, 5L, TimeUnit.SECONDS);
    }

    @Getter
    @Setter
    public static class RegisterEventResp {
        private Integer rsp;
    }

}
