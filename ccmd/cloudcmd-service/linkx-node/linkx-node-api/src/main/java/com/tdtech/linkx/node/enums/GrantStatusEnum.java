package com.tdtech.linkx.node.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 节点授权/授权状态枚举
 */
@Getter
@AllArgsConstructor
public enum GrantStatusEnum {

    UNAUTHORIZED(0, "未授权"),
    AUTHORIZED(1, "已授权"),
    REJECTED(2, "拒绝");

    private final int code;
    private final String desc;

    public static GrantStatusEnum fromCode(int code) {
        for (GrantStatusEnum status : values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        return null;
    }
}
