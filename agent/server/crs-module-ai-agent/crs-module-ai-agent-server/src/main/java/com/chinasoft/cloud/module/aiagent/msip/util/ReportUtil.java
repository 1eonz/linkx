package com.chinasoft.cloud.module.aiagent.msip.util;

import com.chinasoft.cloud.framework.common.util.json.JsonUtils;
import com.chinasoft.cloud.module.aiagent.msip.constant.MSIPConstant;
import com.chinasoft.cloud.module.aiagent.msip.entity.ActiveAlarmsClearRequest;
import com.chinasoft.cloud.module.aiagent.msip.entity.ActiveAlarmsRequest;
import com.chinasoft.cloud.module.aiagent.msip.entity.AlarmTemplateRequest;
import com.chinasoft.cloud.module.aiagent.msip.entity.MSIPResponse;
import com.chinasoft.cloud.module.aiagent.msip.entity.OperationLog;
import com.chinasoft.cloud.module.aiagent.msip.enums.AlarmTemplate;
import com.chinasoft.cloud.module.aiagent.msip.enums.AlarmTemplateEnEnum;
import com.chinasoft.cloud.module.aiagent.msip.enums.AlarmTemplateZhEnum;
import com.chinasoft.cloud.module.aiagent.msip.enums.MSIPResponseEnum;
import com.chinasoft.cloud.module.aiagent.service.GlobalsService;
import com.chinasoft.cloud.module.aiagent.util.HttpsUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class ReportUtil {

    private static final String MSIP_HOST = "MSIP_HOST";

    @Resource
    private GlobalsService globalsService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private final Cache<String, String> cache =
            CacheBuilder.newBuilder().expireAfterWrite(10L, TimeUnit.SECONDS).build();

    @SneakyThrows
    private String getConfig(String key) {
        return globalsService.findByName(key);
//        return cache.get(key, () -> globalsService.findByName(key));
    }

    private String getMSIPHost() {
        return getConfig(MSIP_HOST);
    }

    public void saveOperationLog(OperationLog operationLog) {
        // 上报日志
        log.info("saveOperationLog operationLog: {}", operationLog);
        try {
            String url = getMSIPHost() + "/msip/oam/v1/log";
            String param = JsonUtils.toJsonString(operationLog);
            String resp = HttpsUtil.put(url, param);
            // msip的这个接口没有返回值的
            log.info("saveOperationLog url: {}, resp: {}", url, resp);
        } catch (Exception e) {
            log.error("saveOperationLog error: {}", e.getMessage());
        }
    }

    private String saveAlarmTemplate(AlarmTemplateRequest alarmTemplateRequest) {
        try {
            String url = getMSIPHost() + "/msip/alarm/v2/alarm-templates/internal";
            String param = JsonUtils.toJsonString(alarmTemplateRequest);
            String resp = HttpsUtil.post(url, param);
            MSIPResponse wsipResponse = JsonUtils.parseObject(resp, MSIPResponse.class);
            log.info("saveAlarmTemplate url: {}, resp: {}", url, wsipResponse);
            return wsipResponse.getResultCode();
        } catch (Exception e) {
            log.error("saveAlarmTemplate error: {}", e.getMessage());
        }
        return null;
    }

    private String saveActiveAlarms(ActiveAlarmsRequest activeAlarmsRequest) {
        try {
            String url = getMSIPHost() + "/msip/alarm/v2/active-alarms/internal";
            String param = JsonUtils.toJsonString(activeAlarmsRequest);
            String resp = HttpsUtil.post(url, param);
            MSIPResponse wsipResponse = JsonUtils.parseObject(resp, MSIPResponse.class);
            log.info("saveActiveAlarms url: {}, resp: {}", url, wsipResponse);
            return wsipResponse.getResultCode();
        } catch (Exception e) {
            log.error("saveActiveAlarms error: {}", e.getMessage());
        }
        return null;
    }

    private String clearAlarm(ActiveAlarmsClearRequest activeAlarmsClearRequest) {
        try {
            String url = getMSIPHost() + "/msip/alarm/v2/active-alarms/internal/clear";
            String param = JsonUtils.toJsonString(activeAlarmsClearRequest);
            String resp = HttpsUtil.post(url, param);
            MSIPResponse wsipResponse = JsonUtils.parseObject(resp, MSIPResponse.class);
            log.info("clearAlarm url: {}, resp: {}", url, wsipResponse);
            return wsipResponse.getResultCode();
        } catch (Exception e) {
            log.error("clearAlarm error: {}", e.getMessage());
        }
        return null;
    }

    public void saveAlarm2MSIP(AlarmTemplate alarmTemplate, Object... params) {
        ActiveAlarmsRequest activeAlarmsRequest = new ActiveAlarmsRequest(alarmTemplate, params);

        saveAlarm2MSIP(activeAlarmsRequest);
    }

    private void saveAlarm2MSIP(ActiveAlarmsRequest activeAlarmsRequest) {
        try {
            boolean hasAlertActiveRecord = hasAlertActiveRecord(activeAlarmsRequest.getAlarmId());
            if (hasAlertActiveRecord) {
                // 如果存在了活动告警，则不重复发送
                log.info("hasAlertActiveRecord true, alarmId: {}", activeAlarmsRequest.getAlarmId());
                return;
            }
            doSaveAlarm(activeAlarmsRequest);
        } catch (Exception e) {
            log.error("saveAlarm2MSIP error: {}", e.getMessage());
        }
    }

    private void doSaveAlarm(ActiveAlarmsRequest activeAlarmsRequest) {
        String alarmId = activeAlarmsRequest.getAlarmId();
        String resultCode = saveActiveAlarms(activeAlarmsRequest);
        MSIPResponseEnum msipResponseEnum = MSIPResponseEnum.matchCode(resultCode);
        switch (msipResponseEnum) {
            case COMMON_SUCCESS:
                // 成功则保存记录到redis
                saveAlertActiveRecord2Redis(alarmId);
                break;
            case COMMON_FAIL:
                    log.error("doSaveAlarm is failed, activeAlarmsRequest: {}", activeAlarmsRequest);
                break;
            case FAILED:
                log.warn("alarm template is not exits, ready to save alarm template: {}", activeAlarmsRequest);
                AlarmTemplateZhEnum alarmTemplateZhEnum = AlarmTemplateZhEnum.matchAlarmId(alarmId);
                AlarmTemplateEnEnum alarmTemplateEnEnum = AlarmTemplateEnEnum.matchAlarmId(alarmId);

                if (Objects.nonNull(alarmTemplateZhEnum)) {
                    // 中文模板
                    AlarmTemplateRequest alarmTemplateZhRequest = new AlarmTemplateRequest(alarmTemplateZhEnum);
                    saveAlarmTemplate(alarmTemplateZhRequest);
                }
                if (Objects.nonNull(alarmTemplateEnEnum)) {
                    // 英文模板
                    AlarmTemplateRequest alarmTemplateEnRequest = new AlarmTemplateRequest(alarmTemplateEnEnum, "en");
                    saveAlarmTemplate(alarmTemplateEnRequest);
                }
                // 重新上报活动告警
                saveActiveAlarms(activeAlarmsRequest);
                // 存入redis
                saveAlertActiveRecord2Redis(alarmId);
                break;
            default:
                log.info("doSaveAlarm do nothing: {}", activeAlarmsRequest);
                break;
        }
    }


    public void clearAlarm2MSIP(AlarmTemplate alarmTemplate) {
        clearAlarm2MSIP(alarmTemplate.getAlarmId());
    }

    private void clearAlarm2MSIP(String alarmId) {
        if (!hasAlertActiveRecord(alarmId)) {
            return;
        }
        ActiveAlarmsClearRequest request = new ActiveAlarmsClearRequest(alarmId);
        clearAlarm(request);
        deleteAlertActiveRecord2Redis(alarmId);
    }

    private String getAlertActiveRecordKey(String alarmId) {
        return String.format(MSIPConstant.ALERT_ACTIVE_RECORD_KEY, alarmId);
    }

    /**
     * 判断是否已经存在活动告警
     * @param alarmId
     * @return true: 是，false: 否
     */
    private boolean hasAlertActiveRecord(String alarmId) {
        String record = stringRedisTemplate.opsForValue().get(getAlertActiveRecordKey(alarmId));
        return Objects.nonNull(record);
    }

    private void saveAlertActiveRecord2Redis(String alarmId) {
        String key = getAlertActiveRecordKey(alarmId);
        stringRedisTemplate.opsForValue().set(key, alarmId);
    }

    private void deleteAlertActiveRecord2Redis(String alarmId) {
        stringRedisTemplate.delete(getAlertActiveRecordKey(alarmId));
    }
}
