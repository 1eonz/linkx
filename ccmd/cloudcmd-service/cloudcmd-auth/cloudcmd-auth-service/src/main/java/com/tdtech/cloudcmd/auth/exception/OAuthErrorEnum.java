package com.tdtech.cloudcmd.auth.exception;

/**
 * @author zWX523748
 * @date 2020/6/1 16:46
 */
public enum OAuthErrorEnum {

    COMMON_ERROR_523(523, "OAuthErrorEnum_COMMON_ERROR_523"), COMMON_ERROR_524(524, "OAuthErrorEnum_COMMON_ERROR_524"),;

    private int code;
    private String msg;

    private OAuthErrorEnum(int code, String msg) {
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
