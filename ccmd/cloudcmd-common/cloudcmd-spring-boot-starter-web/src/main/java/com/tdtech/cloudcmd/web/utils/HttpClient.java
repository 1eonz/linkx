package com.tdtech.cloudcmd.web.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.PreDestroy;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Stream;

@Slf4j
public class HttpClient {

    private final HttpClientConfigurationProperties properties;

    private final ObjectMapper mapper;

    private final Object initLock = new Object();
    private volatile boolean initFlag = false;

    private java.net.http.HttpClient sClient;
    private java.net.http.HttpClient client;
    private ThreadPoolTaskExecutor taskExecutor;

    public HttpClient(ObjectMapper objectMapper, HttpClientConfigurationProperties properties) {
        mapper = objectMapper;
        this.properties = properties;
    }

    public void lazyInit() {
        if (!initFlag) {
            synchronized (initLock) {
                if (!initFlag) {
                    taskExecutor = new ThreadPoolTaskExecutor();
                    // 核心线程池大小
                    taskExecutor.setCorePoolSize(properties.getCoreThreadPoolSize());
                    // 最大线程数
                    taskExecutor.setMaxPoolSize(properties.getMaxThreadPoolSize());
                    // 队列容量
                    taskExecutor.setQueueCapacity(properties.getThreadQueueSize());
                    // 活跃时间
                    taskExecutor.setKeepAliveSeconds(properties.getThreadKeepAliveSeconds());
                    // 线程名字前缀
                    taskExecutor.setThreadNamePrefix("Httpclient-worker-thread");
                    // setRejectedExecutionHandler：当pool已经达到max size的时候，如何处理新任务
                    // CallerRunsPolicy：不在新线程中执行任务，而是由调用者所在的线程来执行
                    taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
                    taskExecutor.initialize();
                    TrustManager[] trustAllCertificates = new TrustManager[] {new X509TrustManager() {
                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        @Override
                        public void checkClientTrusted(X509Certificate[] arg0, String arg1) {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] arg0, String arg1) {
                        }
                    }};
                    SSLParameters sslParams = new SSLParameters();
                    sslParams.setEndpointIdentificationAlgorithm("");
                    SSLContext ctx;
                    try {
                        ctx = SSLContext.getInstance("TLS");
                        ctx.init(null, trustAllCertificates, new SecureRandom());
                    } catch (NoSuchAlgorithmException | KeyManagementException e) {
                        throw new HttpClientException(e);
                    }
                    sClient = java.net.http.HttpClient.newBuilder().connectTimeout(properties.getConnectTimeout())
                        .executor(taskExecutor).sslParameters(sslParams).sslContext(ctx).build();
                    client = java.net.http.HttpClient.newBuilder().connectTimeout(properties.getConnectTimeout())
                        .executor(taskExecutor).build();
                    initFlag = true;
                }
            }
        }

    }

    @PreDestroy
    public void cleanUp() {
        if (taskExecutor != null) {
            taskExecutor.setWaitForTasksToCompleteOnShutdown(false);
            taskExecutor.shutdown();
        }
    }

    /**
     * get Json</br> high level api
     */
    public <T> T getJson(URI uri, Map<String, String> headers, Class<T> respType) {
        return get(uri, headers, bytes -> deserializeJson(bytes, respType));
    }

    /**
     * get Json</br> high level api
     */
    public <T> T getJson(URI uri, Map<String, String> headers, TypeReference<T> respType) {
        return get(uri, headers, bytes -> deserializeJson(bytes, respType));
    }

    /**
     * get any</br> high level api
     */
    public <T> T get(URI uri, Map<String, String> headers, Function<byte[], T> deserializer) {
        return sendRequest(uri, HttpMethodEnum.GET, headers, null, null, properties.getDefaultReadTimeOut(),
            deserializer);
    }

    /**
     * get any</br> high level api
     */
    public <T> T getWithTimeout(URI uri, Map<String, String> headers, Function<byte[], T> deserializer, Duration timeout) {
        return sendRequest(uri, HttpMethodEnum.GET, headers, null, null, timeout, deserializer);
    }

    /**
     * delete Json</br> high level api
     */
    public <T> T deleteJson(URI uri, Map<String, String> headers, Class<T> respType) {
        return delete(uri, headers, bytes -> deserializeJson(bytes, respType));
    }

    /**
     * delete any</br> high level api
     */
    public <T> T delete(URI uri, Map<String, String> headers, Function<byte[], T> deserializer) {
        return sendRequest(uri, HttpMethodEnum.DELETE, headers, null, null, properties.getDefaultReadTimeOut(),
            deserializer);
    }

    /**
     * post Json</br> high level api
     */
    public <T> T postJson(URI uri, Map<String, String> headers, Object jsonBody, Class<T> respType) {
        return post(uri, headers, HttpRequest.BodyPublishers.ofByteArray(serializeReqBody(jsonBody)),
            "application/json;charset=UTF-8", bytes -> deserializeJson(bytes, respType));
    }

    /**
     * post Json</br> high level api
     */
    public <T> T postJson(URI uri, Map<String, String> headers, Object jsonBody, TypeReference<T> respType) {
        return post(uri, headers, HttpRequest.BodyPublishers.ofByteArray(serializeReqBody(jsonBody)),
            "application/json;charset=UTF-8", bytes -> deserializeJson(bytes, respType));
    }

    /**
     * post any</br> high level api
     */
    public <T> T post(URI uri, Map<String, String> headers, HttpRequest.BodyPublisher bodyPublisher, String contentType,
        Function<byte[], T> deserializer) {
        return sendRequest(uri, HttpMethodEnum.POST, headers,
            bodyPublisher == null ? HttpRequest.BodyPublishers.noBody() : bodyPublisher, contentType,
            properties.getDefaultReadTimeOut(), deserializer);
    }

    /**
     * put Json</br> high level api
     */
    public <T> T putJson(URI uri, Map<String, String> headers, Object jsonBody, Class<T> respType) {
        return put(uri, headers, HttpRequest.BodyPublishers.ofByteArray(serializeReqBody(jsonBody)),
            "application/json;charset=UTF-8", bytes -> deserializeJson(bytes, respType));
    }

    /**
     * put any</br> high level api
     */
    public <T> T put(URI uri, Map<String, String> headers, HttpRequest.BodyPublisher bodyPublisher, String contentType,
        Function<byte[], T> deserializer) {
        return sendRequest(uri, HttpMethodEnum.PUT, headers,
            bodyPublisher == null ? HttpRequest.BodyPublishers.noBody() : bodyPublisher, contentType,
            properties.getDefaultReadTimeOut(), deserializer);
    }

    private byte[] serializeReqBody(@NotNull Object body) {
        if (body instanceof byte[]) {
            return (byte[])body;
        } else if (body instanceof String) {
            return ((String)body).getBytes(StandardCharsets.UTF_8);
        } else {
            try {
                return mapper.writeValueAsBytes(body);
            } catch (JsonProcessingException e) {
                throw new HttpClientException(e);
            }
        }
    }

    @SuppressWarnings("unchecked cast")
    private <T> T deserializeJson(byte[] source, Class<T> type) {
        if (source == null || source.length == 0) {
            return null;
        }
        if (type == byte[].class) {
            return (T)source;
        } else if (type == String.class) {
            return (T)new String(source, StandardCharsets.UTF_8);
        } else if (type == Void.class) {
            return null;
        } else {
            try {
                return mapper.readValue(source, type);
            } catch (IOException e) {
                throw new HttpClientException(e);
            }
        }
    }

    @SuppressWarnings("unchecked cast")
    private <T> T deserializeJson(byte[] source, TypeReference<T> type) {
        if (source == null || source.length == 0) {
            return null;
        }
        try {
            return mapper.readValue(source, type);
        } catch (IOException e) {
            throw new HttpClientException(e);
        }
    }

    /**
     * low level api</br> without response status check
     */
    public <T> HttpResponse<T> sendRequest(HttpRequest req, HttpResponse.BodyHandler<T> bodyHandler) {
        lazyInit();
        Objects.requireNonNull(req);
        Objects.requireNonNull(bodyHandler);
        HttpResponse<T> resp;
        CompletableFuture<HttpResponse<T>> cf = null;
        try {
            var uri = req.uri();
            var timeout = req.timeout().orElseGet(() -> Duration.ofSeconds(30L));
            log.debug("send req:{}", req);
            if (Objects.equals(uri.getScheme(), "http")) {
                cf = client.sendAsync(req, bodyHandler);
                resp = cf.get(timeout.getSeconds(), TimeUnit.SECONDS);
            } else {
                cf = sClient.sendAsync(req, bodyHandler);
                resp = cf.get(timeout.getSeconds(), TimeUnit.SECONDS);
            }
        } catch (TimeoutException | ExecutionException e) {
            throw new HttpClientException("send request error," + req, e);
        } catch (InterruptedException e) {
            if (cf != null) {
                cf.cancel(true);
            }
            Thread.currentThread().interrupt();
            throw new HttpClientException(e);
        }
        log.debug("receive resp:{}", resp);
        return resp;
    }

    /**
     * low level api</br> without response status check
     */
    public HttpResponse<byte[]> sendRequest(URI uri, HttpMethodEnum method, Map<String, String> headers,
        HttpRequest.BodyPublisher bodyPublisher, String contentType, Duration readTimeout) {
        Objects.requireNonNull(method);
        var req = buildRequest(uri, method, headers, bodyPublisher, readTimeout, contentType);
        return sendRequest(req, HttpResponse.BodyHandlers.ofByteArray());
    }

    /**
     * low level 2 api</br> without response status check
     */
    public HttpResponse<byte[]> sendJsonRequest(URI uri, HttpMethodEnum method, Map<String, String> headers,
        Object requestBody, Duration readTimeout) {
        Objects.requireNonNull(method);
        HttpRequest.BodyPublisher bodyPublisher = requestBody == null ? HttpRequest.BodyPublishers.noBody()
            : HttpRequest.BodyPublishers.ofByteArray(serializeReqBody(requestBody));
        return sendRequest(uri, method, headers, bodyPublisher, "application/json;charset=UTF-8", readTimeout);
    }

    private <T> T sendRequest(URI uri, HttpMethodEnum method, Map<String, String> headers,
        HttpRequest.BodyPublisher bodyPublisher, String contentType, Duration readTimeout,
        Function<byte[], T> deserializer) {
        var resp = sendRequest(uri, method, headers, bodyPublisher, contentType, readTimeout);
        return checkResponseAndDeserialize(resp, deserializer);
    }

    private HttpRequest buildRequest(URI uri, HttpMethodEnum method, Map<String, String> headers,
        HttpRequest.BodyPublisher bodyPublisher, Duration readTimeout, String contentType) {
        var builder = HttpRequest.newBuilder().uri(uri);
        if (headers != null && !headers.isEmpty()) {
            headers.forEach((k, v) -> {
                if (k != null && v != null) {
                    builder.setHeader(k, v);
                }
            });
        }
        switch (method) {
            case GET: {
                builder.GET();
                break;
            }
            case DELETE: {
                if (bodyPublisher == null) {
                    builder.DELETE();
                } else {
                    var delete = builder.method("DELETE", bodyPublisher);
                    if (contentType != null && !contentType.isBlank()) {
                        delete.setHeader("content-type", contentType);
                    }
                }
                break;
            }
            case PUT: {
                var put = builder.PUT(bodyPublisher);
                if (contentType != null && !contentType.isBlank()) {
                    put.setHeader("content-type", contentType);
                }
                break;
            }
            case POST: {
                var post = builder.POST(bodyPublisher);
                if (contentType != null && !contentType.isBlank()) {
                    post.setHeader("content-type", contentType);
                }
                break;
            }
            default: {
                throw new UnsupportedOperationException();
            }
        }
        builder.timeout(readTimeout);
        return builder.build();
    }

    private <T> T checkResponseAndDeserialize(HttpResponse<byte[]> response, Function<byte[], T> deserializer) {
        checkResponseStatus(response);
        return deserializer.apply(response.body());
    }

    /************************ static utility methods **************************/
    @SneakyThrows
    public static URI buildUri(String path, Map<String, ?> param) {
        if (param == null || param.isEmpty()) {
            return new URI(path);
        }
        var stringBuilder = new StringBuilder();
        param.forEach((k, v) -> {
            if (v != null) {
                stringBuilder.append(URLEncoder.encode(k, StandardCharsets.UTF_8)).append('=')
                    .append(URLEncoder.encode(v.toString(), StandardCharsets.UTF_8)).append('&');
            }
        });
        if (stringBuilder.length() > 0) {
            stringBuilder.delete(stringBuilder.length() - 1, stringBuilder.length());
            var uri = path + "?" + stringBuilder;
            return new URI(uri);
        } else {
            return new URI(path);
        }
    }

    public static Stream<HttpCookie> readCookie(HttpResponse<?> resp) {
        return Stream.of(resp.headers().allValues("Set-Cookie2"), resp.headers().allValues("Set-Cookie"))
            .filter(Objects::nonNull).flatMap(Collection::stream).filter(a -> a != null && !a.isBlank())
            .map(HttpCookie::parse).flatMap(Collection::stream);
    }

    public static void checkResponseStatus(HttpResponse<byte[]> resp) {
        var httpStatus = HttpStatus.valueOf(resp.statusCode());
        if (httpStatus.is4xxClientError() || httpStatus.is5xxServerError() || httpStatus.is3xxRedirection()
            || httpStatus.is1xxInformational()) {
            throw new HttpStatusException(HttpStatus.valueOf(resp.statusCode()),
                "req method:" + resp.request().method() + "req headers:" + resp.request().headers()
                    .map() + "uri:" + resp.uri() + " status:" + resp.statusCode() + " body:" + new String(resp.body(),
                    StandardCharsets.UTF_8));
        }
    }

    /************************ inner class **************************/
    public static class HttpClientException extends RuntimeException {
        public HttpClientException(String message) {
            super(message);
        }

        public HttpClientException(String message, Throwable cause) {
            super(message, cause);
        }

        public HttpClientException(Throwable cause) {
            super(cause);
        }
    }

    @Getter
    public static class HttpStatusException extends HttpClientException {

        private final HttpStatus status;

        public HttpStatusException(HttpStatus status, String body) {
            super(body);
            this.status = status;
        }

    }

    public enum HttpMethodEnum {
        GET, POST, PUT, DELETE
    }
}
