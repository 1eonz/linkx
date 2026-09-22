package com.tdtech.cloudcmd.cnd.privatezone.queue.mq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tdtech.cloudcmd.cnd.privatezone.queue.redis.RedisQueue;

import io.micronaut.configuration.kafka.annotation.ErrorStrategy;
import io.micronaut.configuration.kafka.annotation.ErrorStrategyValue;
import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.OffsetStrategy;
import io.micronaut.configuration.kafka.annotation.Topic;
import io.micronaut.context.annotation.Property;
import jakarta.inject.Inject;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@KafkaListener(groupId = "${micronaut.application.name}", //
    threads = 10, //
    offsetStrategy = OffsetStrategy.AUTO, //
    batch = true, //
    errorStrategy = @ErrorStrategy(value = ErrorStrategyValue.RETRY_ON_ERROR, retryDelay = "50ms", retryCount = 2),
    properties = {@Property(name = "allow.auto.create.topics", value = "true")})
public class CagentListener {
    private static final Logger log = LoggerFactory.getLogger(CagentListener.class);

    @Inject
    RedisQueue redisQueue;

    public CagentListener() {
        log.info("init CagentListener");
    }

    @Topic({"cloudcmd-cagent"})
    public Mono<Void> pushQueue1(Flux<String> records) {
        return redisQueue.pushQueue("cloudcmd-cagent", records).then();
    }

    @Topic({"invalid_token"})
    public Mono<Void> pushQueue2(Flux<String> records) {
        return redisQueue.pushQueue("invalid_token", records).then();
    }

    @Topic({"messageToCagent"})
    public Mono<Void> pushQueue3(Flux<String> records) {
        return redisQueue.pushQueue("messageToCagent", records).then();
    }

    @Topic({"sysMessageToCagent"})
    public Mono<Void> pushQueue4(Flux<String> records) {
        return redisQueue.pushQueue("sysMessageToCagent", records).then();
    }

    @Topic({"adminTOCagent"})
    public Mono<Void> pushQueue5(Flux<String> records) {
        return redisQueue.pushQueue("adminTOCagent", records).then();
    }

}
