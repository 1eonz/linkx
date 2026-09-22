package com.tdtech.cloudcmd.im.openapi.controller.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 应用列表查询类型
 */
@AllArgsConstructor
@Getter
public enum AppListQueryTypeEnum {
    ALL(0, "全部应用"),
    MY(1, "我的常用应用");

    private final int value;
    private final String desc;

    public static AppListQueryTypeEnum of(Integer type) {
        if (type == null) {
            return null;
        }
        for (AppListQueryTypeEnum t : values()) {
            if (t.value == type) {
                return t;
            }
        }
        return null;
    }
}