package com.tdtech.cloudcmd.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author swx181319
 */
public class SHA256Utils {
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
}
