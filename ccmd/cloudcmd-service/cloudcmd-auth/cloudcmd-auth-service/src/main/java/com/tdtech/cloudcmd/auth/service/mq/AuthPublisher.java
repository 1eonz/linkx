package com.tdtech.cloudcmd.auth.service.mq;

import com.alibaba.fastjson.JSONObject;
import com.tdtech.cloudcmd.auth.constant.OAuthConstant;
import com.tdtech.cloudcmd.bean.CagentMqFrame;
import com.tdtech.cloudcmd.bean.MsgBody;
import com.tdtech.cloudcmd.msip.enums.AlarmTemplate;
import com.tdtech.cloudcmd.msip.util.ReportUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

import static com.tdtech.cloudcmd.auth.constant.OAuthConstant.AUTH_SUB_SYSTEM;
import static com.tdtech.cloudcmd.auth.constant.OAuthConstant.MODEL_NAME_AUTH;
import static com.tdtech.cloudcmd.auth.constant.OAuthConstant.MUNICAST_NOTIFY_TYPE;
import static com.tdtech.cloudcmd.auth.constant.OAuthConstant.NOTIFY_OFFLINE;
import static com.tdtech.cloudcmd.auth.constant.OAuthConstant.NOTIFY_TOKEN_REFRESH;
import static com.tdtech.cloudcmd.auth.constant.OAuthConstant.TYPE_BIZ_TOKEN_REFRESH;

@Slf4j
@Component
public class AuthPublisher {
    @Resource
    private StreamBridge streamBridge;

    @Resource
    private ReportUtil reportUtil;

    /**
     * 发布踢出消息
     *
     * @param data
     * @return
     */
    public boolean publishKickOutMessage(Long userId, Object data) {
        CagentMqFrame frame = this.kickoutFrame(userId, data);
        log.info("publish auth token kickout data is:{}", frame);
        Message<CagentMqFrame> build = org.springframework.messaging.support.MessageBuilder.withPayload(frame).build();
        return streamBridge.send("cloudcmd-cagent", build);
    }

    /**
     * 发出token超时通知,
     *
     * @param token
     * @param data
     * @return
     */
    public boolean publishOfflineMessage(String token, Object data) {
        CagentMqFrame frame = this.assembleCagentFrame(token, MUNICAST_NOTIFY_TYPE, NOTIFY_OFFLINE, data);
        log.debug("publish auth token offline data is:{}", frame);
        Message<CagentMqFrame> build = org.springframework.messaging.support.MessageBuilder.withPayload(frame).build();
        return streamBridge.send("cloudcmd-cagent", build);
    }

    /**
     * 发出token即将超时通知
     *
     * @param token
     * @param data
     * @return
     */
    public boolean publishTokenWillTimeoutMessage(String token, String notifyType, Object data) {
        CagentMqFrame frame = this.assembleCagentFrame(token, MUNICAST_NOTIFY_TYPE, notifyType, data);
        log.debug("publish auth token will timeout data is:{}", frame);
        Message<CagentMqFrame> build = org.springframework.messaging.support.MessageBuilder.withPayload(frame).build();
        return streamBridge.send("cloudcmd-cagent", build);
    }

    /**
     * 发送刷新token接口
     *
     * @param data
     * @return
     */
    public boolean publishRefreshTokenMessage(String oldToken, Object data) {
        JSONObject body = new JSONObject();
        body.put("jsonrpc", "2.0");
        body.put("method", NOTIFY_TOKEN_REFRESH);
        body.put("params", data);
        JSONObject message = new JSONObject();
        message.put("type", TYPE_BIZ_TOKEN_REFRESH);
        message.put("subsystem", AUTH_SUB_SYSTEM);
        message.put("tokenkey", oldToken);
        message.put("body", body);
        // log.info("publish RefreshTokenMessage will timeout data is:{}", message);
        Message<JSONObject> build = org.springframework.messaging.support.MessageBuilder.withPayload(message).build();
        return streamBridge.send("cloudcmd-cagent", build);
    }

    public boolean publishAlarmMessage(AlarmTemplate alarmTemplate, Object params) {
        log.info("publishAlarmMessage alarmTemplate: {}, params: {}", alarmTemplate, params);
        reportUtil.saveAlarm2MSIP(alarmTemplate, params);
        return true;
    }

    public boolean publishAlarmClearMessage(AlarmTemplate alarmTemplate) {
        log.info("publishAlarmClearMessage alarmId:{}", alarmTemplate.getAlarmId());
        reportUtil.clearAlarm2MSIP(alarmTemplate);
        return true;
    }

    private CagentMqFrame kickoutFrame(Long userId, Object data) {
        return new CagentMqFrame().new Builder().typeSubSystemMessage(AUTH_SUB_SYSTEM).typeKickOut()
            .body(MODEL_NAME_AUTH, OAuthConstant.NOTIFY_KICKOUT, data).unicast().userIds(List.of(userId + "")).build()
            .build();
    }

    private CagentMqFrame assembleCagentFrame(String tokenKey, Short type, String notifyType, Object data) {
        CagentMqFrame frame = new CagentMqFrame();
        frame.setSubsystem(AUTH_SUB_SYSTEM);
        frame.setType(type);
        frame.setTokenkey(tokenKey);
        MsgBody body = new MsgBody();
        body.setModule(MODEL_NAME_AUTH);
        body.setNotifyType(notifyType);
        body.setData(data);
        frame.setBody(body);
        return frame;
    }

}
