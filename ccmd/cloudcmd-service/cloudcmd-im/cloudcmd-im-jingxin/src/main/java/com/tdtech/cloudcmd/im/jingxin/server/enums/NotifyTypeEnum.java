package com.tdtech.cloudcmd.im.jingxin.server.enums;

public enum NotifyTypeEnum {
    ON_DUTY_PERIOD(1, "值班临期"),
    PERMISSION_APPROVAL(2, "权限审批");

    private int code;
    private String desc;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    NotifyTypeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static NotifyTypeEnum matchCode(int code) {
        NotifyTypeEnum[] enumArray = NotifyTypeEnum.values();
        for (int i = 0; i < enumArray.length; i++) {
            if (enumArray[i] != null && enumArray[i].getCode() == code) {
                return enumArray[i];
            }
        }
        return null;
    }

}
