package com.tdtech.cloudcmd.script.controller;

import com.tdtech.cloudcmd.script.engine.JsEngine;
import com.tdtech.cloudcmd.script.repo.TicketClient;
import com.tdtech.cloudcmd.script.repo.TicketClientRepository;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.script.ScriptException;

@Controller("/script")
public class ScriptController {

    private static final Logger log = LoggerFactory.getLogger(ScriptController.class);

    @Inject
    JsEngine jsEngine;
    @Inject
    TicketClientRepository ticketClientRepository;

    @Post("/check")
    public Mono<Boolean> checkScript(@Body Mono<String> scriptMono){
        return scriptMono.map(jsEngine::checkScript);
    }

    @Post("/load")
    public Mono<Void> loadScript(@Body Mono<Long> idPub) {
        return idPub//
                .flatMap(id -> ticketClientRepository.findByIdAndStatus(id, 0)//
                        .defaultIfEmpty(new TicketClient(id, null, null, null, null, 0)))//
                .flatMap(config -> {
                    try {
                        jsEngine.loadJs(config.id() + "", config.script());
                        return Mono.empty();
                    } catch (ScriptException e) {
                        return Mono.error(new RuntimeException(e));
                    }
                })//
                .then();
    }


    @Post("/try")
    public Mono<Object> tryScript(@Body Mono<TryParam> bodyPub) {
        return bodyPub//
                .flatMap(tp -> {
                    try {
                        return Mono.just(jsEngine.tryJs(tp.script,tp.conf, tp.params));
                    } catch (ScriptException e) {
                        return Mono.error(new RuntimeException(e));
                    }
                });
    }

    @Serdeable
    public record TryParam(String script,String conf, String params) {

    }
}
