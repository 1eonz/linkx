package com.tdtech.cloudcmd.license.entity;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum StatusEnum implements Serializable {
    // 未激活 License未激活，请重新导入
    NOT_ACTIVATED(0),
    // 激活
    ACTIVATED_NORMAL(1),
    // 即将过期
    ACTIVATED_TO_BE_EXPIRED(2),
    // 已过期 License已过期，请重新导入
    ACTIVATED_EXPIRED(3),
    // 失效可试用
    DISABLED_IN_TRIAL(4),
    // 失效 License已过期，请重新导入
    DISABLED(5);

    private final int code;
    private static final long serialVersionUID = 1L;

    StatusEnum(int code) {
        this.code = code;
    }

    @JsonCreator
    public static StatusEnum codeOf(int code) {
        for (StatusEnum value : StatusEnum.values()) {
            if (value.code == code) {
                return value;
            }
        }
        return null;
    }

    @JsonValue
    public int code() {
        return this.code;
    }
}
