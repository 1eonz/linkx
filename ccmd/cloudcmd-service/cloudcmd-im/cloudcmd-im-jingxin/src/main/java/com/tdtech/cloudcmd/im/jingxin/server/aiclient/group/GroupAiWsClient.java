package com.tdtech.cloudcmd.im.jingxin.server.aiclient.group;

import com.tdtech.cloudcmd.im.jingxin.client.CachedImConfig;
import com.tdtech.cloudcmd.im.jingxin.client.ImHttpClient;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImHeadersReq;
import com.tdtech.cloudcmd.im.jingxin.client.utils.RandomUtil;
import com.tdtech.cloudcmd.im.jingxin.client.ws.WsClient;
import com.tdtech.cloudcmd.im.jingxin.server.aiclient.ReactAiAgentClient;
import com.tdtech.cloudcmd.im.jingxin.server.aiclient.entity.AiVirtualUser;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.ImUserVirtualMapper;
import com.tdtech.cloudcmd.msip.enums.AlarmTemplateZhEnum;
import com.tdtech.cloudcmd.msip.util.ReportUtil;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.StringUtils;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.SneakyThrows;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PreDestroy;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Configuration
public class GroupAiWsClient implements GroupAiWsClientInterface {

    private static final Logger logger = LoggerFactory.getLogger(GroupAiWsClient.class);

    private final Map<String, WsClient> wsClients = new ConcurrentHashMap<>();

    private final ReportUtil reportUtil;

    private final ReactAiAgentClient reactAiAgentClient;

    private final StreamBridge streamBridge;

    private final ImUserVirtualMapper imUserVirtualMapper;

    private final ImHttpClient coopImHttpClient;

    private final CachedImConfig cachedImConfig;

    private final GroupAiClient groupAiClient;


    public GroupAiWsClient(ReportUtil reportUtil, ReactAiAgentClient reactAiAgentClient,
                           StreamBridge streamBridge, ImUserVirtualMapper imUserVirtualMapper,
                           @Qualifier("coopImHttpClient") ImHttpClient imHttpClient,
                           CachedImConfig cachedImConfig, GroupAiClient groupAiClient) {
        this.reportUtil = reportUtil;
        this.reactAiAgentClient = reactAiAgentClient;
        this.streamBridge = streamBridge;
        this.imUserVirtualMapper = imUserVirtualMapper;
        this.coopImHttpClient = imHttpClient;
        this.cachedImConfig = cachedImConfig;
        this.groupAiClient = groupAiClient;
        Map<Long, String> userMap = groupAiClient.getUserMap();
        for (var map : userMap.entrySet()) {
            wsClients.put(map.getValue(), new WsClient(map.getValue(),
                    new GroupAiWebSocketMessageHandler(map.getKey(), coopImHttpClient, groupAiClient, reactAiAgentClient,
                            reportUtil, streamBridge, imUserVirtualMapper)));
        }
    }

    @Scheduled(fixedDelay = 10000L, initialDelay = 20000L)
    public void keepAllAlive() {
        logger.info("Start keep all group AI websocket alive");
        Map<Long, String> userMap = groupAiClient.getUserMap();
        for (var map : userMap.entrySet()) {
            try {
                keepAlive(map.getValue());
            } catch (Exception e) {
                logger.error("Group AI {} keep alive failed", map.getValue(), e);
            }
        }

        Set<String> userIds = new HashSet<>(userMap.values());

        for (var key : wsClients.keySet()) {
            if (!userIds.contains(key)) {
                close(key);
                wsClients.remove(key);
            }
        }
        logger.info("Keep all group AI websocket alive finished");
    }

