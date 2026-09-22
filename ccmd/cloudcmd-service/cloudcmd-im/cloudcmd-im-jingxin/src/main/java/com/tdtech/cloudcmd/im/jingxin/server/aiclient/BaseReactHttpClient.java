package com.tdtech.cloudcmd.im.jingxin.server.aiclient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tdtech.cloudcmd.im.jingxin.client.NamedLogger;
import com.tdtech.cloudcmd.util.json.JsonUtil;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.Map;

@Slf4j
public abstract class BaseReactHttpClient implements NamedLogger {

    private final WebClient webClient;

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    /**
     * 构造函数，初始化 WebClient 实例 配置默认 Content-Type 为 application/json 设置连接超时为 10 秒
     */
    @SneakyThrows
    public BaseReactHttpClient() {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 设置为1MB
            .build();
        // 创建 SSL 上下文
        SslContext sslContext =
            SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        webClient = WebClient.builder()//
            .exchangeStrategies(strategies).defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .clientConnector(new ReactorClientHttpConnector(HttpClient.create()//
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)//
                .responseTimeout(Duration.ofSeconds(120))//
                .secure(sslContextSpec -> sslContextSpec.sslContext(sslContext))//

            )).build();
    }

    /**
     * 异步发送 POST 请求到指定 URI
     *
     * @param <T>      响应体类型
     * @param uri      目标 URI
     * @param headers  请求头映射
     * @param reqBody  请求体对象
     * @param respType 响应体类型 Class 对象
     * @return Mono<T> 响应式结果
     */
    public <T> Mono<T> postJsonAsync(String uri, Map<String, String> headers, Object reqBody,
        TypeReference<T> respType) {
        return webClient.post().uri(uri).bodyValue(reqBody).headers(httpHeaders -> {
                // 设置请求头
                if (headers != null && !headers.isEmpty()) {
                    for (var entry : headers.entrySet()) {
                        if (entry.getKey() != null && entry.getValue() != null) {
                            httpHeaders.set(entry.getKey(), entry.getValue());
                        } else {
                            log("error", "header cant be null!!->{} for req:{}", headers, uri);
                        }
                    }
                }
            }).retrieve()
            // 处理非 2xx 状态码的错误响应
            .onStatus(status -> !status.is2xxSuccessful(), resp -> resp.bodyToMono(String.class).map(body -> {
                log("error", "response error req:{} {} {} {} {}", uri, headers, reqBody, resp.statusCode(), body);
                return new RequestException("req error:" + resp.statusCode() + ":" + body);
            })).bodyToMono(String.class)//
            .doOnSuccess(respBody -> log("info", "req succeed:{} {} {} resp:{}", uri, headers, reqBody, respBody))//交互日志
            .flatMap(respBody -> Mono.justOrEmpty(JsonUtil.parseJson(respBody, respType)));
    }

    public Mono<String> postAsync(String uri, Map<String, String> headers, Object reqBody) {
        return webClient.post().uri(uri).bodyValue(reqBody).headers(httpHeaders -> {
                // 设置请求头
                if (headers != null && !headers.isEmpty()) {
                    for (var entry : headers.entrySet()) {
                        if (entry.getKey() != null && entry.getValue() != null) {
                            httpHeaders.set(entry.getKey(), entry.getValue());
                        } else {
                            log("error", "header cant be null!!->{} for req:{}", headers, uri);
                        }
                    }
                }
            }).retrieve()
            // 处理非 2xx 状态码的错误响应
            .onStatus(status -> !status.is2xxSuccessful(), resp -> resp.bodyToMono(String.class).map(body -> {
                log("error", "response error req:{} {} {} {} {}", uri, headers, reqBody, resp.statusCode(), body);
                return new RequestException("req error:" + resp.statusCode() + ":" + body);
            })).bodyToMono(String.class)//
            .doOnSuccess(
                respBody -> log("info", "req succeed:{} {} {} resp:{}", uri, headers, reqBody, respBody));//交互日志
    }

    /**
     * 异步发送 POST 请求到指定 URI
     *
     * @param <T>      响应体类型
     * @param uri      目标 URI
     * @param headers  请求头映射
     * @param respType 响应体类型 Class 对象
     * @return Mono<T> 响应式结果
     */
    public <T> Mono<T> getJsonAsync(String uri, Map<String, String> headers, TypeReference<T> respType) {
        return webClient.get().uri(uri).headers(httpHeaders -> {
                // 设置请求头
                if (headers != null && !headers.isEmpty()) {
                    for (var entry : headers.entrySet()) {
                        if (entry.getKey() != null && entry.getValue() != null) {
                            httpHeaders.set(entry.getKey(), entry.getValue());
                        } else {
                            log("error", "header cant be null!!->{} for req:{}", headers, uri);
                        }
                    }
                }
            }).retrieve()
            // 处理非 2xx 状态码的错误响应
            .onStatus(status -> !status.is2xxSuccessful(), resp -> resp.bodyToMono(String.class).map(body -> {
                log("error", "response error req:{} {} {} {}", uri, headers, resp.statusCode(), body);
                return new RequestException("req error:" + resp.statusCode() + ":" + body);
            })).bodyToMono(String.class)//
            .doOnSuccess(respBody -> log("info", "req succeed:{} {} resp:{}", uri, headers, respBody))//交互日志
            .flatMap(respBody -> Mono.justOrEmpty(JsonUtil.parseJson(respBody, respType)));
    }

    public <T> T postJson(String uri, Map<String, String> headers, Object reqBody, TypeReference<T> respType) {
        return webClient.post().uri(uri).headers(httpHeaders -> {
                if (headers != null) {
                    headers.forEach((k, v) -> {
                        if (k != null && v != null) {
                            httpHeaders.set(k, v);
                        } else {
                            log("error", "header cant be null!!->{} for req:{}", headers, uri);
                        }
                    });
                }
            }).bodyValue(reqBody).retrieve()
            .onStatus(status -> !status.is2xxSuccessful(), resp -> resp.bodyToMono(String.class).map(body -> {
                log("error", "response error req:{} {} {} {}", uri, headers, resp.statusCode(), body);
                throw new RequestException("req error:" + resp.statusCode() + ":" + body);
            })).bodyToMono(String.class)
            .doOnSuccess(respBody -> log("info", "req succeed:{} {} {} resp:{}", uri, headers, reqBody, respBody))
            .map(respBody -> JsonUtil.parseJson(respBody, respType)).block();   // <-- 同步阻塞
    }

    public static class RequestException extends RuntimeException {
        public RequestException(String message) {
            super(message);
        }
    }
}
