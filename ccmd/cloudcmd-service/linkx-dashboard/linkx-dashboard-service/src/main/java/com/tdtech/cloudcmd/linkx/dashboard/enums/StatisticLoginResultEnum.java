package com.tdtech.cloudcmd.linkx.dashboard.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public enum StatisticLoginResultEnum {

    SUCCESS(0, "成功"),

    FAILURE(1, "失败");

    private final int code;

    private final String loginResult;

    StatisticLoginResultEnum(int code, String loginResult) {
        this.code = code;
        this.loginResult = loginResult;
    }

    // code -> loginResult
    public static final Map<Integer, String> CODE_TO_LOGIN_RESULT;

    // loginResult -> code
    public static final Map<String, Integer> LOGIN_RESULT_TO_CODE;

    static {
        CODE_TO_LOGIN_RESULT = Arrays.stream(values())
                .collect(Collectors.toMap(StatisticLoginResultEnum::getCode,
                        StatisticLoginResultEnum::getLoginResult));

        LOGIN_RESULT_TO_CODE = Arrays.stream(values())
                .collect(Collectors.toMap(StatisticLoginResultEnum::getLoginResult,
                        StatisticLoginResultEnum::getCode));
    }
}