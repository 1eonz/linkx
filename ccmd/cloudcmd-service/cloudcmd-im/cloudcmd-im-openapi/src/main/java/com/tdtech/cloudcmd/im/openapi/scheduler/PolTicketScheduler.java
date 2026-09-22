package com.tdtech.cloudcmd.im.openapi.scheduler;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.tdtech.cloudcmd.im.jingxin.api.entity.polTicket.PoliceTicketClient;
import com.tdtech.cloudcmd.im.openapi.repo.PoliceTicketClientMapper;
import com.tdtech.cloudcmd.msip.enums.AlarmTemplateZhEnum;
import com.tdtech.cloudcmd.msip.util.ReportUtil;
import com.tdtech.cloudcmd.redis.RedisUtil;
import com.tdtech.cloudcmd.util.json.JsonUtil;
import com.tdtech.cloudcmd.web.utils.HttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.Map;

@Slf4j
@Configuration
public class PolTicketScheduler {

    private static final String REDIS_KEY = "cloudcmd:im:openapi:polticket:execute-time";

    @Resource
    private PoliceTicketClientMapper policeTicketClientMapper;
    @Resource
    private RedisUtil redisUtil;
    @Resource
    private HttpClient httpClient;

    @Resource
    private ReportUtil reportUtil;

    @Resource
    @Qualifier("groupAITaskExecutorService")
    private ThreadPoolTaskExecutor taskExecutor;

    @Resource
    private StreamBridge streamBridge;

    @Scheduled(initialDelay = 10000L, fixedDelay = 60L * 1000L)
    public void scheduled() {
        var policeTicketClients = policeTicketClientMapper.selectList(Wrappers.lambdaQuery(
            PoliceTicketClient.class).eq(PoliceTicketClient::getStatus, 0));
        if (policeTicketClients == null || policeTicketClients.isEmpty()) {
            log.warn("no config found");
            return;
        }
        var now = System.currentTimeMillis();
        for (var config : policeTicketClients) {
            taskExecutor.execute(() -> {
                var lastTime = redisUtil.hGet(REDIS_KEY, config.getId() + "", Long.class);
                lastTime = lastTime == null ? 0L : lastTime;
                if (config.getExecutePeriod() + lastTime < now) {
                    var uri = HttpClient.buildUri(
                        config.getSchema() + "://" + config.getIp() + ":" + config.getPort()
                            + config.getPath(), JsonUtil.parseJson(config.getParams()));
                    var resp = httpClient.sendJsonRequest(uri,
                        HttpClient.HttpMethodEnum.valueOf(config.getMethod()),
                        JsonUtil.parseJson(config.getHeaders(), new TypeReference<>() {
                        }), config.getBody(), Duration.ofSeconds(30L));
                    var body = new String(resp.body());
                    log.info("send req:{} resp:{} {}", config, resp.statusCode(), body);
                    if (resp.statusCode() != 200) {
                        // 上报告警
                        reportAlarm(config.getBody());
                        return;
                    }
                    // 擦除告警
                    clearAlarm();
                    processAsync(config.getSystemCode(), body);
                    redisUtil.hSet(REDIS_KEY, config.getId() + "", now);
                }
            });
        }
    }

    private void reportAlarm(String requestContent) {
        // 系统任务，没有用户名
        String userName = "system";
        reportUtil.saveAlarm2MSIP(AlarmTemplateZhEnum.INVOKING_THE_ALARM_TICKET_SYSTEM_FAILED, userName, requestContent);
    }

    private void clearAlarm() {
        reportUtil.clearAlarm2MSIP(AlarmTemplateZhEnum.INVOKING_THE_ALARM_TICKET_SYSTEM_FAILED);
    }

    public void processAsync(String systemCode, String payload) {
        streamBridge.send("cloudcmd-im-jingxin-polticket",
            Map.of("systemCode", systemCode, "param", payload));
    }
}
