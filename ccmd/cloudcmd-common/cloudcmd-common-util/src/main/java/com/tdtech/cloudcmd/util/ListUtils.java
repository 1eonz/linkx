package com.tdtech.cloudcmd.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.util.CollectionUtils;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class ListUtils extends org.apache.commons.collections.ListUtils {
    /**
     * 判断list是否不是空的
     * 
     * @param list 要判断的目标list
     * @return boolean
     */
    public static boolean isNotBlankList(List<?> list) {
        return !CollectionUtils.isEmpty(list);
    }

    /**
     * 判断list是否是空的
     *
     * @param list 要判断的目标list
     * @return boolean
     */
    public static boolean isBlankList(List<?> list) {
        return CollectionUtils.isEmpty(list);
    }

    /**
     * 将字符串转换成List
     */
    public static List<String> toStringList(String[] strs) {
        return Stream.of(strs).collect(Collectors.toList());
    }

    /**
     * 将字符串转换成List，以,分隔
     */
    public static List<String> toStringList(String str) {
        return toStringList(str, ",");
    }

    /**
     * 将字符串转换成Set,以,分隔
     */
    public static Set<String> toStringSet(String str) {
        return toStringSet(str, ",");
    }

    /**
     * 将字符串转换成List
     * 
     * @param str 数组字符串
     * @param sparator 分隔符
     */
    public static List<String> toStringList(String str, String sparator) {
        return Lists.newArrayList(Splitter.on(sparator).trimResults().split(str));
    }

    /**
     * 将字符串转换成Set
     * 
     * @param str 数组字符串
     * @param sparator 分隔符
     */
    public static Set<String> toStringSet(String str, String sparator) {
        return Sets.newHashSet(toStringList(str, sparator));
    }

    public static <T> Set<T> listToSet(List<T> list) {
        if (CollectionUtils.isEmpty(list))
            return Collections.emptySet();
        return new HashSet<T>(list);
    }
}
