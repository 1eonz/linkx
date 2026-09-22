package com.tdtech.cloudcmd.linkx.third.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TaskCreateTypeEnum {
    MANUAL(1, "人工"),
    AUTO(2, "自动");

    private final Integer code;

    private final String msg;
}
