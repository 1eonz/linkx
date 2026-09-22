package com.tdtech.linkx.encryptor.enums;

import lombok.Getter;

/**
 * 加密模式枚举
 */
@Getter
public enum EncryptModeEnum {
    /**
     * 加密
     */
    ENCRYPT("encrypt", "加密"),

    /**
     * 解密
     */
    DECRYPT("decrypt", "解密");

    private final String code;
    private final String desc;

    EncryptModeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static EncryptModeEnum fromCode(String code) {
        for (EncryptModeEnum mode : values()) {
            if (mode.getCode().equalsIgnoreCase(code)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unsupported mode: " + code);
    }
}
