package com.tdtech.cloudcmd.im.jingxin.server.enums;

import org.apache.commons.lang.StringUtils;

/**
 * @author ly
 * @date 2025/8/28
 **/
public enum ApplicationTypeEnum {
    COLLABORATIVE_STATISTICS("0","协同统计"),
    THIRD_PARTY_APPLICATION("1","三方应用"),
    THIRD_PARTY_TASKS("2","三方任务");

    private String code;
    private String msg;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    ApplicationTypeEnum(String code, String msg){
        this.code = code;
        this.msg = msg;
    }

    public static ApplicationTypeEnum matchCode(String code) {
        ApplicationTypeEnum[] enumArray = ApplicationTypeEnum.values();
        for (int i = 0; i < enumArray.length; i++) {
            if (enumArray[i] != null && StringUtils.equals(enumArray[i].getCode(), code)) {
                return enumArray[i];
            }
        }
        return null;
    }

}
