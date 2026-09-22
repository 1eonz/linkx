package com.tdtech.cloudcmd.msip.util;

import com.alibaba.fastjson.JSONObject;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.tdtech.cloudcmd.base.api.service.GlobalsRpcService;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.msip.constant.MSIPConstant;
import com.tdtech.cloudcmd.msip.entity.*;
import com.tdtech.cloudcmd.msip.enums.*;
import com.tdtech.cloudcmd.redis.RedisLockFactory;
import com.tdtech.cloudcmd.redis.RedisUtil;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.json.JsonUtil;
import com.tdtech.cloudcmd.web.utils.HttpClient;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class ReportUtil {

    private static final String MSIP_HOST = "MSIP_HOST";

    /**
     * 查询活跃的告警信息接口地址
     */
    private static final String ALARM_INFO_QUERY_URI = "/msip/alarm/v1/current-alarms";

    private final Cache<String, String> cache =
            CacheBuilder.newBuilder().expireAfterWrite(10L, TimeUnit.SECONDS).build();

    @DubboReference
    private GlobalsRpcService globalsRpcService;

    @Resource
    private HttpClient httpClient;

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private RedisLockFactory redisLockFactory;


    private String getMSIPHost() {
        return getConfig(MSIP_HOST);
    }

    @SneakyThrows
    private String getConfig(String key) {
        return globalsRpcService.getGlobalsValueByName(key);
//        return cache.get(key, () -> globalsRpcService.getGlobalsValueByName(key));
    }

    public void saveOperationLog(OperationLog param) {
        try {
            // 上报日志
            URI uri = new URI(getMSIPHost() + "/msip/oam/v1/log");
            Object resp = httpClient.putJson(uri, null, param, String.class);
            log.info("saveOperationLog uri: {}, resp: {}", uri, resp);
        } catch (Exception e) {
            log.info("saveOperationLog error: {}", e.getMessage());
        }
    }

    /**
     * 上报告警模板
     * @param alarmTemplateRequest
     * @return
     */
    private String saveAlarmTemplate(AlarmTemplateRequest alarmTemplateRequest) {
        try {
            URI uri = new URI(getMSIPHost() + "/msip/alarm/v2/alarm-templates/internal");
            MSIPResponse resp = httpClient.postJson(uri, null, alarmTemplateRequest, MSIPResponse.class);
            log.info("saveAlarmTemplate param: {}, uri: {}, resp: {}", alarmTemplateRequest, uri, resp);
            return resp.getResultCode();
        } catch (Exception e) {
            log.info("saveAlarmTemplate error: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 上报活动告警
     * @param activeAlarmsRequest
     */
    private String saveActiveAlarms(ActiveAlarmsRequest activeAlarmsRequest) {
        try {
            URI uri = new URI(getMSIPHost() + "/msip/alarm/v2/active-alarms/internal");
            MSIPResponse resp = httpClient.postJson(uri, null, activeAlarmsRequest, MSIPResponse.class);
            log.info("saveActiveAlarms param: {}, uri: {}, resp: {}", activeAlarmsRequest, uri, resp);
            return resp.getResultCode();
        } catch (Exception e) {
            log.error("saveActiveAlarms error: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 告警恢复（即删除活动告警）
     * @param activeAlarmsClearRequest
     */
    private void clearAlarm(ActiveAlarmsClearRequest activeAlarmsClearRequest) {
        try {
            URI uri = new URI(getMSIPHost() + "/msip/alarm/v2/active-alarms/internal/clear");
            MSIPResponse resp = httpClient.postJson(uri, null, activeAlarmsClearRequest, MSIPResponse.class);
            log.info("clearAlarm param: {}, uri: {}, resp: {}", activeAlarmsClearRequest, uri, resp);
        } catch (Exception e) {
            log.error("clearAlarm error: {}", e.getMessage());
        }
    }

    public void saveAlarm2MSIP(AlarmTemplate alarmTemplate, Object... params) {
        ActiveAlarmsRequest activeAlarmsRequest = new ActiveAlarmsRequest(alarmTemplate, params);

        saveAlarm2MSIP(activeAlarmsRequest);
    }

    private void saveAlarm2MSIP(ActiveAlarmsRequest activeAlarmsRequest) {
        String lockKey = String.format(MSIPConstant.ALERT_LOCK_KEY, activeAlarmsRequest.getAlarmId());
        var redisLock = redisLockFactory.newRedisLock(lockKey, Duration.ofSeconds(20L));
        if (!redisLock.tryLock(10L, TimeUnit.SECONDS)) {
            log.error("saveAlarm2MSIP lock timeout");
            return;
        }
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
        } finally {
            redisLock.unlock();
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
        String lockKey = String.format(MSIPConstant.CLEAR_ALERT_LOCK_KEY, alarmId);
        var redisLock = redisLockFactory.newRedisLock(lockKey, Duration.ofSeconds(20L));
        if (!redisLock.tryLock(10L, TimeUnit.SECONDS)) {
            log.error("clearAlarm2MSIP lock timeout");
            return;
        }
        try {
            if (!hasAlertActiveRecord(alarmId)) {
                return;
            }
            ActiveAlarmsClearRequest request = new ActiveAlarmsClearRequest(alarmId);
            clearAlarm(request);
            deleteAlertActiveRecord2Redis(alarmId);
        } catch (Exception e) {
            log.error("clearAlarm2MSIP error: {}", e.getMessage());
        } finally {
            redisLock.unlock();
        }
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
        Long count = redisUtil.exists(getAlertActiveRecordKey(alarmId));
        return Objects.nonNull(count) && count.longValue() > 0;
    }

    private void saveAlertActiveRecord2Redis(String alarmId) {
        redisUtil.set(getAlertActiveRecordKey(alarmId), alarmId);
    }

    private void deleteAlertActiveRecord2Redis(String alarmId) {
        redisUtil.del(getAlertActiveRecordKey(alarmId));
    }


    public void saveAlertActiveRecord2Redis(List<String> alarmIds) {
        if (CollectionUtils.isEmpty(alarmIds)) {
            return;
        }
        Map<String, String> alarmMap = new HashMap<>();
        alarmIds.forEach(alarmId -> alarmMap.put(getAlertActiveRecordKey(alarmId), alarmId));
        redisUtil.mSet(alarmMap);
    }

    /**
     * 查询当前系统活跃的活动告警
     *
     * @param queryReq 查询条件
     */
    public List<LiveAlarmsResp.AlarmsInfo> queryActiveAlarms(QueryLiveAlarmsReq queryReq) {
        try {
            // 查询
            URI uri = HttpClient.buildUri(getMSIPHost() + ALARM_INFO_QUERY_URI,
                    JsonUtil.parseJson(JSONObject.toJSONString(queryReq)));
            MSIPResponse resp = httpClient.getJson(uri, null, MSIPResponse.class);
            log.info("queryLiveAlarms param: {}, uri: {}, resp: {}", queryReq, uri, resp);
            // 响应体解析成所需的数据
            MSIPResponseEnum msipResponseEnum = MSIPResponseEnum.matchCode(resp.getResultCode());
            if (!MSIPResponseEnum.COMMON_SUCCESS.equals(msipResponseEnum) || Objects.isNull(resp.getDatas())) {
                return Collections.emptyList();
            }
            LiveAlarmsResp liveAlarmsResp = JSONObject.parseObject(JSONObject.toJSONString(resp.getDatas()), LiveAlarmsResp.class);
            if (CollectionUtils.isEmpty(liveAlarmsResp.getContent())) {
                return Collections.emptyList();
            }
            return liveAlarmsResp.getContent();
        } catch (Exception e) {
            log.error("queryLiveAlarms error,", e);
        }
        return Collections.emptyList();
    }

    /**
     * 便捷方法：记录操作日志（自动填充操作人、状态）
     *
     * @param type      操作类型枚举
     * @param operation 操作描述
     * @param status    操作状态（MSIPConstant.OPERATION_SUCCESS / OPERATION_FAILURE）
     */
    public void saveOperationLog(OperationTypeEnum type, String operation, Integer status) {
        try {
            OperationLog operationLog = new OperationLog(type);
            operationLog.setOperation(operation);
            operationLog.setStatus(status);
            UserInfo userInfo = SecurityUtils.getUser();
            if (Objects.nonNull(userInfo)) {
                operationLog.setOperator(userInfo.getUserName());
            }
            saveOperationLog(operationLog);
        } catch (Exception ex) {
            log.error("saveOperationLog error: {}", ex.getMessage());
        }
    }

    /**
     * 便捷方法：记录操作失败日志（默认 FAILURE 状态）
     *
     * @param type      操作类型枚举
     * @param operation 操作描述
     */
    public void saveOperationLog(OperationTypeEnum type, String operation) {
        saveOperationLog(type, operation, MSIPConstant.OPERATION_FAILURE);
    }


}
