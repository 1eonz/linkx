package com.tdtech.cloudcmd.linkx.third.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum GroupTypeEnum {
    SYSTEM(1, "系统级"),

    USER(2, "用户级");

    private final Integer code;

    private final String msg;

}
