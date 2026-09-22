package com.tdtech.cloudcmd.im.jingxin.client.utils;

import java.security.SecureRandom;
import java.util.Base64;

public final class RandomUtil {

    private static final SecureRandom random = new SecureRandom();

    private RandomUtil() {
        throw new UnsupportedOperationException();
    }

    public static String randomString() {
        var randB = new byte[32];
        random.nextBytes(randB);
        return Base64.getEncoder().encodeToString(randB);
    }
}
