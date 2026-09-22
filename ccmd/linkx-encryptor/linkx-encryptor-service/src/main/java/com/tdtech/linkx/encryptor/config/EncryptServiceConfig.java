package com.tdtech.linkx.encryptor.config;

import com.tdtech.linkx.encryptor.service.IEncryptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 加密服务配置类
 * 
 * 根据配置动态选择加密服务实现：
 * - SDK：使用 SdkSM 的 ISecService（国密算法，生产环境）
 * - BOUNCYCASTLE：使用 BouncyCastle 开源库（国密算法，开发/测试环境）
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class EncryptServiceConfig {

    private final SdkProperties sdkProperties;
    private final IEncryptService sdkEncryptService;
    private final IEncryptService bouncyCastleEncryptService;

    /**
     * 动态选择加密服务实现
     * 
     * @return 加密服务实例
     */
    @Bean
    public IEncryptService encryptService() {
        String encryptType = sdkProperties.getEncryptType().toUpperCase();
        
        log.info("初始化加密服务，类型: {}", encryptType);
        
        if ("BOUNCYCASTLE".equals(encryptType)) {
            log.info("使用 BouncyCastle 国密加密服务（开源实现，支持文本和文件流式）");
            return bouncyCastleEncryptService;
        } else {
            log.info("使用 SDK 国密加密服务（生产环境，支持文本和文件流式）");
            return sdkEncryptService;
        }
    }
}
