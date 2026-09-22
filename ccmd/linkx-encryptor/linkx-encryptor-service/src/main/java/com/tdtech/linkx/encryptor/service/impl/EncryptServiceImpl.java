package com.tdtech.linkx.encryptor.service.impl;

import com.tdtech.linkx.encryptor.config.SdkProperties;
import com.tdtech.linkx.encryptor.request.TextEncryptRequest;
import com.tdtech.linkx.encryptor.service.EncryptMode;
import com.tdtech.linkx.encryptor.service.IEncryptService;
import com.tdtech.linkx.encryptor.service.EncryptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 加密服务实现类
 * 
 * 支持多种加密实现：
 * - SDK 实现：使用 SdkSM 的国密算法
 * - BouncyCastle 实现：使用 BouncyCastle 的国密算法
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EncryptServiceImpl implements EncryptService {

    private final IEncryptService encryptService;
    private final SdkProperties sdkProperties;
    
    private static final String SM4_ALGORITHM = "SM4";
    private static final String PROVIDER = "BC";

    @Override
    public String sm2Encrypt(TextEncryptRequest request) {
        try {
            log.info("SM2加密开始，数据长度: {}, 加密服务类型: {}", 
                     request.getData().length(), encryptService.getServiceType());
            
            // 将文本转换为字节数组
            byte[] plainData = request.getData().getBytes(StandardCharsets.UTF_8);
            
            // 调用加密服务
            byte[] encryptedData = encryptService.cipherKey(plainData, EncryptMode.ENCRYPT);
            
            // 将加密结果转换为 Base64 字符串
            String result = Base64.getEncoder().encodeToString(encryptedData);
            
            log.info("SM2加密完成，结果长度: {}", result.length());
            return result;
        } catch (Exception e) {
            log.error("SM2加密失败", e);
            throw new RuntimeException("SM2加密失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String sm2Decrypt(TextEncryptRequest request) {
        try {
            log.info("SM2解密开始，数据长度: {}, 加密服务类型: {}", 
                     request.getData().length(), encryptService.getServiceType());
            
            // 将 Base64 字符串转换为字节数组
            byte[] encryptedData = Base64.getDecoder().decode(request.getData());
            
            // 调用加密服务
            byte[] plainData = encryptService.cipherKey(encryptedData, EncryptMode.DECRYPT);
            
            // 将解密结果转换为字符串
            String result = new String(plainData, StandardCharsets.UTF_8);
            
            log.info("SM2解密完成，结果长度: {}", result.length());
            return result;
        } catch (Exception e) {
            log.error("SM2解密失败", e);
            throw new RuntimeException("SM2解密失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String sm4Encrypt(TextEncryptRequest request) {
        try {
            log.info("SM4加密开始，数据长度: {}, 加密服务类型: {}", 
                     request.getData().length(), encryptService.getServiceType());
            
            // 将文本转换为字节数组
            byte[] plainData = request.getData().getBytes(StandardCharsets.UTF_8);
            
            // 调用加密服务
            byte[] encryptedData = encryptService.cipherData(plainData, EncryptMode.ENCRYPT);
            
            // 将加密结果转换为 Base64 字符串
            String result = Base64.getEncoder().encodeToString(encryptedData);
            
            log.info("SM4加密完成，结果长度: {}", result.length());
            return result;
        } catch (Exception e) {
            log.error("SM4加密失败", e);
            throw new RuntimeException("SM4加密失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String sm4Decrypt(TextEncryptRequest request) {
        try {
            log.info("SM4解密开始，数据长度: {}, 加密服务类型: {}", 
                     request.getData().length(), encryptService.getServiceType());
            
            // 将 Base64 字符串转换为字节数组
            byte[] encryptedData = Base64.getDecoder().decode(request.getData());
            
            // 调用加密服务
            byte[] plainData = encryptService.cipherData(encryptedData, EncryptMode.DECRYPT);
            
            // 将解密结果转换为字符串
            String result = new String(plainData, StandardCharsets.UTF_8);
            
            log.info("SM4解密完成，结果长度: {}", result.length());
            return result;
        } catch (Exception e) {
            log.error("SM4解密失败", e);
            throw new RuntimeException("SM4解密失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void sm4StreamEncrypt(InputStream inputStream, OutputStream outputStream, Integer bufferSize) {
        try {
            log.info("SM4流式加密开始，加密服务类型: {}", encryptService.getServiceType());
            
            // 获取 SM4 密钥
            String sm4KeyStr = sdkProperties.getSm4Key();
            if (sm4KeyStr == null || sm4KeyStr.length() != 16) {
                log.warn("SM4 密钥未配置或长度不正确，使用默认密钥");
                sm4KeyStr = "1234567890123456";
            }
            
            // 使用 CipherOutputStream 进行流式加密（自动处理填充）
            SecretKey key = new SecretKeySpec(sm4KeyStr.getBytes(), SM4_ALGORITHM);
            Cipher cipher = Cipher.getInstance(SM4_ALGORITHM + "/ECB/PKCS5Padding", PROVIDER);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            
            try (CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, cipher)) {
                byte[] buffer = new byte[bufferSize != null ? bufferSize : 8192];
                int bytesRead;
                
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    cipherOutputStream.write(buffer, 0, bytesRead);
                }
                
                cipherOutputStream.flush();
            }
            
            log.info("SM4流式加密完成");
        } catch (Exception e) {
            log.error("SM4流式加密失败", e);
            throw new RuntimeException("SM4流式加密失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void sm4StreamDecrypt(InputStream inputStream, OutputStream outputStream, Integer bufferSize) {
        try {
            log.info("SM4流式解密开始，加密服务类型: {}", encryptService.getServiceType());
            
            // 获取 SM4 密钥
            String sm4KeyStr = sdkProperties.getSm4Key();
            if (sm4KeyStr == null || sm4KeyStr.length() != 16) {
                log.warn("SM4 密钥未配置或长度不正确，使用默认密钥");
                sm4KeyStr = "1234567890123456";
            }
            
            // 使用 CipherInputStream 进行流式解密（自动处理填充）
            SecretKey key = new SecretKeySpec(sm4KeyStr.getBytes(), SM4_ALGORITHM);
            Cipher cipher = Cipher.getInstance(SM4_ALGORITHM + "/ECB/PKCS5Padding", PROVIDER);
            cipher.init(Cipher.DECRYPT_MODE, key);
            
            try (CipherInputStream cipherInputStream = new CipherInputStream(inputStream, cipher)) {
                byte[] buffer = new byte[bufferSize != null ? bufferSize : 8192];
                int bytesRead;
                
                while ((bytesRead = cipherInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                
                outputStream.flush();
            }
            
            log.info("SM4流式解密完成");
        } catch (Exception e) {
            log.error("SM4流式解密失败", e);
            throw new RuntimeException("SM4流式解密失败: " + e.getMessage(), e);
        }
    }
}
