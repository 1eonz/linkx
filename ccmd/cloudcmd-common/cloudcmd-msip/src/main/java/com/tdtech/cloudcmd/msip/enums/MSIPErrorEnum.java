package com.tdtech.cloudcmd.msip.enums;

/**
 * @author zWX523748
 * @date 2020/6/1 16:46
 */
public enum MSIPErrorEnum {

    SYSTEM_FUNCTIONS_ARE_LIMITED(182, "系统功能受限，请联系管理员");

    private int code;
    private String msg;

    private MSIPErrorEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return this.code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return this.msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

}
