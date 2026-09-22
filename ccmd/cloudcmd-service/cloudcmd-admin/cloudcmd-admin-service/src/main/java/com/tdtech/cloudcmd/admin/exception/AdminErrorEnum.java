package com.tdtech.cloudcmd.admin.exception;

/**
 * @author zWX523748
 * @date 2020/6/1 16:46
 */
public enum AdminErrorEnum {

    COMMON_ERROR_511(511, "AdminErrorEnum_COMMON_ERROR_511"),
    COMMON_ERROR_513(513, "AdminErrorEnum_COMMON_ERROR_513"),
    COMMON_ERROR_514(514, "AdminErrorEnum_COMMON_ERROR_514"),
    COMMON_ERROR_518(518, "AdminErrorEnum_COMMON_ERROR_518"),
    COMMON_ERROR_563(563, "AdminErrorEnum_COMMON_ERROR_563"),
    COMMON_ERROR_537(537, "AdminErrorEnum_COMMON_ERROR_537");

    private int code;
    private String msg;

    AdminErrorEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return this.code;
    }

    public String getMsg() {
        return this.msg;
    }

}
