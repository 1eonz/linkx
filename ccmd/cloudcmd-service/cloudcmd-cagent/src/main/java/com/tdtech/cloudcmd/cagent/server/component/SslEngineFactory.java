//package com.tdtech.cloudcmd.cagent.server.component;
//
//import java.security.KeyStore;
//
//import javax.annotation.PostConstruct;
//import javax.net.ssl.KeyManagerFactory;
//import javax.net.ssl.SSLContext;
//import javax.net.ssl.SSLEngine;
//
//import org.springframework.core.io.ClassPathResource;
//import org.springframework.stereotype.Component;
//
//import lombok.extern.slf4j.Slf4j;
//
//@Slf4j
//@Component
//public class SslEngineFactory {
//
//    private static final String P1 = "e";
//    private static final String P2 = "L";
//    private static final String P3 = "T";
//    private static final String P4 = "E";
//    private static final String P5 = "@";
//    private static final String P6 = "c";
//    private static final String P7 = "o";
//    private static final String P8 = "m";
//    private static final String P9 = "1";
//    private static final String P10 = "2";
//    private static final String P11 = "3";
//
//    private SSLContext sslContext;
//
//    @PostConstruct
//    public void postConstruct() throws Exception {
//        KeyStore keyStore = KeyStore.getInstance("PKCS12");
//        ClassPathResource classPathResource = new ClassPathResource("server.p12");
//        keyStore.load(classPathResource.getInputStream(), getSSLP().toCharArray());
//        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
//        kmf.init(keyStore, getSSLP().toCharArray());
//        sslContext = SSLContext.getInstance("TLS");
//        sslContext.init(kmf.getKeyManagers(), null, null);
//    }
//
//    public SSLEngine newSslEngine() {
//        return sslContext.createSSLEngine();
//    }
//
//    private String getSSLP() {
//        return P1 + P2 + P3 + P4 + P5 + P6 + P7 + P8 + P9 + P10 + P11;
//    }
//
//}
