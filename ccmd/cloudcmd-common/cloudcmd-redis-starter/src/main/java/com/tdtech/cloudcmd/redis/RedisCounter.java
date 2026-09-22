package com.tdtech.cloudcmd.redis;

import lombok.AllArgsConstructor;

import java.time.Duration;
import java.util.Objects;

/**
 * @author dl272038
 * @date 2022/8/30 17:25
 */
@AllArgsConstructor
public class RedisCounter {

    private final static String WINDOW_KEY_PREFIX = "cloudcmd:limiter:timedWindow:";

    private RedisUtil redisUtil;

    /**
     * 滑动窗口计数
     */
    public boolean check(String key, Duration timedWindowWidth, Integer timedWindowSize) {
        Objects.requireNonNull(timedWindowWidth);
        Objects.requireNonNull(timedWindowSize);
        if (key == null || key.isBlank()) {
            throw new NullPointerException("key");
        }
        double now = (double) System.currentTimeMillis();
        double startUp = now - (double) timedWindowWidth.toMillis();
        var aLong = redisUtil.zCount(WINDOW_KEY_PREFIX + key, startUp, now);
        return aLong == null || aLong < timedWindowSize;
    }

    public void add(String key, Duration ttl) {
        Objects.requireNonNull(ttl);
        if (key == null || key.isBlank()) {
            throw new NullPointerException("key");
        }
        var redis_key = WINDOW_KEY_PREFIX + key;
        var now = System.currentTimeMillis();
        redisUtil.zAdd(redis_key, (double) now, now);
        redisUtil.expire(redis_key, ttl);
    }


}
