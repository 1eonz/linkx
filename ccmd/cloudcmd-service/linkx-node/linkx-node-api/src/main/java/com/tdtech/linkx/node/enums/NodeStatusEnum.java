package com.tdtech.linkx.node.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 节点状态枚举（统一server/client）
 */
@Getter
@AllArgsConstructor
public enum NodeStatusEnum {

    DISCONNECTED(0, "未连接"),
    CONNECTING(1, "连接中"),
    CONNECTED(2, "已连接"),
    AUTHENTICATING(3, "认证中"),
    AUTHENTICATED(4, "已认证"),
    AUTH_FAILED(5, "认证失败"),
    RECONNECTING(6, "重连中");

    private final int code;
    private final String desc;

    public static NodeStatusEnum fromCode(int code) {
        for (NodeStatusEnum status : values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        return null;
    }
}
