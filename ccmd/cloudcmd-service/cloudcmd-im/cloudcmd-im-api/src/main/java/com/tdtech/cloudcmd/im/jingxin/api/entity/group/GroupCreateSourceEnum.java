package com.tdtech.cloudcmd.im.jingxin.api.entity.group;
 
import lombok.AllArgsConstructor;
import lombok.Getter;
 
/**
 * 建群来源枚举
 * 对应 OpenApiCreateGroupCOV3.subType 与 CreateGroupCO/CreateGroupCOV2.source 字段
 */
@AllArgsConstructor
@Getter
public enum GroupCreateSourceEnum {
    ONE_KEY(1, "一键建群"),
    CUSTOM(3, "自定义建群"),
    ONE_KEY_DISPATCH(5, "一键调度");
 
    private final int value;
    private final String desc;
 
    public static GroupCreateSourceEnum of(Integer value) {
        if (value == null) {
            return ONE_KEY;
        }
        for (GroupCreateSourceEnum s : values()) {
            if (s.value == value) {
                return s;
            }
        }
        return null;
    }
}