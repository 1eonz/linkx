package com.cs.datatool.utils;

import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.apache.commons.lang3.time.FastDateFormat;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class DateFormatUtil {
    public static final String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";
    private static final Pattern YYYY_MM_DD_HH_MM_SS_PATTERN =
        Pattern.compile("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$");

    public static final String YYYYMMDDHHMMSS = "yyyyMMddHHmmss";
    private static final Pattern YYYYMMDDHHMMSS_PATTERN = Pattern.compile("^\\d{14}$");

    public static final String YYYYMMDDHHMMSSSSS = "yyyyMMddHHmmssSSS";
    private static final Pattern YYYYMMDDHHMMSSSSS_PATTERN = Pattern.compile("^\\d{17}$");

    public static final String YYYY_MM_DD = "yyyy-MM-dd";
    private static final Pattern YYYY_MM_DD_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

    public static final String HH_MM_SS= "HH:mm:ss";
    private static final Pattern HH_MM_SS_PATTERN = Pattern.compile("^\\d{2}:\\d{2}:\\d{2}$");

    private static final FastDateFormat COMMON_FORMAT = FastDateFormat.getInstance(YYYY_MM_DD_HH_MM_SS);

    private DateFormatUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * 默认pattern： yyyy-MM-dd HH:mm:ss
     */
    @Deprecated
    public static String format(Date date) {
        if (date == null) {
            return null;
        }
        return COMMON_FORMAT.format(date);
    }

    public static String format(Long time, String pattern, ZoneId timeZoneId) {
        if (time == null) {
            return null;
        }
        var localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(time), timeZoneId);
        return localDateTime.format(DateTimeFormatter.ofPattern(pattern));
    }

    public static String format(Date date, String pattern, ZoneId timeZoneId) {
        if (date == null) {
            return null;
        }
        return format(date.getTime(), pattern, timeZoneId);
    }

    public static String format(Date date, String pattern) {
        if (date == null) {
            return null;
        }
        Objects.requireNonNull(pattern);
        var instance = FastDateFormat.getInstance(pattern);
        return instance.format(date);
    }

    /**
     * 默认pattern： yyyy-MM-dd HH:mm:ss
     */
    @Deprecated
    public static String format(Long date) {
        if (date == null) {
            return null;
        }
        return COMMON_FORMAT.format(date);
    }

    public static String format(Long date, String pattern) {
        if (date == null) {
            return null;
        }
        Objects.requireNonNull(pattern);
        var instance = FastDateFormat.getInstance(pattern);
        return instance.format(date);
    }

    /**
     * 默认pattern： yyyy-MM-dd HH:mm:ss
     */
    @Deprecated
    public static String format(LocalDateTime date) {
        if (date == null) {
            return null;
        }
        return date.format(DateTimeFormatter.ofPattern(YYYY_MM_DD_HH_MM_SS));
    }

    public static String format(LocalDateTime date, String pattern) {
        if (date == null) {
            return null;
        }
        Objects.requireNonNull(pattern);
        return date.format(DateTimeFormatter.ofPattern(pattern));
    }

    public static String format(LocalTime time, String pattern) {
        if (time == null) {
            return null;
        }
        Objects.requireNonNull(pattern);
        return time.format(DateTimeFormatter.ofPattern(pattern));
    }

    public static String format(LocalDate date, String pattern) {
        if (date == null) {
            return null;
        }
        Objects.requireNonNull(pattern);
        return date.format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 正则自适应时间格式，没事少用
     */
    @Deprecated
    @SneakyThrows(ParseException.class)
    public static Date parseDate(String str) {
        if (YYYY_MM_DD_HH_MM_SS_PATTERN.matcher(str).matches()) {
            return FastDateFormat.getInstance(YYYY_MM_DD_HH_MM_SS).parse(str);
        } else if (YYYYMMDDHHMMSS_PATTERN.matcher(str).matches()) {
            return FastDateFormat.getInstance(YYYYMMDDHHMMSS).parse(str);
        } else if (YYYYMMDDHHMMSSSSS_PATTERN.matcher(str).matches()) {
            return FastDateFormat.getInstance(YYYYMMDDHHMMSSSSS).parse(str);
        } else if (YYYY_MM_DD_PATTERN.matcher(str).matches()) {
            return FastDateFormat.getInstance(YYYY_MM_DD).parse(str);
        } else if (HH_MM_SS_PATTERN.matcher(str).matches()) {
            return FastDateFormat.getInstance(HH_MM_SS).parse(str);
        } else {
            throw new ParseException("unknown format", 0);
        }
    }

    @SneakyThrows(ParseException.class)
    public static Date parseDate(String str, String pattern) {
        return FastDateFormat.getInstance(pattern).parse(str);
    }

    @SneakyThrows(ParseException.class)
    public static Date parseDate(String str, String pattern, TimeZone timeZone) {

        return FastDateFormat.getInstance(pattern, timeZone).parse(str);
    }

    public static LocalDateTime parseLocalDateTime(String str, String pattern) {
        return LocalDateTime.parse(str, DateTimeFormatter.ofPattern(pattern));
    }

    public static LocalDate parseLocalDate(String str, String pattern) {
        return LocalDate.parse(str, DateTimeFormatter.ofPattern(pattern));
    }

    public static LocalTime parseLocalTime(String str, String pattern) {
        return LocalTime.parse(str, DateTimeFormatter.ofPattern(pattern));
    }

    public static String transform(String source, String sourcePattern, String targetPattern) {
        var date = parseDate(source, sourcePattern);
        return format(date, targetPattern);
    }

    /**
     * 正则解析源字符串格式，没事别用
     */
    @Deprecated
    public static String transform(String source, String targetPattern) {
        var date = parseDate(source);
        return format(date, targetPattern);
    }
}
