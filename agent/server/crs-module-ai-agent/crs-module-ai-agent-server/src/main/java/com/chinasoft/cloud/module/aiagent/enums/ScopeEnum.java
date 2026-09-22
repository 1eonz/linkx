package com.chinasoft.cloud.module.aiagent.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/**
 * 智能体作用域
 */
@Getter
@AllArgsConstructor
public enum ScopeEnum {

    ALL(0, "所有"),
    AI_ASSISTANT(1, "仅AI智能体问答"),
    IM(2, "仅IM");

    private final Integer type;
    private final String name;

    /**
     * 按 Excel 文本名解析，空值默认 AI_ASSISTANT(1)
     */
    public static ScopeEnum nameOf(String name) {
        if (StringUtils.isBlank(name)) {
            return AI_ASSISTANT;
        }
        for (ScopeEnum e : values()) {
            if (e.name.equals(name)) {
                return e;
            }
        }
        return AI_ASSISTANT;
    }

    /**
     * 判断文本是否是合法的枚举名，空值视为合法（非必填，由 handler 用默认值兜底）
     */
    public static boolean isValidName(String name) {
        if (StringUtils.isBlank(name)) {
            return true;
        }
        for (ScopeEnum e : values()) {
            if (e.name.equals(name)) {
                return true;
            }
        }
        return false;
    }
}