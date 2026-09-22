package com.tdtech.cloudcmd.encryptor.service;

/**
 * 加密服务接口
 * 提供字符串加解密接口
 */
public interface EncryptionService {

    /**
     * 判断是否开启了加密
     *
     * @return 是否开启加密
     */
    boolean encryptEnabled();


    /**
     * 加密字符串
     * 
     * @param plaintext 明文
     * @return 密文
     */
    String encrypt(String plaintext);

    /**
     * 解密字符串
     * 
     * @param ciphertext 密文
     * @return 明文
     */
    String decrypt(String ciphertext);

    /**
     * 开启加密
     */
    void enable();

    /**
     * 关闭加密
     */
    void disable();

    /**
     * 通过流的方式加密文件
     *
     * @param filePath 文件路径
     * @param encFileDir 加密后的文件存放目录
     * @return 加密后的文件路径
     */
    String encryptFile(String filePath, String encFileDir);

    /**
     * 通过流的方式解密文件
     *
     * @param filePath 文件路径
     * @param decFileDir 解密后的文件存放目录
     * @return 解密后的文件路径
     */
    String decryptFile(String filePath, String decFileDir);

    /**
     * 获取加密后的文件路径-若加密文件不存在，则返回原路径
     *
     * @param fileName   文件名称
     * @param encFileDir 加密后的文件存放目录
     * @return 加密后的文件路径
     */
    String getEncryptFilePath(String fileName, String encFileDir);
}
