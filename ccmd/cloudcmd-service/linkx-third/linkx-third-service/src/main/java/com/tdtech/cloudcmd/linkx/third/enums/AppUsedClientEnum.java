package com.tdtech.cloudcmd.linkx.third.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum AppUsedClientEnum {
    //打开的端侧。1：app；2：BS PC；3：CS PC
    APP(1, "APP"),

    BSPC(2, "BSPC"),

    CSPC(3, "CSPC");

    private final Integer code;

    private final String msg;
}
