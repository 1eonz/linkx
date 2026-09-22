package com.tdtech.cloudcmd.icp.proxy.util;

import com.tdtech.cloudcmd.redis.RedisLockFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeriaExecutor {

    private final RedisLockFactory redisLockFactory;
    private final ThreadPoolTaskExecutor asyncMessageTaskExecutor;

    public void run(String lockKey, Runnable runnable) {
        var redisLock = redisLockFactory.newRedisLock(lockKey, Duration.ofMinutes(3L));
        if (!redisLock.tryLock(0L, TimeUnit.SECONDS)) {
            throw new LockException("lock busy");
        }
        var lockFlag = new AtomicBoolean(true);
        try {
            //续锁
            asyncMessageTaskExecutor.execute(() -> {
                while (lockFlag.get()) {
                    redisLock.delayTimeOut(Duration.ofMinutes(3L));
                    try {
                        Thread.sleep(2000L);
                    } catch (InterruptedException e) {
                        log.warn("interrupted", e);
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            });
            runnable.run();
        } finally {
            lockFlag.set(false);
            redisLock.unlock();
        }
    }

    public static class LockException extends RuntimeException {
        public LockException() {
            super();
        }

        public LockException(String message) {
            super(message);
        }

        public LockException(String message, Throwable cause) {
            super(message, cause);
        }

        public LockException(Throwable cause) {
            super(cause);
        }
    }
}
