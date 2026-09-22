package com.tdtech.cloudcmd.admin.util.enums;

import com.tdtech.cloudcmd.i18n.I18nUtil;
import lombok.Getter;

@Getter
public enum SystemConfigType {

    /**
     * 公共设置 - 系统名称
     */
    SYSTEM_NAME(2031323063807125505L, I18nUtil.get("SYSTEM_CONFIG_COMMON")),
    
    /**
     * 公共设置 - 应用排版
     */
    APP_COUNT_IN_ROW(2031324046037624834L, I18nUtil.get("SYSTEM_CONFIG_COMMON")),
    
    /**
     * 公共设置 - 建群配置
     */
    CREAT_GROUP_CONFIG(2031324046037624835L, I18nUtil.get("SYSTEM_CONFIG_COMMON")),
    
    /**
     * PC端设置 - 导航自定义
     */
    PC_NAV_CUSTOM(2031324046037624836L, I18nUtil.get("SYSTEM_CONFIG_PC"));
    
    private final Long id;
    private final String name;

    SystemConfigType(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public static String getNameById(Long id) {
        if (id == null) {
            return I18nUtil.get("SYSTEM_CONFIG_UNKNOWN");
        }
        for (SystemConfigType type : values()) {
            if (type.getId().equals(id)) {
                return type.getName();
            }
        }
        return I18nUtil.get("SYSTEM_CONFIG_UNKNOWN");
    }
}
