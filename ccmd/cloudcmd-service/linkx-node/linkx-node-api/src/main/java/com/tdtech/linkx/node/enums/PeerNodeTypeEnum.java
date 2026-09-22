package com.tdtech.linkx.node.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 节点类型枚举
 */
@Getter
@AllArgsConstructor
public enum PeerNodeTypeEnum {

    SERVER(1, "server", "连入本机的客户端"),
    CLIENT(2, "client", "本机连出的服务端");

    private final int code;
    private final String name;
    private final String desc;

    public static PeerNodeTypeEnum fromCode(int code) {
        for (PeerNodeTypeEnum type : values()) {
            if (type.getCode() == code) {
                return type;
            }
        }
        return null;
    }
}
