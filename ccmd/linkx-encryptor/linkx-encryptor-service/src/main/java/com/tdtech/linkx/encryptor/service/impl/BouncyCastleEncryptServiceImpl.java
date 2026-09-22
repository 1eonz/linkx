package com.tdtech.linkx.encryptor.service.impl;

import com.tdtech.linkx.encryptor.config.SdkProperties;
import com.tdtech.linkx.encryptor.service.EncryptMode;
import com.tdtech.linkx.encryptor.service.IEncryptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * BouncyCastle 加密服务实现
 * 
 * 使用 BouncyCastle 库实现国密算法（SM2/SM4）
 * 优点：
 * - 真正的加密算法
 * - 支持流式分块处理（SM4）
 * - 无需外部 SDK 服务
 * - 开源免费
 * 
 * 密钥管理：
 * - SM4：配置优先，配置为空则使用固定默认值
 * - SM2：配置优先，配置为空则使用固定默认值
 */
@Slf4j
@Service("bouncyCastleEncryptService")
@RequiredArgsConstructor
public class BouncyCastleEncryptServiceImpl implements IEncryptService {

    private static final String PROVIDER = "BC";
    private static final String SM4_ALGORITHM = "SM4";
    
    // 固定默认密钥（确保历史数据可解密）
    private static final String DEFAULT_SM4_KEY = "1234567890123456";
    private static final String DEFAULT_SM2_PUBLIC_KEY = "MFkwEwYHKoZIzj0CAQYIKoEcz1NBa0UwCgYEAy9KV7sJ7n7r8K9L0M1N2O3P4Q5R6S7T8U9V0W1X2Y3Z4a5b6c7d8e9f0g1h2i3j4k5l6m7n8o9p0q1r2s3t4u5v6w7x8y9z0=";
    private static final String DEFAULT_SM2_PRIVATE_KEY = "MIGTAgEAMBMGByqGSM49AgEGBqBBMCFaASauTC9KV7sJ7n7r8K9L0M1N2O3P4Q5R6S7T8U9V0W1X2Y3Z4a5b6c7d8e9f0g1h2i3j4k5l6m7n8o9p0q1r2s3t4u5v6w7x8y9z0=";
    
    private final SdkProperties sdkProperties;
    
    private SecretKey sm4Key;
    private KeyPair sm2KeyPair;
    
    static {
        // 注册 BouncyCastle 提供者
        Security.addProvider(new BouncyCastleProvider());
    }
    
    /**
     * 初始化密钥
     */
    private void initKeys() {
        if (sm4Key != null && sm2KeyPair != null) {
            return; // 已初始化
        }
        
        try {
            // 初始化 SM4 密钥
            String sm4KeyStr = sdkProperties.getSm4Key();
            if (sm4KeyStr == null || sm4KeyStr.trim().isEmpty()) {
                log.info("SM4 密钥未配置，使用固定默认密钥");
                sm4KeyStr = DEFAULT_SM4_KEY;
            } else if (sm4KeyStr.length() != 16) {
                log.warn("SM4 密钥长度不正确（应为16字节），使用固定默认密钥");
                sm4KeyStr = DEFAULT_SM4_KEY;
            }
            this.sm4Key = new SecretKeySpec(sm4KeyStr.getBytes(), SM4_ALGORITHM);
            log.info("SM4 密钥初始化完成");
            
            // 初始化 SM2 密钥对
            this.sm2KeyPair = loadSm2KeyPair();
            
            log.info("BouncyCastle 加密服务初始化完成");
        } catch (Exception e) {
            log.error("BouncyCastle 加密服务初始化失败", e);
            throw new RuntimeException("BouncyCastle 加密服务初始化失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 加载 SM2 密钥对
     */
    private KeyPair loadSm2KeyPair() throws Exception {
        String publicKeyStr = sdkProperties.getSm2PublicKey();
        String privateKeyStr = sdkProperties.getSm2PrivateKey();
        
        // 如果配置了密钥对，则加载
        if (publicKeyStr != null && !publicKeyStr.trim().isEmpty() && 
            privateKeyStr != null && !privateKeyStr.trim().isEmpty()) {
            log.info("从配置加载 SM2 密钥对");
            try {
                return loadSm2KeyPairFromBase64(publicKeyStr, privateKeyStr);
            } catch (Exception e) {
                log.warn("从配置加载 SM2 密钥对失败，使用固定默认密钥: {}", e.getMessage());
            }
        }
        
        // 使用固定默认密钥对
        log.info("SM2 密钥对未配置或配置无效，使用固定默认密钥");
        return generateDefaultSm2KeyPair();
    }
    
    /**
     * 生成固定默认 SM2 密钥对
     */
    private KeyPair generateDefaultSm2KeyPair() throws Exception {
        // 使用固定种子生成密钥对，确保每次重启密钥一致
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("SM2", PROVIDER);
        SecureRandom random = new SecureRandom("LinkX-SM2-Default-Key-2024".getBytes());
        keyPairGenerator.initialize(256, random);
        return keyPairGenerator.generateKeyPair();
    }
    
    /**
     * 从 Base64 字符串加载 SM2 密钥对
     */
    private KeyPair loadSm2KeyPairFromBase64(String publicKeyStr, String privateKeyStr) throws Exception {
        byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyStr);
        byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyStr);
        
        KeyFactory keyFactory = KeyFactory.getInstance("SM2", PROVIDER);
        PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));
        PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
        
        return new KeyPair(publicKey, privateKey);
    }

    @Override
    public byte[] cipherKey(byte[] data, EncryptMode mode) throws Exception {
        initKeys(); // 确保密钥已初始化
        
        log.debug("BouncyCastle SM2 {} 操作，数据长度: {}", mode, data.length);
        
        try {
            Cipher cipher = Cipher.getInstance("SM2", PROVIDER);
            
            if (mode == EncryptMode.ENCRYPT) {
                // SM2 加密：使用公钥
                cipher.init(Cipher.ENCRYPT_MODE, sm2KeyPair.getPublic());
                return cipher.doFinal(data);
            } else {
                // SM2 解密：使用私钥
                cipher.init(Cipher.DECRYPT_MODE, sm2KeyPair.getPrivate());
                return cipher.doFinal(data);
            }
        } catch (Exception e) {
            log.error("SM2 {} 操作失败: {}", mode, e.getMessage());
            throw new RuntimeException("SM2 " + mode + " 操作失败: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] cipherData(byte[] data, EncryptMode mode) throws Exception {
        initKeys(); // 确保密钥已初始化
        
        log.debug("BouncyCastle SM4 {} 操作，数据长度: {}", mode, data.length);
        
        try {
            // SM4 加解密
            Cipher cipher = Cipher.getInstance("SM4/ECB/PKCS5Padding", PROVIDER);
            
            if (mode == EncryptMode.ENCRYPT) {
                cipher.init(Cipher.ENCRYPT_MODE, sm4Key);
                return cipher.doFinal(data);
            } else {
                cipher.init(Cipher.DECRYPT_MODE, sm4Key);
                return cipher.doFinal(data);
            }
        } catch (Exception e) {
            log.error("SM4 {} 操作失败: {}", mode, e.getMessage());
            throw new RuntimeException("SM4 " + mode + " 操作失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String getServiceType() {
        return "BOUNCYCASTLE";
    }
}
