package com.tdtech.cloudcmd.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializer;
import redis.clients.jedis.JedisPoolConfig;

import javax.annotation.Resource;

/**
 * @author mWX556161
 * @date 2020/6/3 9:13
 */
@Configuration
public class RedisAutoConfiguration {

    @Resource
    private RedisProperties redisProperties;

    @Bean
    public JedisPoolConfig jedisPoolConfig() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxIdle(10);
        config.setMaxTotal(100);
        config.setMaxWaitMillis(5000);
        config.setMinIdle(0);
        config.setTestOnBorrow(true);
        config.setTestWhileIdle(true);
        config.setNumTestsPerEvictionRun(2);
        config.setTimeBetweenEvictionRunsMillis(30000);
        config.setMinEvictableIdleTimeMillis(60000);
        config.setSoftMinEvictableIdleTimeMillis(60000);
        return config;
    }

    @Bean("customizeRedisConnectionFactory")
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        JedisConnectionFactory redisConnectionFactory = new JedisConnectionFactory(jedisPoolConfig());
        redisConnectionFactory.setDatabase(redisProperties.getDatabase());
        redisConnectionFactory.setHostName(redisProperties.getHost());
        redisConnectionFactory.setPort(redisProperties.getPort());
        redisConnectionFactory.setPassword(redisProperties.getPassword());
        redisConnectionFactory.setTimeout(150000);
        redisConnectionFactory.setUsePool(true);
        return redisConnectionFactory;

    }

    @Bean
    public ByteArrayRedisTemplate byteArrayRedisTemplate(RedisConnectionFactory factory) {
        ByteArrayRedisTemplate template = new ByteArrayRedisTemplate();
        ByteArrayRedisTemplate.ByteArrayRedisSerializer byteArrayRedisSerializer =
            new ByteArrayRedisTemplate.ByteArrayRedisSerializer();
        template.setConnectionFactory(factory);
        template.setKeySerializer(RedisSerializer.string());
        template.setHashKeySerializer(RedisSerializer.string());
        template.setDefaultSerializer(byteArrayRedisSerializer);
        template.setValueSerializer(byteArrayRedisSerializer);
        template.setHashValueSerializer(byteArrayRedisSerializer);
        // 设置关闭事务，redisTemplate自动释放连接
        template.setEnableTransactionSupport(false);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisUtil redisUtil(ByteArrayRedisTemplate template,ObjectMapper objectMapper) {
        return new RedisUtil(template, objectMapper);
    }

    @Bean
    public RedisLockFactory redisLockFactory(ByteArrayRedisTemplate template) {
        return new RedisLockFactory(template);
    }

    @Bean
    public RedisCounter simpleLimitter(RedisUtil redisUtil) {
        return new RedisCounter(redisUtil);
    }
}
