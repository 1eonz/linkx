package com.tdtech.cloudcmd.admin.util.sha256;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * @author wWX623688
 */
public class SHA256Encryption implements IEncryption {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Integer SALT_BYTE_SIZE = 24;

    /**
     * 对字符串加密,加密算法使用SHA-256
     *
     * @param strSrc 要加密的字符串
     * @return
     */
    public static String encrypt(String strSrc) {
        MessageDigest md = null;
        String strDes = null;

        byte[] bt = strSrc.getBytes();

        try {
            md = MessageDigest.getInstance("SHA-256");
            md.update(bt);
            strDes = bytes2Hex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            return null;
        }

        return strDes;
    }

    public static String bytes2Hex(byte[] bts) {
        String des = "";
        String tmp = null;

        for (int i = 0; i < bts.length; i++) {
            tmp = (Integer.toHexString(bts[i] & 0xFF));

            if (tmp.length() == 1) {
                des += "0";
            }

            des += tmp;
        }

        return des;
    }

    @Override
    public boolean authenticateCode(String plainCode, String encryptedCode, String salt) {
        String encryptedPlainCode = EncryptionUtil.sha256Encryption(plainCode + salt);

        return encryptedPlainCode.equals(encryptedCode);
    }

    @Override
    public String generateEncryptedCode(String password, String salt) {
        // Auto-generated method stub
        return EncryptionUtil.sha256Encryption(password + salt);
    }

    @Override
    public String generateSalt() {
        byte[] salt = new byte[SALT_BYTE_SIZE];
        SECURE_RANDOM.nextBytes(salt);

        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < salt.length; i++) {
            sb.append(salt[i]);
        }

        return sb.toString();
    }
}
