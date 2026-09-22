package com.tdtech.cloudcmd.im.openapi.controller.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;

/**
 * @author ly
 * @date 2025/8/28
 **/
@AllArgsConstructor
public enum BusinessScopeEnum {
    COLLABORATIVE_STATISTICS("0","协同统计"),
    THIRD_PARTY_APPLICATION("1","三方应用"),
    THIRD_PARTY_TASKS("2","三方任务"),
    POLICE_TICKET("3","警单"),
    GROUP("4", "群组"),
    AGENT_USER("5", "agent服务用户"),
    DUTY_SCHEDULE("6", "排班管理"),
    IM_USER("7", "警信用户"),
    IM_MESSAGE("8", "警信消息"),
    APPLICATION("9", "应用信息");

    @Getter
    private String code;
    @Getter
    private String msg;

    public static BusinessScopeEnum matchCode(String code) {
        BusinessScopeEnum[] enumArray = BusinessScopeEnum.values();
        for (int i = 0; i < enumArray.length; i++) {
            if (enumArray[i] != null && StringUtils.equals(enumArray[i].getCode(), code)) {
                return enumArray[i];
            }
        }
        return null;
    }

}
