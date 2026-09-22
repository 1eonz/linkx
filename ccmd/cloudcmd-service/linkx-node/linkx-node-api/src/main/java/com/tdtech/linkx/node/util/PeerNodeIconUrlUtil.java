package com.tdtech.linkx.node.util;

import org.apache.commons.lang3.StringUtils;

/**
 * 跨节点协同岗图标 URL 处理工具。
 * <p>
 * 对端节点返回的协同岗 iconUrl 是相对路径（如 /collaboration/static/xxx.jpg），
 * 本端前端无法直接访问，需拼上对端节点完整 URL 前缀。
 */
public final class PeerNodeIconUrlUtil {

    /**
     * 对端节点图片访问端口（K8s NodePort 统一入口）
     */
    private static final String PEER_NODE_PORT = "30844";

    /**
     * 对端节点图片访问路径前缀
     */
    private static final String PEER_NODE_PATH_PREFIX = "/linkx/admin";

    private PeerNodeIconUrlUtil() {
    }

    /**
     * 根据对端节点 IP 构造图片访问 baseUrl。
     *
     * @param peerIp 对端节点 IP
     * @return 形如 https://10.28.70.22:30844/linkx/admin；IP 为空时返回 null
     */
    public static String buildBaseUrl(String peerIp) {
        if (StringUtils.isBlank(peerIp)) {
            return null;
        }
        return "https://" + peerIp + ":" + PEER_NODE_PORT + PEER_NODE_PATH_PREFIX;
    }

    /**
     * 将相对路径的 iconUrl 拼成对端完整 URL。
     * <p>
     * - baseUrl 为空：返回 null（无法访问对端，不返回无效相对路径）
     * - iconUrl 为空：返回 null
     * - iconUrl 已是 http/https 开头的完整URL：原样返回
     * - 其他：返回 baseUrl + iconUrl
     *
     * @param baseUrl  对端节点 baseUrl（由 {@link #buildBaseUrl} 生成）
     * @param iconUrl  协同岗图标相对路径
     * @return 完整图标URL或 null
     */
    public static String prepend(String baseUrl, String iconUrl) {
        if (StringUtils.isBlank(baseUrl) || StringUtils.isBlank(iconUrl)) {
            return null;
        }
        if (iconUrl.startsWith("http")) {
            return iconUrl;
        }
        return baseUrl + iconUrl;
    }
}
