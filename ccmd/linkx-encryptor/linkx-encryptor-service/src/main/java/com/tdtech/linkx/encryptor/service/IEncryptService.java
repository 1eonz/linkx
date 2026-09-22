package com.tdtech.linkx.encryptor.service;

/**
 * 加密服务接口
 * 
 * 提供统一的加解密接口，支持多种实现方式：
 * - SDK 实现：使用 SdkSM 的 ISecService
 * - Base64 实现：使用 Base64 编码（用于测试环境）
 */
public interface IEncryptService {
    
    /**
     * SM2 加密/解密（非对称加密）
     * 
     * @param data 待处理的数据
     * @param mode 加密模式：ENCRYPT-加密，DECRYPT-解密
     * @return 处理后的数据
     * @throws Exception 加解密异常
     */
    byte[] cipherKey(byte[] data, EncryptMode mode) throws Exception;
    
    /**
     * SM4 加密/解密（对称加密）
     * 
     * @param data 待处理的数据
     * @param mode 加密模式：ENCRYPT-加密，DECRYPT-解密
     * @return 处理后的数据
     * @throws Exception 加解密异常
     */
    byte[] cipherData(byte[] data, EncryptMode mode) throws Exception;
    
    /**
     * 获取加密服务类型
     * 
     * @return 服务类型：SDK 或 BASE64
     */
    String getServiceType();
}
