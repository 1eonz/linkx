package com.tdtech.linkx.encryptor.enums;

import lombok.Getter;

/**
 * 流式模式枚举
 */
@Getter
public enum StreamModeEnum {
    /**
     * 流式加密
     */
    STREAM("stream", "流式加密"),

    /**
     * 非流式加密（一次性加密）
     */
    NON_STREAM("non-stream", "非流式加密");

    private final String code;
    private final String desc;

    StreamModeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static StreamModeEnum fromCode(String code) {
        for (StreamModeEnum mode : values()) {
            if (mode.getCode().equalsIgnoreCase(code)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unsupported stream mode: " + code);
    }
}
