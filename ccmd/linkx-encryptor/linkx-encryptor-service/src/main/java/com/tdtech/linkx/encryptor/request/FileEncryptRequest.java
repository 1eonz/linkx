package com.tdtech.linkx.encryptor.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * 文件加解密请求
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileEncryptRequest {
    /**
     * 加密算法：SM4
     */
    @NotBlank(message = "算法不能为空")
    private String algorithm;

    /**
     * 加密模式：encrypt-加密，decrypt-解密
     */
    @NotBlank(message = "模式不能为空")
    private String mode;

    /**
     * 流式模式：stream-流式加密，non-stream-非流式加密
     */
    @NotBlank(message = "流式模式不能为空")
    private String streamMode;

    /**
     * 源文件路径
     */
    @NotBlank(message = "源文件路径不能为空")
    private String sourcePath;

    /**
     * 目标文件路径
     */
    @NotBlank(message = "目标文件路径不能为空")
    private String targetPath;

    /**
     * 缓冲区大小（字节），默认8192
     */
    private Integer bufferSize = 8192;
}
