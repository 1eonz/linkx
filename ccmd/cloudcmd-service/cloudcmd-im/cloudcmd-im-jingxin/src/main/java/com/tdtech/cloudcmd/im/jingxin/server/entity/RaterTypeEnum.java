package com.tdtech.cloudcmd.im.jingxin.server.entity;

import lombok.Getter;

/**
 * 评价者类型枚举
 */
@Getter
public enum RaterTypeEnum {

    /**
     * 群主
     */
    OWNER(1, "群主"),

    /**
     * 群成员
     */
    MEMBER(2, "群成员");

    /**
     * 类型编码
     */
    private final Integer code;

    /**
     * 类型描述
     */
    private final String description;

    RaterTypeEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据编码获取枚举实例
     *
     * @param code 类型编码
     * @return 对应的枚举实例，若不存在则返回null
     */
    public static RaterTypeEnum fromCode(Integer code) {
        for (RaterTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
