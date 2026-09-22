package com.tdtech.cloudcmd.linkx.dashboard.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 建群入口枚举（导出转中文用）。
 */
@Getter
public enum GroupCreateTypeEnum {

    MANUAL(0, "手工建群"),
    OPEN_API(1, "开放接口建群");

    private final int code;

    private final String name;

    GroupCreateTypeEnum(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static final Map<Integer, String> CODE_TO_NAME;

    static {
        CODE_TO_NAME = Arrays.stream(values())
                .collect(Collectors.toMap(GroupCreateTypeEnum::getCode, GroupCreateTypeEnum::getName));
    }
}