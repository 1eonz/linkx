package com.chinasoft.cloud.module.aiagent.dal.dataobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * 提问类型
 */
@Getter
@AllArgsConstructor
public enum AgentRecordAskType {
    AI_ASSISTANT(1, "ai助手提问"),
    GROUP_AT(2, "群@群助手提问"),
    SINGLE_CHAT(3, "单聊智能体"),
    GROUP_NORMAL(4, "群聊普通消息（非@）");

    private final int value;
    private final String desc;

    /**
     * 有效问答类型值列表（AI助手提问、群@消息、单聊透传），不含群聊普通消息
     */
    public static List<Integer> getValidValues() {
        return List.of(AI_ASSISTANT.getValue(), GROUP_AT.getValue(), SINGLE_CHAT.getValue());
    }

    public static boolean isValid(Integer value) {
        if (value == null) {
            return false;
        }
        for (AgentRecordAskType type : values()) {
            if (type.value == value) {
                return true;
            }
        }
        return false;
    }
}