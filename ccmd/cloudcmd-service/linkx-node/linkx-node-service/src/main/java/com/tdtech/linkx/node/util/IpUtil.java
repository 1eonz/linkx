package com.tdtech.linkx.node.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class IpUtil {
    public static boolean isValidIpv4(String ip) {
        try {
            InetAddress inetAddress = InetAddress.getByName(ip);
            return inetAddress.getHostAddress().equals(ip);
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
