package com.tdtech.cloudcmd.util;

public class HexUtil {

    private static final char[] HEX_ARR = "0123456789ABCDEF".toCharArray();

    public static String bytes2Hex(byte[] byteArray) {
        if (byteArray == null) {
            return null;
        }
        char[] hexChars = new char[byteArray.length * 2];
        for (int j = 0; j < byteArray.length; j++) {
            int v = byteArray[j] & 0xFF;
            hexChars[j * 2] = HEX_ARR[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARR[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] hexToBytes(String str) {
        if (str == null) {
            return null;
        }
        if (str.length() == 0) {
            return new byte[0];
        }
        if (str.length() % 2 == 1) {
            throw new RuntimeException("not a valid hex str");
        }
        byte[] byteArray = new byte[str.length() / 2];
        for (int i = 0; i < byteArray.length; i++) {
            String subStr = str.substring(2 * i, 2 * i + 2);
            byteArray[i] = ((byte)Integer.parseInt(subStr, 16));
        }
        return byteArray;
    }

}
