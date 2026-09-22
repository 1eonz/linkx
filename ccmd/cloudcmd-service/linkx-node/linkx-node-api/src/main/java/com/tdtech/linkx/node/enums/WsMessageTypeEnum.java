package com.tdtech.linkx.node.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * WebSocket消息类型枚举
 */
@Getter
@AllArgsConstructor
public enum WsMessageTypeEnum {

    REGISTER("register", "注册认证"),
    PING("ping", "心跳请求（客户端发送）"),
    PONG("pong", "心跳响应（服务端返回）"),
    MESSAGE("message", "业务消息"),
    DISCONNECT("disconnect", "断开连接通知");

    private final String type;
    private final String desc;

    public static WsMessageTypeEnum fromType(String type) {
        for (WsMessageTypeEnum msgType : values()) {
            if (msgType.getType().equals(type)) {
                return msgType;
            }
        }
        return null;
    }
}
