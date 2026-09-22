package com.tdtech.cloudcmd.util;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

/**
 * @author zhuangzl
 * @date 2020-06-25 09:53
 */
public class LongUtils {

    private static final long DEFAULT_VALUE = -1L;

    public static Long parseLong(String var) {
        if (!StringUtils.isNumeric(var)) {
            return DEFAULT_VALUE;
        }
        return Long.parseLong(var);
    }

    public static Long parseLong(String var, long defaultValue) {
        String s = Optional.ofNullable(var).orElse(defaultValue + "");
        return Long.valueOf(s);
    }
}
