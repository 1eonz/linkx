package com.tdtech.linkx.encryptor.service;

import com.tdtech.linkx.encryptor.request.TextEncryptRequest;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * 加密服务接口
 */
public interface EncryptService {

    /**
     * SM2 文本加密
     *
     * @param request 加密请求
     * @return 加密结果
     */
    String sm2Encrypt(TextEncryptRequest request);

    /**
     * SM2 文本解密
     *
     * @param request 解密请求
     * @return 解密结果
     */
    String sm2Decrypt(TextEncryptRequest request);

    /**
     * SM4 文本加密
     *
     * @param request 加密请求
     * @return 加密结果
     */
    String sm4Encrypt(TextEncryptRequest request);

    /**
     * SM4 文本解密
     *
     * @param request 解密请求
     * @return 解密结果
     */
    String sm4Decrypt(TextEncryptRequest request);

    /**
     * SM4 文件流式加密 - 接收输入流，输出到输出流
     * 
     * 适用场景：跨机房调用，无需共享文件系统
     * 
     * @param inputStream 输入流（待加密的文件）
     * @param outputStream 输出流（加密后的文件）
     * @param bufferSize 缓冲区大小（可选，默认8KB）
     */
    void sm4StreamEncrypt(InputStream inputStream, OutputStream outputStream, Integer bufferSize);

    /**
     * SM4 文件流式解密 - 接收输入流，输出到输出流
     * 
     * 适用场景：跨机房调用，无需共享文件系统
     * 
     * @param inputStream 输入流（待解密的文件）
     * @param outputStream 输出流（解密后的文件）
     * @param bufferSize 缓冲区大小（可选，默认8KB）
     */
    void sm4StreamDecrypt(InputStream inputStream, OutputStream outputStream, Integer bufferSize);
}
