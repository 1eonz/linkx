package com.tdtech.cloudcmd.enums;

public enum ConnectTypeEnum {
    ALL("2", "ResponseCodeEnum_CONNECT_TYPE_2"), FAILURE("1", "ResponseCodeEnum_CONNECT_TYPE_1"),
    SUCCESS("0", "ResponseCodeEnum_CONNECT_TYPE_0");

    private String code;
    private String msg;

    private ConnectTypeEnum(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public String getCode() {
        return this.code;
    }

    public String getMsg() {
        return this.msg;
    }
}
