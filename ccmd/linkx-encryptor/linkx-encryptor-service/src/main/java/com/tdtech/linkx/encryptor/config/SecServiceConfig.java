package com.tdtech.linkx.encryptor.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sonicom.scm.api.ISecService;
import org.sonicom.scm.api.impl.SecService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 国密服务配置类
 * 
 * 配置 SdkSM 的 ISecService Bean
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SecServiceConfig {

    private final SdkProperties sdkProperties;

    /**
     * 初始化 SDK 环境变量
     */
    @PostConstruct
    public void initSdkEnvironment() {
        // 设置必需的环境变量
        System.setProperty("ENV", sdkProperties.getEnv());
        System.setProperty("SM_HOST", sdkProperties.getSm().getHost());
        System.setProperty("SM_SECRET", sdkProperties.getSm().getSecret());
        
        // 设置可选的环境变量
        if (sdkProperties.getHttpConnector() != null) {
            System.setProperty("HTTP_CONNECTOR", sdkProperties.getHttpConnector().toString());
        }
        // 注意：SDK 配置加载器按「环境变量名小写 + 下划线转点」映射配置键，
        // bz.server.id 对应 BZ_SERVER_ID（三段），而非 BZ_SERVERID
        if (sdkProperties.getBzServerId() != null) {
            System.setProperty("BZ_SERVER_ID", sdkProperties.getBzServerId());
        }
        if (sdkProperties.getBzDeviceId() != null) {
            System.setProperty("BZ_DEVICE_ID", sdkProperties.getBzDeviceId());
        }

        // 从环境变量读取并输出所有设置的环境变量
        log.info("SDK 环境变量初始化完成:");
        log.info("  ENV: {}", System.getProperty("ENV"));
        log.info("  SM_HOST: {}", System.getProperty("SM_HOST"));
        log.info("  SM_SECRET: {}", System.getProperty("SM_SECRET"));
        log.info("  HTTP_CONNECTOR: {}", System.getProperty("HTTP_CONNECTOR"));
        log.info("  BZ_SERVER_ID: {}", System.getProperty("BZ_SERVER_ID"));
        log.info("  BZ_DEVICE_ID: {}", System.getProperty("BZ_DEVICE_ID"));
    }

    /**
     * 创建 ISecService Bean
     * 
     * ISecService 是 SdkSM 库提供的国密算法服务接口
     * 实现类为 SecService
     * 
     * @return ISecService 实例
     */
    @Bean
    public ISecService secService() {
        log.info("初始化国密服务 ISecService");
        return new SecService();
    }
}
