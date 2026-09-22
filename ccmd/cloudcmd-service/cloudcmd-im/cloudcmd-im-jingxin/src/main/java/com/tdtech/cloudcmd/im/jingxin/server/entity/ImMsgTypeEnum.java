package com.tdtech.cloudcmd.im.jingxin.server.entity;

import lombok.Getter;

/**
 * IM消息类型枚举
 */
@Getter
public enum ImMsgTypeEnum {

    /**
     * 文本消息
     */
    TEXT_MESSAGE(1, "文本消息"),

    /**
     * 彩信消息
     */
    MEDIA_MSG(2, "彩信消息"),

    /**
     * 位置共享
     */
    LOCATION_SHARE(3, "位置共享"),

    /**
     * 已读回执
     */
    READ_RECEIPT(4, "已读回执"),

    /**
     * 名片
     */
    BUSINESS_CARD(5, "名片"),

    /**
     * 群接龙
     */
    GROUP_SOLICIT(6, "群接龙"),

    /**
     * 撤回消息
     */
    RECALL_MESSAGE(7, "撤回消息"),

    /**
     * 合并转发
     */
    MERGE_FORWARD(8, "合并转发"),

    /**
     * CM群组变更事件
     */
    CM_GROUP_CHANGE_EVENT(10, "CM群组变更事件"),

    /**
     * 新警通110系统的通知消息
     */
    NEW_POLICE_ALERT(11, "新警通110系统的通知消息");

    /**
     * 消息类型编码
     */
    private final Integer code;

    /**
     * 消息类型描述
     */
    private final String description;

    ImMsgTypeEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据编码获取枚举实例
     *
     * @param code 消息类型编码
     * @return 对应的枚举实例，若不存在则返回null
     */
    public static ImMsgTypeEnum fromCode(Integer code) {
        for (ImMsgTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}