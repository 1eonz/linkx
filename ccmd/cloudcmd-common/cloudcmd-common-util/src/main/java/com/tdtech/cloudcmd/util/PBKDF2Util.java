package com.tdtech.cloudcmd.util;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.apache.commons.codec.binary.Base64;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * @author zhuangzl
 * @date 2020-06-29 11:08
 */
@Slf4j
public class PBKDF2Util {

    public static final String DEFAULT_CIPHER_AND_PADDING = "PBKDF2WithHmacSHA256";

    public static final String DEFAULT_SALT_ALGORITHM = "SHA1PRNG";

    public static final int DEFAULT_SALT_LENGTH = 16;

    public static final int iterations = 10000;

    public static final String SEPARATOR = "_";

    /**
     * 生成盐值
     * 
     * @return
     * @throws NoSuchAlgorithmException
     */
    public static String generateSalt() throws NoSuchAlgorithmException {
        return generateSalt(DEFAULT_SALT_LENGTH, DEFAULT_SALT_ALGORITHM);
    }

    /**
     * 生成盐值
     * 
     * @param length
     * @return
     * @throws NoSuchAlgorithmException
     */
    public static String generateSalt(int length) throws NoSuchAlgorithmException {
        return generateSalt(length, DEFAULT_SALT_ALGORITHM);
    }

    /**
     * 生成盐值
     * 
     * @param length
     * @param algorithm
     * @return
     * @throws NoSuchAlgorithmException
     */
    public static String generateSalt(int length, String algorithm) throws NoSuchAlgorithmException {
        SecureRandom random = SecureRandom.getInstance(algorithm);
        byte[] salt = new byte[length];
        random.nextBytes(salt);
        return Base64.encodeBase64String(salt);
    }

    /**
     *
     * digest
     *
     * @param hashAlgorithm 哈希算法
     * @param password 口令
     * @param salt 盐值
     * @param iterations 迭代次数，
     * @param keyLength 生成密文长度
     * @return
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    public static byte[] digest(String hashAlgorithm, char[] password, String salt, int iterations, int keyLength)
        throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory kFactory = SecretKeyFactory.getInstance(hashAlgorithm);
        PBEKeySpec pbeKeySpec = new PBEKeySpec(password, salt.getBytes(), iterations, keyLength);
        SecretKey secretKey = kFactory.generateSecret(pbeKeySpec);
        return secretKey.getEncoded();
    }

    /**
     *
     * 要求迭代次数至少10000次，有性能约束的产品最少1000次，输出秘钥的长度最少256比特(32字节)，其中盐值和迭代次数不需要加密保存
     *
     * @param password 用户输入口令
     * @param salt 盐值，要求最少8字节，推荐16字节及以上，安全随机数
     * @return
     * @throws InvalidKeySpecException
     * @throws NoSuchAlgorithmException
     */
    @SneakyThrows
    public static byte[] PBKDF2ForPass(String password, String salt) {

        // 默认采用PBKDF2WithHmacSHA256算法，迭代次数10000次，输出密文的字节长度256。
        return PBKDF2Util.digest(DEFAULT_CIPHER_AND_PADDING, password.toCharArray(), salt, iterations, 256);

    }

    /**
     * 华为公司建议秘钥生成算法
     *
     * @param text 用户输入或读取的一串字符串
     * @param salt 盐值，要求最少8字节，推荐16字节及以上，安全随机数
     * @param keyLength 生成的秘钥长度
     * @return
     */
    @SneakyThrows
    public static byte[] PBKDF2ForPrivateKey(String text, String salt, int keyLength) {

        // 默认采用PBKDF2WithHmacSHA256算法，迭代次数10000次
        return PBKDF2Util.digest(DEFAULT_CIPHER_AND_PADDING, text.toCharArray(), salt, 10000, keyLength);
    }

    /**
     * 华为公司建议PBKDF2算法字符串密文保存格式: AlgID_Salt_IterCount_CipherData
     *
     * @param password 用户输入口令
     * @param salt 盐值，要求最少8字节，推荐16字节及以上，安全随机数
     * @return
     */
    public static String PBKDF2ForPassStandard(String password, String salt) {
        byte[] digest = PBKDF2ForPass(password, salt);
        // Base64编码字符串形式
        String base64Digest = java.util.Base64.getEncoder().encodeToString(digest);
        return DEFAULT_CIPHER_AND_PADDING + SEPARATOR + salt + SEPARATOR + iterations + SEPARATOR + base64Digest;
    }

    /**
     * 从加密后的密码中获取salt
     * 
     * @param standardPass
     * @return
     */
    public static String getSaltFromStandardPass(String standardPass) {
        if (standardPass == null || standardPass.length() <= 0) {
            return null;
        }
        return standardPass.split(SEPARATOR)[1];
    }
}
