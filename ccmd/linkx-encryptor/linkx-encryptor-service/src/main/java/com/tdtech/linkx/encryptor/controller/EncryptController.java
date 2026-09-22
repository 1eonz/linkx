package com.tdtech.linkx.encryptor.controller;

import com.tdtech.linkx.encryptor.request.TextEncryptRequest;
import com.tdtech.linkx.encryptor.resp.R;
import com.tdtech.linkx.encryptor.service.EncryptService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 加密控制器
 * 提供 SM2/SM4 加解密功能
 */
@Slf4j
@RestController
@RequestMapping("/linkx/v1/encryptor")
@RequiredArgsConstructor
public class EncryptController {

    private final EncryptService encryptService;

    /**
     * SM2 文本加密
     */
    @PostMapping("/sm2/encrypt")
    public R<String> sm2Encrypt(@Valid @RequestBody TextEncryptRequest request) {
        log.info("SM2加密请求");
        String result = encryptService.sm2Encrypt(request);
        log.info("SM2加密成功");
        return R.success(result);
    }

    /**
     * SM2 文本解密
     */
    @PostMapping("/sm2/decrypt")
    public R<String> sm2Decrypt(@Valid @RequestBody TextEncryptRequest request) {
        log.info("SM2解密请求");
        String result = encryptService.sm2Decrypt(request);
        log.info("SM2解密成功");
        return R.success(result);
    }

    /**
     * SM4 文本加密
     */
    @PostMapping("/sm4/encrypt")
    public R<String> sm4Encrypt(@Valid @RequestBody TextEncryptRequest request) {
        log.info("SM4加密请求");
        String result = encryptService.sm4Encrypt(request);
        log.info("SM4加密成功");
        return R.success(result);
    }

    /**
     * SM4 文本解密
     */
    @PostMapping("/sm4/decrypt")
    public R<String> sm4Decrypt(@Valid @RequestBody TextEncryptRequest request) {
        log.info("SM4解密请求");
        String result = encryptService.sm4Decrypt(request);
        log.info("SM4解密成功");
        return R.success(result);
    }

    /**
     * SM4 文件流式加密 - 接收文件上传，流式返回加密文件
     * 
     * 适用场景：跨机房调用，无需共享文件系统
     * 
     * @param file 上传的文件
     * @param bufferSize 缓冲区大小（可选，默认8KB）
     * @param response HTTP响应对象，直接写入加密后的文件流
     */
    @PostMapping("/sm4/stream/encrypt")
    public void sm4StreamEncrypt(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "bufferSize", required = false) Integer bufferSize,
            HttpServletResponse response) throws IOException {
        
        log.info("SM4流式文件加密请求，文件名: {}, 大小: {} bytes", 
                 file.getOriginalFilename(), file.getSize());
        
        try {
            // 设置响应头：原文件名 + .enc
            String originalFilename = file.getOriginalFilename();
            String encryptedFilename = originalFilename + ".enc";
            
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", 
                "attachment; filename=" + encryptedFilename);
            
            // 流式加密：从输入流读取 -> 加密 -> 写入输出流
            encryptService.sm4StreamEncrypt(
                file.getInputStream(), 
                response.getOutputStream(), 
                bufferSize
            );
            
            log.info("SM4流式文件加密成功");
        } catch (Exception e) {
            log.error("SM4流式文件加密失败", e);
            // 流式接口异常：重置响应并返回错误信息
            response.reset();
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            String errorMsg = String.format("{\"code\":500,\"message\":\"SM4流式文件加密失败: %s\",\"data\":null}", 
                                           e.getMessage());
            response.getWriter().write(errorMsg);
        }
    }

    /**
     * SM4 文件流式解密 - 接收文件上传，流式返回解密文件
     * 
     * 适用场景：跨机房调用，无需共享文件系统
     * 
     * @param file 上传的文件
     * @param bufferSize 缓冲区大小（可选，默认8KB）
     * @param response HTTP响应对象，直接写入解密后的文件流
     */
    @PostMapping("/sm4/stream/decrypt")
    public void sm4StreamDecrypt(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "bufferSize", required = false) Integer bufferSize,
            HttpServletResponse response) throws IOException {
        
        log.info("SM4流式文件解密请求，文件名: {}, 大小: {} bytes", 
                 file.getOriginalFilename(), file.getSize());
        
        try {
            // 设置响应头：去掉 .enc 扩展名
            String originalFilename = file.getOriginalFilename();
            String decryptedFilename = originalFilename;
            
            if (originalFilename.endsWith(".enc")) {
                decryptedFilename = originalFilename.substring(0, originalFilename.length() - 4);
            }
            
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", 
                "attachment; filename=" + decryptedFilename);
            
            // 流式解密：从输入流读取 -> 解密 -> 写入输出流
            encryptService.sm4StreamDecrypt(
                file.getInputStream(), 
                response.getOutputStream(), 
                bufferSize
            );
            
            log.info("SM4流式文件解密成功");
        } catch (Exception e) {
            log.error("SM4流式文件解密失败", e);
            // 流式接口异常：重置响应并返回错误信息
            response.reset();
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            String errorMsg = String.format("{\"code\":500,\"message\":\"SM4流式文件解密失败: %s\",\"data\":null}", 
                                           e.getMessage());
            response.getWriter().write(errorMsg);
        }
    }
}
