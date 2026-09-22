package com.tdtech.linkx.node.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * WebSocket消息子类型枚举
 */
@Getter
@AllArgsConstructor
public enum WsMessageSubTypeEnum {

    REQUEST("request", "注册认证请求（服务端发起挑战）"),
    RESPONSE("response", "注册认证响应（客户端返回JWT）"),
    SUCCESS("success", "注册认证成功"),
    FAILURE("failure", "注册认证失败"),
    AUTH("auth", "授权通知"),
    UNAUTH("unauth", "取消授权通知"),
    SHARE("share", "协同岗分享"),
    UNSHARE("unshare", "取消协同岗分享"),
    STATISTIC("statistic", "统计数据请求/响应"),
    COOP_SEARCH("coop_search", "协同岗搜索请求/响应");

    private final String type;
    private final String desc;

    public static WsMessageSubTypeEnum fromType(String type) {
        for (WsMessageSubTypeEnum subType : values()) {
            if (subType.getType().equals(type)) {
                return subType;
            }
        }
        return null;
    }
}
