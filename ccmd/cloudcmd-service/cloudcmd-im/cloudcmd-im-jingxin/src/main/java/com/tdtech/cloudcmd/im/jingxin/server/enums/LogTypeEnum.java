package com.tdtech.cloudcmd.im.jingxin.server.enums;

/**
 * @author ly
 * @date 2025/8/28
 * <p>
 **/
public enum LogTypeEnum {
    INSERT(0, "新建"),
    UPDATE(1, "修改"),
    DELETE(2, "删除");

    private int code;
    private String msg;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    LogTypeEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public static LogTypeEnum matchCode(int code) {
        LogTypeEnum[] enumArray = LogTypeEnum.values();
        for (int i = 0; i < enumArray.length; i++) {
            if (enumArray[i] != null && enumArray[i].getCode() == code) {
                return enumArray[i];
            }
        }
        return null;
    }

}
