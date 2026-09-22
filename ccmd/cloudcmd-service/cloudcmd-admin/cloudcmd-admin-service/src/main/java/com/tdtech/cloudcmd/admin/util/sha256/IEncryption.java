package com.tdtech.cloudcmd.admin.util.sha256;

/**
 * @author wWX623688
 */
public interface IEncryption {
    /**
     * 认证加密
     * 
     * @param plainCode
     * @param encryptedCode
     * @param salt
     * @return
     */
    boolean authenticateCode(String plainCode, String encryptedCode, String salt);

    /**
     * 加密
     * 
     * @param password
     * @param salt
     * @return
     */
    String generateEncryptedCode(String password, String salt);

    /**
     * 盐值
     * 
     * @return
     */
    String generateSalt();

}
