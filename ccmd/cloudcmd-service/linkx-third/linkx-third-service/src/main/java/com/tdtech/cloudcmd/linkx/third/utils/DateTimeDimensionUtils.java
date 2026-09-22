package com.tdtech.cloudcmd.linkx.third.utils;

import com.tdtech.cloudcmd.encryptor.service.CachedConfig;
import com.tdtech.cloudcmd.linkx.third.vo.DateFormatFieldInfo.DateDimension;
import com.tdtech.cloudcmd.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 时间维度计算工具类
 * <p>
 * 根据 DateDimension 对 LocalDateTime 进行加减操作，
 * 以及从 tb_system_config 读取 backward 步长配置。
 * </p>
 */
@Slf4j
public class DateTimeDimensionUtils {

    /**
     * backward 步长配置键前缀
     */
    private static final String BACKWARD_STEP_CONFIG_PREFIX = "BACKWARD_STEP_";

    /**
     * 各维度的默认步长值
     */
    private static final int DEFAULT_STEP_YEAR = 1;
    private static final int DEFAULT_STEP_MONTH = 1;
    private static final int DEFAULT_STEP_DAY = 1;
    private static final int DEFAULT_STEP_HOUR = 24;
    private static final int DEFAULT_STEP_MINUTE = 60;
    private static final int DEFAULT_STEP_SECOND = 3600;

    /**
     * 根据时间维度对 LocalDateTime 进行减法操作
     *
     * @param dateTime   目标时间
     * @param dimension  时间维度
     * @param units      减去的单元数
     * @return 减去后的时间
     */
    public static LocalDateTime minus(LocalDateTime dateTime, DateDimension dimension, long units) {
        switch (dimension) {
            case YEAR:
                return dateTime.minusYears(units);
            case MONTH:
                return dateTime.minusMonths(units);
            case DAY:
                return dateTime.minusDays(units);
            case HOUR:
                return dateTime.minusHours(units);
            case MINUTE:
                return dateTime.minusMinutes(units);
            case SECOND:
                return dateTime.minusSeconds(units);
            default:
                log.warn("未知的时间维度: {}, 默认按天处理", dimension);
                return dateTime.minusDays(units);
        }
    }

    /**
     * 根据时间维度对 LocalDateTime 进行加法操作
     *
     * @param dateTime   目标时间
     * @param dimension  时间维度
     * @param units      加上的单元数
     * @return 加上后的时间
     */
    public static LocalDateTime plus(LocalDateTime dateTime, DateDimension dimension, long units) {
        switch (dimension) {
            case YEAR:
                return dateTime.plusYears(units);
            case MONTH:
                return dateTime.plusMonths(units);
            case DAY:
                return dateTime.plusDays(units);
            case HOUR:
                return dateTime.plusHours(units);
            case MINUTE:
                return dateTime.plusMinutes(units);
            case SECOND:
                return dateTime.plusSeconds(units);
            default:
                log.warn("未知的时间维度: {}, 默认按天处理", dimension);
                return dateTime.plusDays(units);
        }
    }

    /**
     * 从 tb_system_config 读取 backward 步长 N
     * <p>
     * 配置键格式: BACKWARD_STEP_{DIMENSION}，如 BACKWARD_STEP_DAY
     * </p>
     *
     * @param cachedConfig   缓存配置服务
     * @param dimension         时间维度
     * @return 步长值
     */
    public static int getBackwardStep(CachedConfig cachedConfig, DateDimension dimension) {
        String configKey = BACKWARD_STEP_CONFIG_PREFIX + dimension.name();
        String configValue = cachedConfig.getSystemConfig(configKey);
        if (StringUtils.isNotBlank(configValue)) {
            try {
                int step = Integer.parseInt(configValue.trim());
                log.info("读取 backward 步长配置: key={}, value={}", configKey, step);
                return step;
            } catch (NumberFormatException e) {
                log.warn("backward 步长配置值格式错误: key={}, value={}, 使用默认值", configKey, configValue);
            }
        }
        // 使用默认值
        int defaultStep = getDefaultStep(dimension);
        log.info("backward 步长配置未找到或为空: key={}, 使用默认值: {}", configKey, defaultStep);
        return defaultStep;
    }

    /**
     * 获取各维度的默认步长值
     */
    private static int getDefaultStep(DateDimension dimension) {
        switch (dimension) {
            case YEAR:
                return DEFAULT_STEP_YEAR;
            case MONTH:
                return DEFAULT_STEP_MONTH;
            case DAY:
                return DEFAULT_STEP_DAY;
            case HOUR:
                return DEFAULT_STEP_HOUR;
            case MINUTE:
                return DEFAULT_STEP_MINUTE;
            case SECOND:
                return DEFAULT_STEP_SECOND;
            default:
                return DEFAULT_STEP_DAY;
        }
    }

    /**
     * 计算两个 LocalDateTime 之间的时间单元数（用于日志记录）
     *
     * @param start     开始时间
     * @param end       结束时间
     * @param dimension 时间维度
     * @return 时间单元数
     */
    public static long between(LocalDateTime start, LocalDateTime end, DateDimension dimension) {
        switch (dimension) {
            case YEAR:
                return ChronoUnit.YEARS.between(start, end);
            case MONTH:
                return ChronoUnit.MONTHS.between(start, end);
            case DAY:
                return ChronoUnit.DAYS.between(start, end);
            case HOUR:
                return ChronoUnit.HOURS.between(start, end);
            case MINUTE:
                return ChronoUnit.MINUTES.between(start, end);
            case SECOND:
                return ChronoUnit.SECONDS.between(start, end);
            default:
                return ChronoUnit.DAYS.between(start, end);
        }
    }
}