package com.tdtech.cloudcmd.icp.proxy.ws.protocal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum GisOptTypeEnum {
    REPORT("report"), SUB("sub"), UNSUB("unsub");

    private final String type;

    GisOptTypeEnum(String type) {
        this.type = type;
    }

    @JsonCreator
    public static GisOptTypeEnum typeOf(String type) {
        for (GisOptTypeEnum value : GisOptTypeEnum.values()) {
            if (value.type.equals(type)) {
                return value;
            }
        }
        return null;
    }

    @JsonValue
    public String type() {
        return this.type;
    }
}