package com.tdtech.cloudcmd.linkx.dashboard.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 建群方式枚举（导出转中文用）。
 */
@Getter
public enum GroupSubTypeEnum {

    OTHER(0, "其他"),
    ONE_KEY(1, "一键建群"),
    POLICE(2, "警单建群"),
    CUSTOM(3, "自定义建群"),
    FUNCTIONAL(4, "职能建群"),
    DISPATCH(5, "一键调度");

    private final int code;

    private final String name;

    GroupSubTypeEnum(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static final Map<Integer, String> CODE_TO_NAME;

    static {
        CODE_TO_NAME = Arrays.stream(values())
                .collect(Collectors.toMap(GroupSubTypeEnum::getCode, GroupSubTypeEnum::getName));
    }
}