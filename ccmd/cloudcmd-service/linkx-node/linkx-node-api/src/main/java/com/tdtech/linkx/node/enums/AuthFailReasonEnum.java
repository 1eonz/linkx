package com.tdtech.linkx.node.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 认证失败原因枚举
 */
@Getter
@AllArgsConstructor
public enum AuthFailReasonEnum {

    NOT_AUTHORIZED("not_authorized", "节点未授权", 1),
    AUTH_EXPIRED("auth_expired", "授权已过期", 0),
    JWT_INVALID("jwt_invalid", "JWT验证失败", 0),
    CHALLENGE_INVALID("challenge_invalid", "挑战码无效", 0),
    OTHER("other", "其他错误", 0);

    /**
     * 错误码
     */
    private final String code;

    /**
     * 错误描述
     */
    private final String desc;

    /**
     * 重试策略
     * 0: 正常重试（60秒）
     * 1: 降低频率重试（5分钟）
     * 2: 不重试
     */
    private final int retryStrategy;

    public static AuthFailReasonEnum fromCode(String code) {
        for (AuthFailReasonEnum reason : values()) {
            if (reason.getCode().equals(code)) {
                return reason;
            }
        }
        return OTHER;
    }
}
