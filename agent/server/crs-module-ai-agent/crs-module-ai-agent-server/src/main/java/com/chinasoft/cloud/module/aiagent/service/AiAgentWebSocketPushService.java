package com.chinasoft.cloud.module.aiagent.service;

import com.alibaba.fastjson.JSONObject;
import com.chinasoft.cloud.framework.common.enums.UserTypeEnum;
import com.chinasoft.cloud.framework.common.util.json.JsonUtils;
import com.chinasoft.cloud.module.aiagent.dal.dataobj.AgentRecord;
import com.chinasoft.cloud.module.infra.api.websocket.WebSocketSenderApi;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
public class AiAgentWebSocketPushService {

    private static final String REDIS_APPROVAL_PUSH_TARGET_KEY_PREFIX = "crs:ai-agent:approval:push-target:";
    private static final String MESSAGE_TYPE_APPROVAL_CHANGED = "ai-agent-approval-changed";

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private WebSocketSenderApi webSocketSenderApi;
    @Resource(name = "askPool")
    private ThreadPoolTaskExecutor executorService;

    /* 保存审批回调需要推送的 WebSocket 会话。 */
    public void putPushTarget(Long recordId, String wsSessionId) {
        if (recordId == null || StringUtils.isBlank(wsSessionId)) {
            return;
        }
        stringRedisTemplate.opsForValue().set(REDIS_APPROVAL_PUSH_TARGET_KEY_PREFIX + recordId,
            wsSessionId, Duration.ofHours(24L * 7));
    }

    public void sendApprovalCreated(AgentRecord record, String approvalStatus, Integer approveResult, String approveNo,
                                    String approveDetailUrl, String toLeaderUrl, String approveUser) {
        Map<String, Object> message = buildApprovalMessage(record.getId(), approvalStatus, approveResult, approveNo,
            approveDetailUrl, approveUser);
        message.put("event", "approval_created");
        message.put("closeApprovalPage", true);
        message.put("toLeaderUrl", toLeaderUrl);
        sendWebSocketMessage(record.getIdentityCardNumber(), record.getId(), MESSAGE_TYPE_APPROVAL_CHANGED, message);
    }

    public void sendApprovalChanged(AgentRecord record, String approvalStatus, Integer approveResult, String approveNo,
        String approveDetailUrl, String approveUser) {
        Map<String, Object> message = buildApprovalMessage(record.getId(), approvalStatus, approveResult, approveNo,
            approveDetailUrl, approveUser);
        message.put("event", "approval_changed");
        sendWebSocketMessage(record.getIdentityCardNumber(), record.getId(), MESSAGE_TYPE_APPROVAL_CHANGED, message);
    }

    private Map<String, Object> buildApprovalMessage(Long recordId, String approvalStatus, Integer approveResult,
        String approveNo, String approveDetailUrl, String approveUser) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("recordId", String.valueOf(recordId));
        message.put("approvalStatus", approvalStatus);
        message.put("approveResult", approveResult);
        message.put("approveNo", approveNo);
        message.put("approveDetailUrl", approveDetailUrl);
        message.put("approveUser", approveUser);
        return message;
    }

    private String getPushTarget(Long recordId) {
        if (recordId == null) {
            return null;
        }
        return stringRedisTemplate.opsForValue().get(REDIS_APPROVAL_PUSH_TARGET_KEY_PREFIX + recordId);
    }

    private void sendWebSocketMessage(String idCard, Long recordId, String messageType, Map<String, Object> message) {
        executorService.execute(() -> doSendWebSocketMessage(idCard, recordId, messageType, message));
    }

    private void doSendWebSocketMessage(String idCard, Long recordId, String messageType, Map<String, Object> message) {
        try {
            log.info("send ai-agent websocket push, messageType:{} message:{}", messageType, JSONObject.toJSONString(message));
            webSocketSenderApi.sendByIdCard(idCard, messageType, JSONObject.toJSONString(message));
        } catch (Exception ex) {
            log.warn("send ai-agent websocket push failed, recordId:{} messageType:{}", recordId, messageType, ex);
        }
    }
}
