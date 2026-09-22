package com.tdtech.cloudcmd.linkx.dashboard.enums;

import lombok.Getter;

import java.util.Map;

@Getter
public enum StatisticLoginClientTypeEnum {

    BS_PC(1, "BS PC"),

    CS_PC(2, "CS PC"),

    APP_H5(3, "App H5"),

    ADMIN(4, "Admin"),

    RESTFUL(5, "RESTful"),

    JS_SDK(6, "JS-SDK");

    private final int code;

    private final String clientType;

    StatisticLoginClientTypeEnum(int code, String clientType) {
        this.code = code;
        this.clientType = clientType;
    }

    // code -> clientType
    public static final Map<Integer, String> CODE_TO_CLIENT_TYPE;

    // clientType -> code
    public static final Map<String, Integer> CLIENT_TYPE_TO_CODE;

    static {
        CODE_TO_CLIENT_TYPE = java.util.Arrays.stream(values())
                .collect(java.util.stream.Collectors.toMap(StatisticLoginClientTypeEnum::getCode,
                        StatisticLoginClientTypeEnum::getClientType));

        CLIENT_TYPE_TO_CODE = java.util.Arrays.stream(values())
                .collect(java.util.stream.Collectors.toMap(StatisticLoginClientTypeEnum::getClientType,
                        StatisticLoginClientTypeEnum::getCode));
    }
}