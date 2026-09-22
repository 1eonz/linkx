package com.tdtech.cloudcmd.im.jingxin.server.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * IM 系统群组类型（对应 GroupCreateReq.type）
 */
@AllArgsConstructor
@Getter
public enum ImGroupTypeEnum {
    CHAT_GROUP(1, "普通群组"),
    COLLABORATION_GROUP(3, "协同群组");

    private final int value;
    private final String desc;
}