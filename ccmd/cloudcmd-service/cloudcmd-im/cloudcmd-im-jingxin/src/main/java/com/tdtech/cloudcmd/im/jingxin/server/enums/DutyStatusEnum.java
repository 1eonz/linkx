package com.tdtech.cloudcmd.im.jingxin.server.enums;

/**
 * @author ly
 * @date 2025/8/28
 * 是否已处理：0未处理，1已处理
 **/
public enum DutyStatusEnum {
    UNPROCESSED(0, "未处理"),
    PROCESSED(1, "已处理");

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

    DutyStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static DutyStatusEnum matchCode(int code) {
        DutyStatusEnum[] enumArray = DutyStatusEnum.values();
        for (int i = 0; i < enumArray.length; i++) {
            if (enumArray[i] != null && enumArray[i].getCode() == code) {
                return enumArray[i];
            }
        }
        return null;
    }

}
