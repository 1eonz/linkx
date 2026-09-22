package com.tdtech.cloudcmd.linkx.third.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.tdtech.cloudcmd.linkx.third.vo.DateFormatFieldInfo;
import com.tdtech.cloudcmd.linkx.third.vo.DateFormatFieldInfo.DateDimension;
import com.tdtech.cloudcmd.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 日期动态格式字段提取工具类
 * <p>
 * 用于从南向应用配置的 reqParam / reqBody 等 JSON 字符串中，
 * 识别值部分为日期动态格式占位符的字段，提取字段名、日期格式及时间维度。
 * </p>
 * <p>
 * 识别规则：只有使用 ${xx} 包裹的值才被视为日期动态格式占位符，
 * 不符合此规则的值一律视为普通字符串，不做提取。
 * </p>
 * <p>
 * 典型场景：用户配置请求参数时，某些字段的值使用日期格式占位符，
 * 如 {"startDate": "${yyyy-MM-dd}", "endTime": "${yyyy-MM-dd HH:mm:ss}"}，
 * 运行时需要将这些占位符替换为实际的日期时间值。
 * </p>
 * <p>
 * 支持识别的日期格式示例（需包裹在 ${} 中）：
 * <ul>
 *     <li>分隔符格式：${yyyy-MM-dd}、${YYYY-MM-DD HH:mm:ss}、${yyyy.MM.dd} 等</li>
 *     <li>紧凑格式：${yyyyMMdd}、${yyyyMMddHHmmss} 等</li>
 * </ul>
 * </p>
 */
public class DateFormatFieldUtils {
    /**
     * 日期表达式占位符正则，匹配 ${yyyy-MM-dd}、${yyyyMMddHHmmss} 等格式
     * <p>
     * 匹配规则：以 ${ 开头，} 结尾，中间为日期格式表达式
     * </p>
     */
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile(
            "^\\$\\{(.+)}$"
    );

    /**
     * 分隔符日期格式正则，匹配如 yyyy-MM-dd、YYYY-MM-DD HH:mm:ss、yyyy/MM/dd HH:mm 等格式
     * <p>
     * 匹配规则：以 yyyy/YYYY 开头，可选跟分隔符(-/./) + MM，再可选跟分隔符 + dd/DD，
     * 再可选跟 T或空格 + HH/hh:mm，再可选跟 :ss，再可选跟 .SSS
     * </p>
     */
    private static final Pattern DATE_FORMAT_PATTERN = Pattern.compile(
            "(?i)(?:yyyy|YYYY)(?:[-/.]MM(?:[-/.](?:dd|DD))?)?(?:[T\\s](?:HH|hh):mm(?::ss(?:\\.SSS)?)?)?"
    );

    /**
     * 紧凑日期格式正则，匹配如 yyyyMMdd、yyyyMMddHHmmss 等无分隔符格式
     * <p>
     * 匹配规则：以 yyyy/YYYY 开头，可选跟 MM，可选跟 dd，
     * 可选跟 HH，可选跟 mm，可选跟 ss
     * </p>
     */
    private static final Pattern COMPACT_DATE_FORMAT_PATTERN = Pattern.compile(
            "(?i)(?:yyyy|YYYY)(?:MM(?:dd)?)?(?:HH(?:mm(?:ss)?)?)?"
    );

