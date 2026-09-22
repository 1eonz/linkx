package com.tdtech.cloudcmd.linkx.dashboard.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 群组类型枚举（导出转中文用）。
 */
@Getter
public enum GroupTypeEnum {

    NORMAL(1, "普通群组"),
    COOP(2, "协同群组");

    private final int code;

    private final String name;

    GroupTypeEnum(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static final Map<Integer, String> CODE_TO_NAME;

    static {
        CODE_TO_NAME = Arrays.stream(values())
                .collect(Collectors.toMap(GroupTypeEnum::getCode, GroupTypeEnum::getName));
    }
}