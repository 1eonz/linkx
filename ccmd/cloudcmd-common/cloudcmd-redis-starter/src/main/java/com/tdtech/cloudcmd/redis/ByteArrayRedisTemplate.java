package com.tdtech.cloudcmd.redis;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

public class ByteArrayRedisTemplate extends RedisTemplate<String, byte[]> {

    public static class ByteArrayRedisSerializer implements RedisSerializer<byte[]> {

        @Override
        public byte[] serialize(byte[] bytes) throws SerializationException {
            return bytes;
        }

        @Override
        public byte[] deserialize(byte[] bytes) throws SerializationException {
            return bytes;
        }

        @Override
        public Class<?> getTargetType() {
            return byte[].class;
        }
    }

}
