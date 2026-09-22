package com.tdtech.linkx.node.util;

import java.nio.charset.StandardCharsets;

/**
 * 节点标识工具类
 * peer_id = HexEncode(UTF8(IP:Port))
 */
public class PeerIdUtil {

    private PeerIdUtil() {
    }

    /**
     * 生成节点标识
     *
     * @param ip   IP地址
     * @param port 端口
     * @return 节点标识
     */
    public static String generatePeerId(String ip, int port) {
        String address = ip + ":" + port;
        byte[] utf8Bytes = address.getBytes(StandardCharsets.UTF_8);
        return bytesToHex(utf8Bytes);
    }

    /**
     * 解析节点标识
     *
     * @param peerId 节点标识
     * @return [ip, port]
     */
    public static String[] parsePeerId(String peerId) {
        try {
            byte[] bytes = hexToBytes(peerId);
            String address = new String(bytes, StandardCharsets.UTF_8);
            int colonIndex = address.lastIndexOf(':');
            if (colonIndex > 0) {
                String ip = address.substring(0, colonIndex);
                String port = address.substring(colonIndex + 1);
                return new String[]{ip, port};
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid peerId: " + peerId, e);
        }
        throw new IllegalArgumentException("Invalid peerId format: " + peerId);
    }

    public static String getIp(String peerId) {
        return parsePeerId(peerId)[0];
    }

    public static int getPort(String peerId) {
        return Integer.parseInt(parsePeerId(peerId)[1]);
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
