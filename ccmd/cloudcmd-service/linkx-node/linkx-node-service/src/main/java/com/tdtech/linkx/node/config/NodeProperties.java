package com.tdtech.linkx.node.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * P2P节点配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "p2p")
public class NodeProperties {

    // 本节点信息
    private LocalNode local = new LocalNode();

    // ws服务器配置
    private ServerConfig server = new ServerConfig();

    // ws客户端配置
    private ClientConfig client = new ClientConfig();

    // JWT配置
    private JwtConfig jwt = new JwtConfig();

    // 挑战码配置
    private ChallengeConfig challenge = new ChallengeConfig();

    // proxy 路由配置（originURI 前缀 → 微服务名）
    private ProxyConfig proxy = new ProxyConfig();

    @Data
    public static class LocalNode {
        private String ip;

        private String vip;

        private int port = 30016;

        private String name;

        private String tag;

        private String callbackUrlTemplate = "https://%s:30844/linkx/admin/node/v1/p2p/%s/authorized/callback";

        private String registerUrlTemplate = "https://%s:30844/linkx/admin/node/v1/p2p/register";

        private String deleteClientUrlTemplate = "https://%s:30844/linkx/admin/node/v1/p2p/clients/delete/%s";

        private String deleteServerUrlTemplate = "https://%s:30844/linkx/admin/node/v1/p2p/servers/delete/%s";

        private String proxyUrlTemplate = "http://%s:%d/node/v1/p2p/%s/proxy/%s";

        private String wsUrlTemplate = "ws://%s:30017/ws?peerId=%s";

        public String getEffectiveIp() {
            return (vip != null && !vip.isEmpty()) ? vip : ip;
        }

        public String buildCallbackUrl(String peerId) {
            return String.format(callbackUrlTemplate, getEffectiveIp(), peerId);
        }

        public String buildRegisterUrl(String targetIp) {
            return String.format(registerUrlTemplate, targetIp);
        }

        public String buildDeleteClientUrl(String targetIp, String peerId) {
            return String.format(deleteClientUrlTemplate, targetIp, peerId);
        }

        public String buildDeleteServerUrl(String targetIp, String peerId) {
            return String.format(deleteServerUrlTemplate, targetIp, peerId);
        }

        /**
         * 构建 dispatch 调对端 proxy 的 URL。
         *
         * @param targetIp  对端 IP
         * @param peerId    本端 peerId（path 中携带，对端 proxy 用来校验调用方身份）
         * @param originUri 原始业务 URI（如 /collaboration/v1/post/queryDepartment）
         */
        public String buildProxyUrl(String targetIp, String peerId, String originUri) {
            // 去掉前导 /，避免与模板末尾的 / 拼接成 //（模板为 .../proxy/%s）
            String normalizedOriginUri = originUri;
            while (normalizedOriginUri.startsWith("/")) {
                normalizedOriginUri = normalizedOriginUri.substring(1);
            }
            return String.format(proxyUrlTemplate, targetIp, port, peerId, normalizedOriginUri);
        }

        /**
         * 构建到对端的 WS 连接 URL。
         *
         * @param targetIp 对端 IP（PeerNodeServer.ip，应为对端可访问的 APISIX 地址）
         * @param peerId   本端 peerId
         */
        public String buildWsUrl(String targetIp, String peerId) {
            return String.format(wsUrlTemplate, targetIp, peerId);
        }
    }

    /**
     * proxy 路由配置：originURI 前缀 → 微服务 K8s service name。
     * proxy 端收到 originURI 后，按前缀匹配，通过 service name 直连微服务（绕过 APISIX 与网关）。
     */
    @Data
    public static class ProxyConfig {
        private List<Route> routes;

        @Data
        public static class Route {
            /**
             * originURI 前缀，如 /collaboration、/dashboard
             */
            private String prefix;

            /**
             * 目标微服务 K8s service name，如 cloudcmd-im-jingxin
             */
            private String service;
        }
    }

    @Data
    public static class ServerConfig {
        // 是否启用
        private boolean enabled = true;

        // ws端口
        private int port = 30017;
    }

    @Data
    public static class ClientConfig {
        // 心跳间隔（毫秒）
        private long heartbeatInterval = 10000;

        // 重连间隔（毫秒）
        private long reconnectInterval = 60000;
    }

    @Data
    public static class JwtConfig {
        // JWT密钥
        private String secret = "6XhiLJLUhXZo7YcZ9gSX9AOV13T+UP0SAEDIYFNo8jg=";

        // JWT过期时间（秒）
        private long expireSeconds = 3600;
    }

    @Data
    public static class ChallengeConfig {
        // 挑战码过期时间（秒）
        private long expireSeconds = 30;
    }
}
