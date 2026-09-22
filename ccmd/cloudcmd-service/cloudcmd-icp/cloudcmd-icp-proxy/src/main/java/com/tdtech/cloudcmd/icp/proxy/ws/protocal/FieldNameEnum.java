package com.tdtech.cloudcmd.icp.proxy.ws.protocal;

import lombok.Getter;

@Getter
public enum FieldNameEnum {

    CMD("cmd"), OPT("opt");

    private final String value;

    FieldNameEnum(String value) {
        this.value = value;
    }
}
