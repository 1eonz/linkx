package com.tdtech.cloudcmd.linkx.dashboard.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 人员核查结果枚举（导出转中文用）。
 */
@Getter
public enum CheckResultEnum {

    SUCCESS(1, "成功");

    private final int code;

    private final String name;

    CheckResultEnum(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static final Map<Integer, String> CODE_TO_NAME;

    static {
        CODE_TO_NAME = Arrays.stream(values())
                .collect(Collectors.toMap(CheckResultEnum::getCode, CheckResultEnum::getName));
    }
}