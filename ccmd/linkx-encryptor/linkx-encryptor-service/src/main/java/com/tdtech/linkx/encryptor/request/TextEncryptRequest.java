package com.tdtech.linkx.encryptor.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * 文本加解密请求
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TextEncryptRequest {
    /**
     * 待加解密的数据
     */
    @NotBlank(message = "数据不能为空")
    private String data;
}
