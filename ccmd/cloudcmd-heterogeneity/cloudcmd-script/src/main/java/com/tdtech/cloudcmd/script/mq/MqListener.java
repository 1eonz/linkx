package com.tdtech.cloudcmd.script.mq;

import com.tdtech.cloudcmd.script.engine.JsEngine;
import com.tdtech.cloudcmd.script.repo.TicketClient;
import com.tdtech.cloudcmd.script.repo.TicketClientRepository;
import io.micronaut.configuration.kafka.annotation.ErrorStrategy;
import io.micronaut.configuration.kafka.annotation.ErrorStrategyValue;
import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.OffsetStrategy;
import io.micronaut.configuration.kafka.annotation.Topic;
import io.micronaut.context.annotation.Property;
import io.micronaut.json.JsonMapper;
import io.micronaut.messaging.annotation.SendTo;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.script.ScriptException;
import java.io.IOException;
import java.lang.reflect.Array;

@KafkaListener(groupId = "${micronaut.application.name}", //
        threads = 10, //
        offsetStrategy = OffsetStrategy.AUTO, //
        batch = true, //
        errorStrategy = @ErrorStrategy(value = ErrorStrategyValue.RETRY_ON_ERROR, retryDelay = "50ms", retryCount = 2), properties = {
        @Property(name = "allow.auto.create.topics", value = "true")})
public class MqListener {

    private static final Logger log = LoggerFactory.getLogger(MqListener.class);

    @Inject
    JsonMapper jsonMapper;
    @Inject
    JsEngine jsEngine;
    @Inject
    TicketClientRepository ticketClientRepository;

    @Topic({"cloudcmd-im-jingxin-polticket"})
    @SendTo("cloudcmd-im-jingxin-polticket-result")
    public Flux<String> consumePolTicket(Mono<String> records) {
        return records//
                .doOnNext(next -> log.info("on polticket:{}", next))//
                .flatMap(str -> {
                    try {
                        return Mono.just(jsonMapper.readValue(str, PolTicket.class));
                    } catch (IOException e) {
                        return Mono.error(new RuntimeException(e));
                    }
                })//
                .flatMapMany(pt -> {
                    if (pt == null || pt.systemCode == null || pt.param == null) {
                        return Mono.error(new RuntimeException("param error"));
                    }
                    return ticketClientRepository.findBySystemCodeAndStatus(pt.systemCode, 0)//
                            .defaultIfEmpty(new TicketClient(null, null, null, null, null, 0))//
                            .flatMapMany(config -> {
                                if (config == null || config.id() == null) {
                                    return Flux.error(new RuntimeException("System not found"));
                                }
                                try {
                                    var data = jsEngine.runJs(config.id() + "", pt.param,
                                            jsonMapper.writeValueAsString(config));
                                    switch (data) {
                                        case null -> {
                                            return Flux.empty();
                                        }
                                        case Array arr -> {
                                            int length = Array.getLength(arr);
                                            Object[] array = new Object[length];
                                            for (int i = 0; i < length; i++) {
                                                array[i] = Array.get(arr, i);
                                            }
                                            return Flux.fromArray(array);
                                        }
                                        case Iterable<?> obj -> {
                                            return Flux.fromIterable(obj);
                                        }
                                        default -> {
                                            return Flux.just(data);
                                        }
                                    }
                                } catch (ScriptException | IOException e) {
                                    return Flux.error(new RuntimeException(e));
                                }
                            });
                }).flatMap(r -> {
                    try {
                        return Mono.just(jsonMapper.writeValueAsString(r));
                    } catch (IOException e) {
                        return Mono.error(new RuntimeException(e));
                    }
                }).doOnNext(str -> log.info("result:{}", str));
    }

    @Serdeable
    public record PolTicket(String systemCode, String param) {

    }
}
