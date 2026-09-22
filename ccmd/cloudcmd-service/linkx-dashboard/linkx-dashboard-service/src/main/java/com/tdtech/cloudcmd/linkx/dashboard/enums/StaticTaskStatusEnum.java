package com.tdtech.cloudcmd.linkx.dashboard.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 协同任务状态枚举（导出转中文用）。
 */
@Getter
public enum StaticTaskStatusEnum {

    ASSIGN(-1, "待分配"),
    TODO(1, "待办"),
    TRACK(2, "跟踪"),
    FINISH(3, "办结"),
    IGNORE(4, "忽略"),
    NO_REPLY(7, "未及时回复"),
    EXPIRED(8, "已逾期");

    private final int code;

    private final String name;

    StaticTaskStatusEnum(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static final Map<Integer, String> CODE_TO_NAME;

    static {
        CODE_TO_NAME = Arrays.stream(values())
                .collect(Collectors.toMap(StaticTaskStatusEnum::getCode, StaticTaskStatusEnum::getName));
    }
}