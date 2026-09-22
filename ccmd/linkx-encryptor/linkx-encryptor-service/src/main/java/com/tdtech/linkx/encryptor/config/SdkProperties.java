package com.tdtech.linkx.encryptor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SDK 配置属性
 * 
 * 支持通过配置文件或环境变量配置 SDK 参数
 */
@Data
@Component
@ConfigurationProperties(prefix = "sdk")
public class SdkProperties {

    /**
     * 环境
     */
    private String env = "prod";

    /**
     * SM 服务配置
     */
    private SmConfig sm = new SmConfig();
    
    /**
     * HTTP 连接器开关
     */
    private Boolean httpConnector = false;
    
    /**
     * BZ 服务 ID
     */
    private String bzServerId = "bz_server.oo";

    /**
     * BZ 设备 ID
     */
    private String bzDeviceId = "1A-2B-3C-4D-5E-6F";
    
    /**
     * 加密服务类型：SDK 或 BOUNCYCASTLE
     */
    private String encryptType = "SDK";
    
    /**
     * SM4 密钥（16字节/128位）
     * 用于对称加密
     */
    private String sm4Key = "1234567890123456";
    
    /**
     * SM2 公钥（Base64编码）
     * 用于非对称加密
     */
    private String sm2PublicKey;
    
    /**
     * SM2 私钥（Base64编码）
     * 用于非对称解密
     */
    private String sm2PrivateKey;
    
    @Data
    public static class SmConfig {
        /**
         * SM 服务地址
         */
        private String host = "http://docker.scm";
        
        /**
         * SM 密钥
         */
        private String secret = "secret_dev";
    }
}
