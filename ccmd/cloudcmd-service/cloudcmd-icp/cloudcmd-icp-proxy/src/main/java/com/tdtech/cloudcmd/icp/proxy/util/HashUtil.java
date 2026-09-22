package com.tdtech.cloudcmd.icp.proxy.util;

import java.nio.charset.StandardCharsets;

public class HashUtil {
    private HashUtil(){}
    /**
     * 使用MurmurHash3算法计算字符串哈希值
     */
    public static long hash(String key) {
        return hash(key.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * MurmurHash3算法实现
     */
    public static long hash(byte[] data) {
        int len = data.length;
        int seed = 0x12345678;
        int c1 = 0xcc9e2d51;
        int c2 = 0x1b873593;
        int r1 = 15;
        int r2 = 13;
        int m = 5;
        int n = 0xe6546b64;

        int hash = seed;
        int k1;
        int off = 0;

        while (len >= 4) {
            k1 = (data[off++] & 0xff) |
                ((data[off++] & 0xff) << 8) |
                ((data[off++] & 0xff) << 16) |
                ((data[off++] & 0xff) << 24);

            k1 *= c1;
            k1 = (k1 << r1) | (k1 >>> (32 - r1));
            k1 *= c2;

            hash ^= k1;
            hash = (hash << r2) | (hash >>> (32 - r2));
            hash = hash * m + n;

            len -= 4;
        }

        switch (len) {
            case 3:
                hash ^= (data[off + 2] & 0xff) << 16;
            case 2:
                hash ^= (data[off + 1] & 0xff) << 8;
            case 1:
                hash ^= (data[off] & 0xff);
                hash *= c1;
                hash = (hash << r1) | (hash >>> (32 - r1));
                hash *= c2;
                break;
        }

        hash ^= data.length;
        hash ^= (hash >>> 16);
        hash *= 0x85ebca6b;
        hash ^= (hash >>> 13);
        hash *= 0xc2b2ae35;
        hash ^= (hash >>> 16);

        return Integer.toUnsignedLong(hash);
    }

//    public static void main(String[] args) {
//        System.out.println(hash("10001"));
//    }

}
