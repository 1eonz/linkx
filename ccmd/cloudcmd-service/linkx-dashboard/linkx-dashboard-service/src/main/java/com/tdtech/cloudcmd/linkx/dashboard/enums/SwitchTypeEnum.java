package com.tdtech.cloudcmd.linkx.dashboard.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 协同岗切换类型枚举（导出转中文用）。
 */
@Getter
public enum SwitchTypeEnum {

    USER_CLICK(0, "手工切换"),
    IM_NOTIFY(1, "IM状态变化切换"),
    DUTY_AUTO(2, "值班自动上下岗"),
    ADMIN_CLICK(3, "管理员操作下岗"),
    OTHER(99, "其他");

    private final int code;

    private final String name;

    SwitchTypeEnum(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static final Map<Integer, String> CODE_TO_NAME;

    static {
        CODE_TO_NAME = Arrays.stream(values())
                .collect(Collectors.toMap(SwitchTypeEnum::getCode, SwitchTypeEnum::getName));
    }
}