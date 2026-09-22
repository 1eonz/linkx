package com.tdtech.linkx.encryptor.service.impl;

import com.tdtech.linkx.encryptor.service.EncryptMode;
import com.tdtech.linkx.encryptor.service.IEncryptService;
import lombok.extern.slf4j.Slf4j;
import org.sonicom.scm.api.ISecService;
import org.sonicom.scm.enums.Mode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * SDK 加密服务实现
 * 
 * 使用 SdkSM 的 ISecService 实现国密算法加解密
 */
@Slf4j
@Service("sdkEncryptService")
public class SdkEncryptServiceImpl implements IEncryptService {

    @Autowired
    private ISecService secService;

    @Override
    public byte[] cipherKey(byte[] data, EncryptMode mode) throws Exception {
        log.debug("SDK SM2 {} 操作，数据长度: {}", mode, data.length);
        
        // 转换模式
        Mode sdkMode = (mode == EncryptMode.ENCRYPT) ? Mode.ENCRYPT : Mode.DECRYPT;
        
        // 调用 SDK 的 SM2 加解密
        return secService.cipherKey(data, sdkMode);
    }

    @Override
    public byte[] cipherData(byte[] data, EncryptMode mode) throws Exception {
        log.debug("SDK SM4 {} 操作，数据长度: {}", mode, data.length);
        
        // 转换模式
        Mode sdkMode = (mode == EncryptMode.ENCRYPT) ? Mode.ENCRYPT : Mode.DECRYPT;
        
        // 调用 SDK 的 SM4 加解密
        log.info("使用sdk加解密开始");
        byte[] bytes = secService.cipherData(data, sdkMode);
        log.info("使用sdk加解密结束");
        return bytes;
    }

    @Override
    public String getServiceType() {
        return "SDK";
    }
}
