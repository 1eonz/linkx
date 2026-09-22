package com.tdtech.cloudcmd.admin.util.sha256;

/**
 * @author wWX623688
 */
public class EncryptionUtil {

    public static String sha256Encryption(String ciphertext) {
        return SHA256Encryption.encrypt(ciphertext);
    }

}
