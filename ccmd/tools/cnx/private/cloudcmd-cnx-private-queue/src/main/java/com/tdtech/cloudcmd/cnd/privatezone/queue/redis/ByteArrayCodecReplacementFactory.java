package com.tdtech.cloudcmd.cnd.privatezone.queue.redis;

import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Replaces;
import jakarta.inject.Singleton;

@Factory
public class ByteArrayCodecReplacementFactory {
    @Singleton
    @Replaces(RedisCodec.class)
    public RedisCodec<byte[], byte[]> redisCodec() {
        return ByteArrayCodec.INSTANCE;
    }
}
