package com.tdtech.cloudcmd.im.jingxin.api.entity.group;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 开放接口建群类型
 */
@AllArgsConstructor
@Getter
public enum GroupCreateTypeEnum {
    NORMAL(1, "普通群组"),
    COLLABORATION(2, "协同群组");

    private final int value;
    private final String desc;

    public static GroupCreateTypeEnum of(Integer type) {
        if (type == null) {
            return null;
        }
        for (GroupCreateTypeEnum t : values()) {
            if (t.value == type) {
                return t;
            }
        }
        return null;
    }
}