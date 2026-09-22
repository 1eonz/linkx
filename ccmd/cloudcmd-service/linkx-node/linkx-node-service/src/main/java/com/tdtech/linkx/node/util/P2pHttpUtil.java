package com.tdtech.linkx.node.util;

import com.tdtech.cloudcmd.util.json.JsonUtil;
import com.tdtech.linkx.node.dto.ProxyResponse;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Map;

/**
 * P2P 节点间 HTTP 通信工具类。
 * 所有方法均不抛异常，业务方按返回值或日志判断成功与否。
 *
 * SSL 策略：P2P 节点间走 APISIX 自签证书，HttpClient 信任所有证书，
 *           配合 JVM 参数 -Djdk.internal.httpclient.disableHostnameVerification=true 禁用主机名校验。
 *           安全性由 P2P 网络的 IP 白名单 + JWT 机制保证，不依赖 TLS 证书。
 */
@Slf4j
public class P2pHttpUtil {

    private static final HttpClient HTTP_CLIENT = buildTrustAllHttpClient();

    private static HttpClient buildTrustAllHttpClient() {
        try {
            X509TrustManager trustAllManager = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    // 信任所有客户端证书
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    // 信任所有服务端证书
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{trustAllManager}, new java.security.SecureRandom());
            return HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .sslContext(sslContext)
                    .build();
        } catch (Exception e) {
            log.error("buildTrustAllHttpClient: failed, fallback to default HttpClient", e);
            return HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
        }
    }

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private P2pHttpUtil() {
    }

    public static void postJson(String url, Object body, String logLabel) {
        sendJson("POST", url, body, logLabel);
    }

    public static void delete(String url, String logLabel) {
        sendJson("DELETE", url, null, logLabel);
    }

    public static String postJsonForResult(String url, Object body, String logLabel) {
        HttpResponse<String> response = sendJson("POST", url, body, logLabel);
        return (response != null && response.statusCode() == 200) ? response.body() : null;
    }

    public static String getJsonForResult(String url, String logLabel) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .timeout(REQUEST_TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                log.info("{}: success, url={}", logLabel, url);
                return response.body();
            }
            log.warn("{}: failed, url={}, statusCode={}, body={}", logLabel, url, response.statusCode(), response.body());
            return null;
        } catch (Exception e) {
            log.warn("{}: exception, url={}", logLabel, url, e);
            return null;
        }
    }

    /**
     * 透传 GET 请求，返回完整响应（状态码 + 响应体 + 响应头）。
     * 用于 dispatch 调对端 proxy。
     *
     * @param url         目标 URL（含 querystring）
     * @param headers     附加请求头（如 Authorization: Bearer <p2p-jwt>）
     * @param logLabel    日志标识
     * @return 响应对象（含 statusCode/body/headers），异常时返回 null
     */
    public static ProxyResponse getForProxy(String url, Map<String, String> headers, String logLabel) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .GET();
            if (headers != null) {
                headers.forEach(builder::header);
            }
            HttpRequest request = builder.build();
            HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
            log.info("{}: completed, url={}, statusCode={}", logLabel, url, response.statusCode());
            return new ProxyResponse(response.statusCode(), response.body(), response.headers().map());
        } catch (Exception e) {
            log.warn("{}: exception, url={}", logLabel, url, e);
            return null;
        }
    }

    private static HttpResponse<String> sendJson(String method, String url, Object body, String logLabel) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(REQUEST_TIMEOUT);

            String json = body != null ? JsonUtil.toJsonStr(body) : "";
            HttpRequest.BodyPublisher publisher = body != null
                    ? HttpRequest.BodyPublishers.ofString(json)
                    : HttpRequest.BodyPublishers.noBody();

            switch (method) {
                case "POST":
                    builder.POST(publisher);
                    break;
                case "PUT":
                    builder.PUT(publisher);
                    break;
                case "DELETE":
                    builder.method("DELETE", publisher);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported method: " + method);
            }

            HttpRequest request = builder.build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                log.info("{}: success, url={}", logLabel, url);
            } else {
                log.warn("{}: failed, url={}, statusCode={}, body={}", logLabel, url, response.statusCode(), response.body());
            }
            return response;
        } catch (Exception e) {
            log.warn("{}: exception, url={}", logLabel, url, e);
            return null;
        }
    }
}
