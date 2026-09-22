package com.tdtech.cloudcmd.admin.util.enums;

import com.tdtech.cloudcmd.i18n.I18nUtil;

public enum LayoutSectionType {
    
    /**
     * 轮播图
     */
    CAROUSEL(1, "LAYOUT_SECTION_TYPE_CAROUSEL"),
    
    /**
     * 常用应用
     */
    COMMON_APP(2, "LAYOUT_SECTION_TYPE_COMMON_APP"),
    
    /**
     * 协同群组
     */
    COLLABORATION_GROUP(3, "LAYOUT_SECTION_TYPE_COLLABORATION_GROUP"),
    
    /**
     * 三方网页
     */
    THIRD_PARTY_WEB(4, "LAYOUT_SECTION_TYPE_THIRD_PARTY_WEB"),
    
    /**
     * 分割条
     */
    DIVIDER(5, "LAYOUT_SECTION_TYPE_DIVIDER"),
    
    /**
     * 消息列表
     */
    MESSAGE_LIST(6, "LAYOUT_SECTION_TYPE_MESSAGE_LIST");
    
    private final Integer code;
    private final String nameKey;
    
    LayoutSectionType(Integer code, String nameKey) {
        this.code = code;
        this.nameKey = nameKey;
    }
    
    public Integer getCode() {
        return code;
    }
    
    public String getNameKey() {
        return nameKey;
    }

    public static String getNameByCode(Integer code) {
        if (code == null) {
            return I18nUtil.get("LAYOUT_SECTION_TYPE_UNKNOWN");
        }
        for (LayoutSectionType type : values()) {
            if (type.getCode().equals(code)) {
                return I18nUtil.get(type.getNameKey());
            }
        }
        return I18nUtil.get("LAYOUT_SECTION_TYPE_UNKNOWN");
    }

    public static LayoutSectionType getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (LayoutSectionType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