    public void keepAlive(String userId) {
        WsClient wsClient = getClient(userId);
        if (!wsClient.isActive()) {
            logger.warn("channel not active, connect now...");
            var user = groupAiClient.getUser(userId);
            if (user == null) {
                logger.warn("User not found for ID: {}", userId);
                wsClients.remove(userId);
                return;
            }
            var wsHost = cachedImConfig.getConfig(GroupAiConstant.GLOBAL_WS_HOST_KEY);
            String cliId = user.getClientId();
            String uscc = cachedImConfig.getConfig(GroupAiConstant.GLOBAL_USCC_KEY);
            String allowSeid = cachedImConfig.getConfig(GroupAiConstant.GLOBAL_ALLOW_SEID);
            String allowSekey = cachedImConfig.getConfig(GroupAiConstant.GLOBAL_ALLOW_SEKEY);
            ImHeadersReq imHeadersReq = new ImHeadersReq();
            imHeadersReq.setUscc(uscc);
            imHeadersReq.setAllowSeid(allowSeid);
            imHeadersReq.setAllowSekey(allowSekey);
            imHeadersReq.setXClientId(cliId);
            try {
                var uri = new URI(wsHost);
                var token = groupAiClient.getToken(userId);
                if (token == null || token.getAccessToken() == null || token.getAccessToken().isBlank()) {
                    logger.warn("keepAlive auth failed");
                    return;
                }
                wsClient.connect(uri, token, imHeadersReq, RandomUtil.randomString());
                // 连接成功，则上报MSIP，擦除活动告警
                clearAlarm();
            } catch (WsClient.HandshakeException e) {
                if (Objects.equals(e.getStatus(), HttpResponseStatus.UNAUTHORIZED)) {
                    groupAiClient.deprecateToken(userId);
                }
                logger.error("ws client connect error:{} {}", wsHost, imHeadersReq, e);
                // 上报与警信服务器连接失败的告警
                reportAlarm(e.getMessage());
            } catch (Exception e) {
                logger.error("connect error:{} {}", wsHost, imHeadersReq, e);
                // 上报与警信服务器连接失败的告警
                reportAlarm(e.getMessage());
            }
        }
    }

    public void addGroupAi(AiVirtualUser user) {
        if (user == null || user.getAgentId() == null) {
            return;
        }
        groupAiClient.addUser(user);
        keepAlive(user.getClientId());
        logger.info("Add Group AI {} ws connection", user.getClientId());
    }

    public void addGroupAi(List<AiVirtualUser> users) {
        if (CollectionUtils.isEmpty(users)) {
            return;
        }
        groupAiClient.addUser(users);
        for (var user : users) {
            if (user.getAgentId() == null) {
                continue;
            }
            keepAlive(user.getClientId());
            logger.info("Add Group AI {} ws connection", user.getClientId());
        }
    }

    public void removeGroupAi(String userId) {
        if (StringUtils.isBlank(userId)) {
            return;
        }
        groupAiClient.removeUser(userId);
        close(userId);
        logger.info("Remove Group AI {} ws connection", userId);
    }

    public void removeGroupAi(List<String> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return;
        }
        groupAiClient.removeUser(userIds);
        for (var userId : userIds) {
            close(userId);
            logger.info("Remove Group AI {} ws connection", userId);
        }
    }

    @PreDestroy
    public void destroy() {
        for (WsClient client : wsClients.values()) {
            try {
                client.close();
            } catch (InterruptedException e) {
                logger.warn("Error closing WebSocket client", e);
                Thread.currentThread().interrupt();
            }
        }
        wsClients.clear();
    }

    @Synchronized
    private WsClient getClient(String userId) {
        WsClient wsClient = wsClients.get(userId);
        if (wsClient == null) {
            groupAiClient.addUser(userId);
            wsClient = new WsClient(userId,
                    new GroupAiWebSocketMessageHandler(groupAiClient.getProxyUserId(userId),
                            coopImHttpClient, groupAiClient, reactAiAgentClient,
                            reportUtil, streamBridge, imUserVirtualMapper));
            wsClients.put(userId, wsClient);
        }
        return wsClient;
    }

    private void reportAlarm(String param) {
        reportUtil.saveAlarm2MSIP(AlarmTemplateZhEnum.IM_SERVER_IS_DISCONNECTED, param);
    }

    private void clearAlarm() {
        reportUtil.clearAlarm2MSIP(AlarmTemplateZhEnum.IM_SERVER_IS_DISCONNECTED);
    }

    @SneakyThrows
    public void close(String userId) {
        var wsClient = wsClients.get(userId);
        if (wsClient != null && wsClient.isActive()) {
            wsClient.close();
        }
        wsClients.remove(userId);
    }
}
