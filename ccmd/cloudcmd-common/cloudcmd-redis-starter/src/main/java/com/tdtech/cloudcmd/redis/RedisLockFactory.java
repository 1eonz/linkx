package com.tdtech.cloudcmd.redis;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@AllArgsConstructor
public class RedisLockFactory {
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
        "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
        Long.class);
    private final ByteArrayRedisTemplate redisTemplate;

    public RedisLock newRedisLock(String key, Duration lockExpireTime) {
        return new RedisLock(key, lockExpireTime);
    }

    public RedisLock newRedisLock(String key, Duration lockExpireTime, String random) {
        return new RedisLock(key, lockExpireTime, random);
    }

    public class RedisLock {

        private final byte[] random;
        private final String key;
        private final Duration lockExpireTime;

        private RedisLock(String key, Duration lockExpireTime) {
            this.lockExpireTime = lockExpireTime;
            this.key = key;
            random = new byte[64];
            ThreadLocalRandom.current().nextBytes(random);
        }

        private RedisLock(String key, Duration lockExpireTime, String random) {
            this.lockExpireTime = lockExpireTime;
            this.key = key;
            this.random = random.getBytes(StandardCharsets.UTF_8);
        }

        @SuppressWarnings("BusyWait")
        public boolean tryLock(Long time, TimeUnit unit) {
            Objects.requireNonNull(time);
            Objects.requireNonNull(unit);
            long endTime = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(time, unit);
            Boolean b;
            while ((b = redisTemplate.opsForValue().setIfAbsent(key, random, lockExpireTime)) == null || !b) {
                if (System.currentTimeMillis() > endTime) {
                    return false;
                }
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException e) {
                    log.error("lock interrupted", e);
                    // ignore
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            return true;
        }

        public void delayTimeOut(Duration delay) {
            redisTemplate.expire(key, delay);
        }

        public void tryLockWithException(Long time, TimeUnit unit) {
            if (!tryLock(time, unit)) {
                throw new RedisLockException("lock busy!");
            }
        }

        public boolean unlock() {
            Long execute = redisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(key), new Object[] {random});
            return execute != null && !execute.equals(0L);
        }
    }

}
