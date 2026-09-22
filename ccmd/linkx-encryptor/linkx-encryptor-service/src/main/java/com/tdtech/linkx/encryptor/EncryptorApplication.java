package com.tdtech.linkx.encryptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * LinkX 加密服务启动类
 * <p>
 * 提供 SM2/SM4 国密算法加解密服务
 */
@Slf4j
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class EncryptorApplication {

    public static void main(String[] args) {
        // 启动应用
        SpringApplication.run(EncryptorApplication.class, args);
    }
}
