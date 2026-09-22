package com.chinasoft.cloud.module.aiagent.service;

import com.alibaba.fastjson.JSON;
import com.chinasoft.cloud.framework.common.util.json.JsonUtils;
import com.chinasoft.cloud.module.aiagent.dal.dataobj.AgentRecord;
import com.chinasoft.cloud.module.aiagent.dal.mysql.AgentRecordMapper;
import jakarta.annotation.Resource;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class AiAgentReplyStateService {

    private static final String REDIS_REPLY_STATE_KEY_PREFIX = "crs:ai-agent:reply-state:";

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private AgentRecordMapper agentRecordMapper;

    public void putReplyState(Long id, Integer replyPosition) {
        if (id == null) {
            return;
        }
        ReplyState replyState = getReplyState(id);
        if (replyState == null) {
            replyState = new ReplyState();
        }
        replyState.setReplyPosition(replyPosition == null ? 0 : replyPosition);
        if (replyState.getReplyPaused() == null) {
            replyState.setReplyPaused(Boolean.FALSE);
        }
        saveReplyState(id, replyState);
        updateRecordReplyState(id, replyState);
    }

    public ReplyState updateReadState(Long id, Integer replyPosition, Boolean replyPaused) {
        if (id == null) {
            return null;
        }
        // 只维护前端主动提交的读取位置和暂停标记。
        ReplyState replyState = getReplyState(id);
        if (replyState == null) {
            replyState = new ReplyState();
        }
        if (replyPosition != null) {
            replyState.setReplyPosition(Math.max(replyPosition, -1));
        }
        if (replyPaused != null) {
            replyState.setReplyPaused(replyPaused);
        }
        saveReplyState(id, replyState);
        updateRecordReplyState(id, replyState);
        return replyState;
    }

    private void saveReplyState(Long id, ReplyState replyState) {
        stringRedisTemplate.opsForValue().set(REDIS_REPLY_STATE_KEY_PREFIX + id,
            JsonUtils.toJsonString(replyState), Duration.ofMinutes(30L));
    }

    private void updateRecordReplyState(Long id, ReplyState replyState) {
        if (id == null || replyState == null) {
            return;
        }
        AgentRecord record = new AgentRecord();
        record.setId(id);
        record.setReplyPosition(replyState.getReplyPosition());
        record.setReplyPaused(replyState.getReplyPaused());
        agentRecordMapper.updateById(record);
    }

    public ReplyState getReplyState(Long id) {
        String cacheValue = stringRedisTemplate.opsForValue().get(REDIS_REPLY_STATE_KEY_PREFIX + id);
        if (StringUtils.isBlank(cacheValue)) {
            return null;
        }
        return JSON.parseObject(cacheValue, ReplyState.class);
    }

    public ReplyState resolveReplyState(Long id, String reply, boolean canReturnReplyDirectly,
        boolean hasFinalResponse) {
        ReplyState replyState = getReplyState(id);
        if (replyState != null) {
            return replyState;
        }
        AgentRecord record = agentRecordMapper.selectById(id);
        if (record != null && (record.getReplyPosition() != null || record.getReplyPaused() != null)) {
            replyState = new ReplyState();
            replyState.setReplyPosition(record.getReplyPosition());
            replyState.setReplyPaused(record.getReplyPaused());
            return replyState;
        }
        replyState = new ReplyState();
        if (!canReturnReplyDirectly) {
            replyState.setReplyPosition(0);
            replyState.setReplyPaused(Boolean.FALSE);
            return replyState;
        }
        if (hasFinalResponse) {
            replyState.setReplyPosition(-1);
            replyState.setReplyPaused(Boolean.FALSE);
            return replyState;
        }
        replyState.setReplyPosition(StringUtils.defaultString(reply).length());
        replyState.setReplyPaused(Boolean.FALSE);
        return replyState;
    }

    public Map<Long, ReplyState> loadReplyStateMap(List<AgentRecord> records) {
        List<Long> ids = records == null ? Collections.emptyList()
            : records.stream().map(AgentRecord::getId).filter(Objects::nonNull).toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<String> keys = ids.stream().map(id -> REDIS_REPLY_STATE_KEY_PREFIX + id).toList();
        List<String> values = stringRedisTemplate.opsForValue().multiGet(keys);
        if (values == null || values.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, ReplyState> replyStateMap = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) {
            String value = values.get(i);
            if (StringUtils.isBlank(value)) {
                continue;
            }
            ReplyState replyState = JSON.parseObject(value, ReplyState.class);
            replyStateMap.put(ids.get(i), replyState);
        }
        return replyStateMap;
    }

    public Integer resolveReplyPosition(AgentRecord record, ReplyState replyState) {
        if (replyState != null && replyState.getReplyPosition() != null) {
            return replyState.getReplyPosition();
        }
        if (record != null && record.getReplyPosition() != null) {
            return record.getReplyPosition();
        }
        return record != null && StringUtils.isNotBlank(record.getResponseContent()) ? -1 : 0;
    }

    public Boolean resolveReplyPaused(AgentRecord record, ReplyState replyState) {
        if (replyState != null && replyState.getReplyPaused() != null) {
            return Boolean.TRUE.equals(replyState.getReplyPaused());
        }
        return record != null && Boolean.TRUE.equals(record.getReplyPaused());
    }

    @Data
    public static class ReplyState {

        /**
         * 前端当前读取到的回答字符位置，-1 表示全部读取完成。
         */
        private Integer replyPosition;

        /**
         * 前端是否暂停回答。
         */
        private Boolean replyPaused;
    }
}
