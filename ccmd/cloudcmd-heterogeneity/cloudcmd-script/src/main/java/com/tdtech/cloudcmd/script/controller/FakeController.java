package com.tdtech.cloudcmd.script.controller;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.serde.annotation.Serdeable;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class FakeController {

    private static final Logger log = LoggerFactory.getLogger(FakeController.class);
    private static final SecureRandom SECURE_RANDO = new SecureRandom();

    @Post("/fake")
    public Map<?, ?> post(HttpRequest<String> request, @Body String body) {
        log.info("port:{} {}", request.getUri(), body);
        var id = SECURE_RANDO.nextLong();
        return Map.of("code", 0, "data",
            new PoliceTicket(id, "tag" + id, "name" + id, "code" + id, "source" + id,
                "content" + id, new Date(), "dispatcher" + id));

    }

    @Serdeable
    public record PoliceTicket(Long id,//
                               String tag,//
                               String name, //
                               String code,//
                               String source, //
                               String content,//
                               Date createTime, //
                               String dispatcher) {

    }
}
