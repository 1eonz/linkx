package com.tdtech.cloudcmd.cagent.service.outbound.rule;

import java.util.Arrays;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;

import com.tdtech.cloudcmd.cagent.service.entity.BaseChannel;
import com.tdtech.cloudcmd.exception.BusinessException;

public interface DispatcherRule {
    String AT = "@";
    String DASH = "-";
    String ASTERISK = "*";
    String AND = "&";
    String MC = "mc:";
    String UC = "uc:";
    String COMMA = ",";

    boolean match(String patternFrom, String patternTo);

    <T extends BaseChannel> Stream<T> pick(String patternFrom, String patternTo, Stream<T> source);

    default void assertStringArray(String[] str, int length) {
        if (str.length != length) {
            throw new BusinessException("pattern error:" + Arrays.toString(str) + " - " + length);
        }
    }

    default void assertStringArrayAndNotBlank(String[] str, int length) {
        assertStringArray(str, length);
        for (String s : str) {
            if (StringUtils.isBlank(s)) {
                throw new BusinessException("pattern error:" + Arrays.toString(str) + " - " + length);
            }
        }
    }

    default boolean anyEquals(String[] source, String target) {
        for (String s : source) {
            if (StringUtils.isNotBlank(s) && s.equals(target)) {
                return true;
            }
        }
        return false;
    }
}
