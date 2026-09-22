package com.tdtech.cloudcmd.encryptor.constants;


/**
 * 常量类
 */
public class Constants {
    /**
     * 开启加密的配置的key
     */
    public static final String ENABLE_ENCRYPT = "ENABLE_ENCRYPT";
    
    /**
     * linkx-encryptor 服务地址的key
     */
    public static final String LINKX_ENCRYPTOR_URL = "LINKX_ENCRYPTOR_URL";
    
    /**
     * SM2 加密接口 URI
     */
    public static final String SM2_ENCRYPT_URI = "/linkx/v1/encryptor/sm2/encrypt";
    
    /**
     * SM2 解密接口 URI
     */
    public static final String SM2_DECRYPT_URI = "/linkx/v1/encryptor/sm2/decrypt";
    
    /**
     * SM4 加密接口 URI
     */
    public static final String SM4_ENCRYPT_URI = "/linkx/v1/encryptor/sm4/encrypt";
    
    /**
     * SM4 解密接口 URI
     */
    public static final String SM4_DECRYPT_URI = "/linkx/v1/encryptor/sm4/decrypt";
    
    /**
     * SM4 流式加密接口 URI
     */
    public static final String SM4_STREAM_ENCRYPT_URI = "/linkx/v1/encryptor/sm4/stream/encrypt";
    
    /**
     * SM4 流式解密接口 URI
     */
    public static final String SM4_STREAM_DECRYPT_URI = "/linkx/v1/encryptor/sm4/stream/decrypt";
    
    /**
     * 文件分块大小阈值：50MB
     * 超过此大小的文件将采用分块加密
     */
    public static final long FILE_SIZE_THRESHOLD = 50 * 1024 * 1024L;
    
    /**
     * 每个分块的大小：10MB
     */
    public static final int CHUNK_SIZE = 50 * 1024 * 1024;
}
