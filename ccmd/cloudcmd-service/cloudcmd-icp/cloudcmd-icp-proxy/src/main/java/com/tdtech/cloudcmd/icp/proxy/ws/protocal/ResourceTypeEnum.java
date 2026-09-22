package com.tdtech.cloudcmd.icp.proxy.ws.protocal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ResourceTypeEnum {
    SUB("sub"), UNSUB("unsub");

    private final String type;

    ResourceTypeEnum(String type) {
        this.type = type;
    }

    @JsonCreator
    public static ResourceTypeEnum typeOf(String type) {
        for (ResourceTypeEnum value : ResourceTypeEnum.values()) {
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