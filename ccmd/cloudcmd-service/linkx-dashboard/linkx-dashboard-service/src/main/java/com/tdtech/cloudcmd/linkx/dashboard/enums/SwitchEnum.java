package com.tdtech.cloudcmd.linkx.dashboard.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 协同岗上岗/下岗枚举（导出转中文用）。
 */
@Getter
public enum SwitchEnum {

    ON(0, "上岗"),
    OFF(1, "下岗");

    private final int code;

    private final String name;

    SwitchEnum(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static final Map<Integer, String> CODE_TO_NAME;

    static {
        CODE_TO_NAME = Arrays.stream(values())
                .collect(Collectors.toMap(SwitchEnum::getCode, SwitchEnum::getName));
    }
}