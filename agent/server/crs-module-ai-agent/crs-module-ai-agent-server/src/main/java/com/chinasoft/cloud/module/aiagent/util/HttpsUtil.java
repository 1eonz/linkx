package com.chinasoft.cloud.module.aiagent.util;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okio.BufferedSource;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
public class HttpsUtil {
    public static final OkHttpClient client = new OkHttpClient.Builder().callTimeout(5L, TimeUnit.MINUTES)
            .readTimeout(5L, TimeUnit.MINUTES).connectTimeout(Duration.ofSeconds(10L)).hostnameVerifier((hostname, session) -> true)   // 放过域名校验
            .sslSocketFactory(createTrustAllSSLFactory(), new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }).build();

    private static SSLSocketFactory createTrustAllSSLFactory() {
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, new TrustManager[]{new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }}, new SecureRandom());
            return ctx.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String get(String url) {
        return get(url, null);
    }

    public static String get(String url, String token) {
        var builder = new Request.Builder().url(url).get();
        if (token != null) {
            builder.addHeader("Authorization", token);
        }
        var request = builder.build();
        log.info("get url: {}", url);
        try (Response response = client.newCall(request).execute()) {
            log.info("get response: {}", response);
            if (!response.isSuccessful()) {
                throw new IOException("get error code " + response);
            }
            var responseBody = response.body();
            if (responseBody == null) {
                throw new IOException("get response body is null");
            }
            BufferedSource source = responseBody.source();
            StringBuilder sb = new StringBuilder();
            while (!source.exhausted()) {
                String line = source.readUtf8Line();
                if (line != null) {
                    sb.append(line);
                }
            }
            log.info("get resp: {}", sb);
            return sb.toString();
        } catch (Exception e) {
            log.error("get error:{} ", e.getMessage());
        }
        return "";
    }

    public static String post(String url, String paramJson) {
        return post(url, paramJson, null);
    }

    public static String post(String url, String paramJson, String token) {
        RequestBody requestBody = RequestBody.create(paramJson, MediaType.parse("application/json; charset=utf-8"));
        var builder = new Request.Builder().url(url).post(requestBody);
        if (token != null) {
            builder.addHeader("Authorization", token);
        }
        var request = builder.build();
        log.info("post url: {}, paramJson: {}", url, paramJson);
        try (Response response = client.newCall(request).execute()) {
            log.info("post response: {}", response);
            if (!response.isSuccessful()) {
                throw new IOException("post error code " + response);
            }
            var responseBody = response.body();
            BufferedSource source = responseBody.source();
            StringBuilder sb = new StringBuilder();
            while (!source.exhausted()) {
                String line = source.readUtf8Line();
                if (line != null) {
                    sb.append(line);
                }
            }
            log.info("post resp: {}", sb);
            return sb.toString();
        } catch (Exception e) {
            log.error("post error:{} ", e.getMessage());
        }
        return "";
    }

    public static String put(String url, String paramJson) {
        return put(url, paramJson, null);
    }

    public static String put(String url, String paramJson, String token) {
        RequestBody requestBody = RequestBody.create(paramJson, MediaType.parse("application/json; charset=utf-8"));
        var builder = new Request.Builder().url(url).put(requestBody);
        if (token != null) {
            builder.addHeader("Authorization", token);
        }
        var request = builder.build();
        log.info("put url: {}, paramJson: {}", url, paramJson);
        try (Response response = client.newCall(request).execute()) {
            log.info("put response: {}", response);
            if (!response.isSuccessful()) {
                throw new IOException("put error code " + response);
            }
            var responseBody = response.body();
            BufferedSource source = responseBody.source();
            StringBuilder sb = new StringBuilder();
            while (!source.exhausted()) {
                String line = source.readUtf8Line();
                if (line != null) {
                    sb.append(line);
                }
            }
            log.info("put resp: {}", sb);
            return sb.toString();
        } catch (Exception e) {
            log.error("post error:{} ", e.getMessage());
        }
        return "";
    }

    public static String delete(String url) {
        var request = new Request.Builder().url(url).delete().build();
        log.info("delete url: {}", url);
        try (Response response = client.newCall(request).execute()) {
            log.info("get response: {}", response);
            if (!response.isSuccessful()) {
                throw new IOException("delete error code " + response);
            }
            var responseBody = response.body();
            BufferedSource source = responseBody.source();
            StringBuilder sb = new StringBuilder();
            while (!source.exhausted()) {
                String line = source.readUtf8Line();
                if (line != null) {
                    sb.append(line);
                }
            }
            log.info("delete resp: {}", sb);
            return sb.toString();
        } catch (Exception e) {
            log.error("delete error:{} ", e.getMessage());
        }
        return "";
    }

}
