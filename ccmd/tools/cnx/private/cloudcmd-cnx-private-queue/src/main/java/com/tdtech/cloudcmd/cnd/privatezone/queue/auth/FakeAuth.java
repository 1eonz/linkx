package com.tdtech.cloudcmd.cnd.privatezone.queue.auth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import io.lettuce.core.api.StatefulRedisConnection;
import io.micronaut.json.JsonMapper;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import reactor.core.publisher.Mono;

@Singleton
public class FakeAuth {

    private static final String TOKEN_KEY = "icp-x:auth:token:{access_token}";

    private static final String ACCESS_TOKEN_TIMEOUT_KEY = "icp-x:auth:timeout:token:{access_token}";

    @Inject
    @Named("default")
    StatefulRedisConnection<byte[], byte[]> statefulRedisConnection;
    @Inject
    JsonMapper jsonMapper;

    public Mono<UserInfo> auth(Mono<String> token) {
        return token.flatMap(t -> {
            return getUser(TOKEN_KEY, t)//
                .switchIfEmpty(Mono.defer(() -> getUser(ACCESS_TOKEN_TIMEOUT_KEY, t)))//
                .map(bs -> {
                    try {
                        return jsonMapper.readValue(bs, UserInfo.class);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        });
    }

    private Mono<byte[]> getUser(String key, String t) {
        return statefulRedisConnection.reactive()
            .get(key.replace("{access_token}", t).getBytes(StandardCharsets.UTF_8));
    }

}