    /**
     * 从单个 JSON 字符串中提取日期动态格式字段
     * <p>
     * 遍历 JSON 的第一层键值对，仅当值符合 ${xx} 占位符格式时才提取，
     * 不符合占位符格式的值视为普通字符串，直接忽略。
     * </p>
     *
     * @param jsonStr JSON 字符串，如 {"startDate": "${yyyy-MM-dd}", "name": "test"}
     * @return 日期动态格式字段信息列表，无匹配时返回空列表
     */
    public static List<DateFormatFieldInfo> extractDateFormatFields(String jsonStr) {
        if (StringUtils.isBlank(jsonStr)) {
            return Collections.emptyList();
        }
        JSONObject jsonObj;
        try {
            jsonObj = JSON.parseObject(jsonStr, Feature.OrderedField);
        } catch (Exception e) {
            return Collections.emptyList();
        }
        if (jsonObj == null || jsonObj.isEmpty()) {
            return Collections.emptyList();
        }
        List<DateFormatFieldInfo> result = new ArrayList<>();
        for (String key : jsonObj.keySet()) {
            Object value = jsonObj.get(key);
            if (!(value instanceof String)) {
                continue;
            }
            String strValue = (String) value;
            Matcher placeholderMatcher = PLACEHOLDER_PATTERN.matcher(strValue.trim());
            if (!placeholderMatcher.matches()) {
                continue;
            }
            String innerFormat = placeholderMatcher.group(1);
            String normalizedFormat = normalizeFormat(innerFormat);
            if (normalizedFormat != null) {
                DateDimension dimension = resolveDimension(normalizedFormat);
                result.add(DateFormatFieldInfo.builder()
                        .fieldName(key)
                        .dateFormat(normalizedFormat)
                        .dimension(dimension)
                        .build());
            }
        }
        return result;
    }

    /**
     * 从多个 JSON 字符串中提取日期动态格式字段（合并结果）
     * <p>
     * 典型用法：同时传入 reqParam 和 reqBody，合并提取所有日期动态格式字段。
     * </p>
     *
     * @param jsonStrs 多个 JSON 字符串，如 reqParam、reqBody
     * @return 合并后的日期动态格式字段信息列表
     */
    public static List<List<DateFormatFieldInfo>> extractFromMultipleJsons(String... jsonStrs) {
        List<List<DateFormatFieldInfo>> result = new ArrayList<>();
        for (String jsonStr : jsonStrs) {
            result.add(extractDateFormatFields(jsonStr));
        }
        return result;
    }

    /**
     * 规范化日期格式：从字符串中提取匹配的日期格式部分
     * <p>
     * 优先匹配分隔符格式（如 yyyy-MM-dd），若不匹配再尝试紧凑格式（如 yyyyMMdd）。
     * </p>
     *
     * @param value 待检测的字符串值，如 "YYYY-MM-DD" 或 "yyyyMMddHHmmss"
     * @return 匹配到的日期格式子串，不匹配则返回 null
     */
    static String normalizeFormat(String value) {
        Matcher delimitedMatcher = DATE_FORMAT_PATTERN.matcher(value);
        if (delimitedMatcher.find()) {
            return delimitedMatcher.group();
        }
        Matcher compactMatcher = COMPACT_DATE_FORMAT_PATTERN.matcher(value);
        if (compactMatcher.find()) {
            return compactMatcher.group();
        }
        return null;
    }

    /**
     * 根据日期格式字符串判断时间维度（精度级别）
     * <p>
     * 判定逻辑：从最高精度（秒）向最低精度（年）逐级判断，
     * 格式中包含 ss 则为秒级，包含 mm 则为分级，以此类推。
     * </p>
     * <p>
     * 示例：
     * <ul>
     *     <li>yyyy → YEAR</li>
     *     <li>yyyy-MM → MONTH</li>
     *     <li>yyyy-MM-dd → DAY</li>
     *     <li>yyyy-MM-dd HH → HOUR</li>
     *     <li>yyyy-MM-dd HH:mm → MINUTE</li>
     *     <li>yyyy-MM-dd HH:mm:ss → SECOND</li>
     * </ul>
     * </p>
     *
     * @param format 日期格式字符串，如 "yyyy-MM-dd HH:mm:ss"
     * @return 时间维度枚举值
     */
    static DateDimension resolveDimension(String format) {
        boolean hasSecond = format.contains("ss");
        boolean hasMinute = format.contains("mm") || hasSecond;
        boolean hasHour = format.contains("HH") || format.contains("hh") || hasMinute;
        boolean hasDay = format.contains("dd") || format.contains("DD") || hasHour;
        boolean hasMonth = format.contains("MM") || hasDay;

        if (hasSecond) {
            return DateDimension.SECOND;
        }
        if (hasMinute) {
            return DateDimension.MINUTE;
        }
        if (hasHour) {
            return DateDimension.HOUR;
        }
        if (hasDay) {
            return DateDimension.DAY;
        }
        if (hasMonth) {
            return DateDimension.MONTH;
        }
        return DateDimension.YEAR;
    }
}