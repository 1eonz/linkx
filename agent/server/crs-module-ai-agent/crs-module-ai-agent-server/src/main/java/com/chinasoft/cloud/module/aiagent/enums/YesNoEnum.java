package com.chinasoft.cloud.module.aiagent.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@Getter
@AllArgsConstructor
public enum YesNoEnum {

    YES(1, "是"),
    NO(0, "否");

    private final Integer type;
    private final String name;

    public static YesNoEnum nameOf(String name) {
        return nameOf(name, YES);
    }

    /**
     * 按 Excel 文本名解析，空值或非法值返回 defaultValue
     */
    public static YesNoEnum nameOf(String name, YesNoEnum defaultValue) {
        if (StringUtils.isBlank(name)) {
            return defaultValue;
        }
        for (YesNoEnum e : values()) {
            if (e.name.equals(name)) {
                return e;
            }
        }
        return defaultValue;
    }

    /**
     * 判断文本是否是合法的枚举名，空值视为合法（非必填，由 handler 用默认值兜底）
     */
    public static boolean isValidName(String name) {
        if (StringUtils.isBlank(name)) {
            return true;
        }
        for (YesNoEnum e : values()) {
            if (e.name.equals(name)) {
                return true;
            }
        }
        return false;
    }
}