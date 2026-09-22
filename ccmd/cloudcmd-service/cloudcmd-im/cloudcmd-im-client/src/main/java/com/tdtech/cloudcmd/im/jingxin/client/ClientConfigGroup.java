package com.tdtech.cloudcmd.im.jingxin.client;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientConfigGroup {

    private String name;

    private String imAuthRedisLock;
    private String imToken;

    private String httpHostConfKey;
    private String wsHostConfKey;
    private String gatewayPrefixKey;
    private String cliIdConfKey;
    private String cliSecConfKey;

    private String headersUsccKey;
    private String headersAllowSeidKey;
    private String headersAllowSekeyKey;

    public static List<String> coopConfigNames() {
        return List.of("IM_ADDRESS_HTTP", //
            "IM_ADDRESS_WS", //
            "IM_GATEWAY_PREFIX", //
            "IM_CLI_ID", //
            "IM_CLI_SEC", //
            "IM_USCC", //
            "IM_ALLOW_SEID", //
            "IM_ALLOW_SEKEY");
    }

    public static ClientConfigGroup coopConfigGroup() {
        return new ClientConfigGroup("coopImHttpClient", //
            "cloudcmd:im:coop:auth-lock", //
            "cloudcmd:im:coop:token", //
            "IM_ADDRESS_HTTP", //
            "IM_ADDRESS_WS", //
            "IM_GATEWAY_PREFIX", //
            "IM_CLI_ID", //
            "IM_CLI_SEC", //
            "IM_USCC", //
            "IM_ALLOW_SEID", //
            "IM_ALLOW_SEKEY");
    }

    public static List<String> aiConfigNames() {
        return List.of("IM_ADDRESS_HTTP", //
            "IM_ADDRESS_HTTP", //
            "IM_ADDRESS_WS", //
            "IM_GATEWAY_PREFIX", //
            "IM_GROUP_AI_CLI_ID", //
            "IM_GROUP_AI_CLI_SEC", //
            "IM_USCC", //
            "IM_ALLOW_SEID", //
            "IM_ALLOW_SEKEY");
    }

    public static ClientConfigGroup aiConfigGroup() {
        return new ClientConfigGroup("groupAIImHttpClient", //
            "cloudcmd:im:groupai:auth-lock", //
            "cloudcmd:im:groupai:token", //
            "IM_ADDRESS_HTTP", //
            "IM_ADDRESS_WS", //
            "IM_GATEWAY_PREFIX", //
            "IM_GROUP_AI_CLI_ID", //
            "IM_GROUP_AI_CLI_SEC", //
            "IM_USCC", //
            "IM_ALLOW_SEID", //
            "IM_ALLOW_SEKEY");
    }

    public static List<String> oneO1p4BConfigNames() {
        return List.of("IM_ADDRESS_HTTP", //
                "IM_ADDRESS_HTTP", //
                "IM_ADDRESS_WS", //
                "IM_GATEWAY_PREFIX", //
                "IM_1o1p4B_CLI_ID", //
                "IM_1o1p4B_CLI_SEC", //
                "IM_USCC", //
                "IM_ALLOW_SEID", //
                "IM_ALLOW_SEKEY");
    }

    public static List<String> warningConfigNames() {
        return List.of("IM_ADDRESS_HTTP", //
                "IM_ADDRESS_HTTP", //
                "IM_ADDRESS_WS", //
                "IM_GATEWAY_PREFIX", //
                "IM_WARNING_CLI_ID", //
                "IM_WARNING_CLI_SEC", //
                "IM_USCC", //
                "IM_ALLOW_SEID", //
                "IM_ALLOW_SEKEY");
    }

    public static ClientConfigGroup oneO1p4BConfigGroup() {
        return new ClientConfigGroup("oneO1p4BImHttpClient", //
                "cloudcmd:im:1o1p4B:auth-lock", //
                "cloudcmd:im:1o1p4B:token", //
                "IM_ADDRESS_HTTP", //
                "IM_ADDRESS_WS", //
                "IM_GATEWAY_PREFIX", //
                "IM_1o1p4B_CLI_ID", //
                "IM_1o1p4B_CLI_SEC", //
                "IM_USCC", //
                "IM_ALLOW_SEID", //
                "IM_ALLOW_SEKEY");
    }
    public static ClientConfigGroup warningConfigGroup() {
        return new ClientConfigGroup("warningImHttpClient", //
                "cloudcmd:im:warning:auth-lock", //
                "cloudcmd:im:warning:token", //
                "IM_ADDRESS_HTTP", //
                "IM_ADDRESS_WS", //
                "IM_GATEWAY_PREFIX", //
                "IM_WARNING_CLI_ID", //
                "IM_WARNING_CLI_SEC", //
                "IM_USCC", //
                "IM_ALLOW_SEID", //
                "IM_ALLOW_SEKEY");
    }

    /**
     * 自动上下岗开关配置
     * @return
     */
    public static List<String> autoOnlineOfflineConfigNames() {
        return List.of("FEATURE_COMPATIBILITY_SIGN");
    }

    /**
     * 同步部门开关配置
     * @return
     */
    public static List<String> autoSyncDeptConfigNames() {
        return List.of("DEPARTMENT_SYNC_SIGN");
    }
}
