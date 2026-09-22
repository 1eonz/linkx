package com.tdtech.cloudcmd.linkx.dashboard.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tdtech.cloudcmd.redis.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.function.Supplier;

/**
 * 统计同步水位线工具：Redis 优先 + 统计表兜底回查。
 * <p>
 * 读水位线：先查 Redis，未命中则调 fallback 供应商从统计表回查 (时间 DESC, 源表id DESC LIMIT 1)。
 * 写水位线：直接写 Redis（不持久化到统计表，统计表自身的数据即为兜底回查来源）。
 * <p>
 * Redis 丢失是低频路径，兜底回查不加索引，全表扫可接受。
 */
@Slf4j
@Component
public class WatermarkHelper {

    private static final String KEY_PREFIX = "dashboard:sync:watermark:";

    @Resource
    private RedisUtil redisUtil;

    /**
     * 读水位线：Redis 优先，未命中调 fallback 回查统计表。
     *
     * @param tableName 统计表名（用于拼 Redis key）
     * @param fallback  Redis 未命中时从统计表回查的供应商
     * @return 水位线（首次拉取时 time/id 均为 null）
     */
    public SyncWatermark getWatermark(String tableName, Supplier<SyncWatermark> fallback) {
        String key = KEY_PREFIX + tableName;
        try {
            SyncWatermark watermark = redisUtil.get(key, new TypeReference<SyncWatermark>() {});
            if (watermark != null && !watermark.isEmpty()) {
                return watermark;
            }
        } catch (Exception e) {
            log.warn("getWatermark from redis failed, table:{}, fallback to db", tableName, e);
        }
        // Redis 未命中或异常 → 兜底回查统计表
        SyncWatermark fallbackWatermark = fallback.get();
        if (fallbackWatermark == null) {
            return new SyncWatermark();
        }
        // 回查到则回写 Redis，避免下次再走全表扫
        if (!fallbackWatermark.isEmpty()) {
            setWatermark(tableName, fallbackWatermark);
        }
        return fallbackWatermark;
    }

    /**
     * 写水位线到 Redis。
     */
    public void setWatermark(String tableName, Date time, Long id) {
        setWatermark(tableName, new SyncWatermark(time, id));
    }

    /**
     * 写水位线到 Redis。
     */
    public void setWatermark(String tableName, SyncWatermark watermark) {
        String key = KEY_PREFIX + tableName;
        try {
            redisUtil.set(key, watermark == null ? new SyncWatermark() : watermark);
        } catch (Exception e) {
            log.warn("setWatermark to redis failed, table:{}", tableName, e);
        }
    }
}