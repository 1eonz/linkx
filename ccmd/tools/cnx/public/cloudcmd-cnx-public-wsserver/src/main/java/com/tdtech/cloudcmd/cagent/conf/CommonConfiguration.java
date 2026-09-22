package com.tdtech.cloudcmd.cagent.conf;

import java.net.HttpURLConnection;
import java.security.KeyStore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class CommonConfiguration {

    private static final int READ_TIMEOUT = 10 * 1000;
    private static final int CONNECT_TIMEOUT = 5 * 1000;

    @Bean
    @ConditionalOnMissingBean(RestTemplate.class)
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) {
                try {
                    if (!(connection instanceof HttpsURLConnection)) {
                        super.prepareConnection(connection, httpMethod);
                    }
                    if (connection instanceof HttpsURLConnection) {
                        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                        TrustStrategy anyTrustStrategy = (x509Certificates, str) -> true;
                        SSLContext ctx = SSLContexts.custom().loadTrustMaterial(trustStore, anyTrustStrategy).build();
                        ((HttpsURLConnection)connection).setSSLSocketFactory(ctx.getSocketFactory());
                        HttpsURLConnection httpsConnection = (HttpsURLConnection)connection;
                        httpsConnection.setHostnameVerifier(NoopHostnameVerifier.INSTANCE);
                        httpsConnection.setReadTimeout(READ_TIMEOUT);
                        httpsConnection.setConnectTimeout(CONNECT_TIMEOUT);
                        super.prepareConnection(httpsConnection, httpMethod);
                    }
                } catch (Exception e) {
                    log.info("prepareConnection->Exception:{}", e.getMessage());
                }
            }
        };
        factory.setOutputStreaming(false);
        return new RestTemplate(factory);
    }

    @Bean(destroyMethod = "shutdownNow")
    public ExecutorService workPool() {
        return new ThreadPoolExecutor(10, 200, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(10),
            new ThreadPoolExecutor.CallerRunsPolicy());
    }
}
