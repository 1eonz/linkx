package com.tdtech.cloudcmd.cnd.privatezone.queue.redis;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.lettuce.core.api.StatefulRedisConnection;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Singleton
public class RedisQueue {
    private static final Logger log = LoggerFactory.getLogger(RedisQueue.class);
    private static final String REDIS_QUEUE_PREFIX = "cloudcmd:cnx:private:queue:";

    @Inject
    @Named("default")
    StatefulRedisConnection<byte[], byte[]> statefulRedisConnection;

    public Mono<Long> pushQueue(String channel, Flux<String> datas) {
        return datas.collectList().flatMap(list -> {
            log.info("on message:{} {}", channel, list);
            if (list == null || list.isEmpty()) {
                return Mono.just(0L);
            }
            var array = list.stream()//
                .map(a -> a.getBytes(StandardCharsets.UTF_8))//
                .toArray(byte[][]::new);
            return statefulRedisConnection.reactive()
                .rpush((REDIS_QUEUE_PREFIX + channel).getBytes(StandardCharsets.UTF_8), array);
        });
    }

    public Flux<String> pullAll(String channel) {
        var queueKey = (REDIS_QUEUE_PREFIX + channel).getBytes(StandardCharsets.UTF_8);
        return statefulRedisConnection.reactive().llen(queueKey).flatMapMany(length -> {
            if (length > 0) {
                return Flux.range(0, length.intValue() / 100 + 1)//
                    .flatMap(i -> statefulRedisConnection.reactive()//
                        .lpop(queueKey, 100)//
                        .map(data -> new String(data, StandardCharsets.UTF_8)));
            } else {
                return Flux.empty();
            }
        });
    }

}
