package com.chinasoft.cloud.module.aiagent.enums;

import com.chinasoft.cloud.framework.common.enums.RpcConstants;

/**
 * API 相关的枚举
 *
 * @author 芋道源码
 */
public class ApiConstants {

    /**
     * 服务名
     *
     * 注意，需要保证和 spring.application.name 保持一致
     */
    public static final String NAME = "ai-agent-server";

    public static final String PREFIX = RpcConstants.RPC_API_PREFIX + "/proxy/ai/v1/aiagent";

    public static final String VERSION = "1.0.0";

}
