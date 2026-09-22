package com.tdtech.cloudcmd.linkx.third.utils;

import cn.hutool.core.date.DateException;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;

import java.time.LocalDateTime;

/**
 * 日期工具类
 */
public class DateUtils {

    private static final String[] COMPAT_PATTERNS = {
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",
            "yyyy-MM-dd'T'HHmmss.SSSSSS"
    };


    /**
     * 万能解析字符串转LocalDateTime
     * @param timeStr 任意格式时间字符串
     * @return LocalDateTime，空/非法日期返回null
     */
    public static LocalDateTime parseLdt(String timeStr) {
        if (StrUtil.isBlank(timeStr)) {
            return null;
        }
        // 先尝试hutool内置自动识别所有常规格式
        try {
            return DateUtil.parse(timeStr).toLocalDateTime();
        } catch (DateException ignored) {
            // 内置模板匹配失败，使用自定义兼容模板重试
        }
        // 自定义多模板依次尝试
        try {
            return DateUtil.parse(timeStr, COMPAT_PATTERNS).toLocalDateTime();
        } catch (DateException e) {
            // 全部格式都无法匹配，返回null
            return null;
        }
    }
}
