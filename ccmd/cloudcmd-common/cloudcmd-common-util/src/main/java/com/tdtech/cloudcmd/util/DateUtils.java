package com.tdtech.cloudcmd.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

/**
 * 时间工具类
 *
 * @author 芋道源码
 */
public class DateUtils {
    /**
     * 将 LocalDateTime 转换成 Date
     *
     * @param date LocalDateTime
     * @return LocalDateTime
     */
    public static Date of(LocalDateTime date) {
        if (date == null) {
            return null;
        }
        // 将此日期时间与时区相结合以创建 ZonedDateTime
        ZonedDateTime zonedDateTime = date.atZone(ZoneId.systemDefault());
        // 本地时间线 LocalDateTime 到即时时间线 Instant 时间戳
        Instant instant = zonedDateTime.toInstant();
        // UTC时间(世界协调时间,UTC + 00:00)转北京(北京,UTC + 8:00)时间
        return Date.from(instant);
    }

    /**
     * 将 Date 转换成 LocalDateTime
     *
     * @param date Date
     * @return LocalDateTime
     */
    public static LocalDateTime of(Date date) {
        if (date == null) {
            return null;
        }
        // 转为时间戳
        Instant instant = date.toInstant();
        // UTC时间(世界协调时间,UTC + 00:00)转北京(北京,UTC + 8:00)时间
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
