package com.tdtech.cloudcmd.enums;

/**
 * @author :布控任务类型
 */
public enum SuspectTaskEnum {

    SUCCESS(0, "成功"), FAILURE(1, "失败"), NULL_PARAM(2, "参数为空"), SQL_EXCEPTION(9, "系统异常"), INVALID_PARAM(10, "参数校验失败"),
    DB_CREATE_ERROR(31, "数据插入异常"), DB_DELETE_ERROR(32, "数据删除异常"), DB_UPDATE_ERROR(33, "数据更新异常"),
    NO_PERMISSION(401, "没有相应权限");

    ;

    private int code;
    private String msg;

    private SuspectTaskEnum(int code, String msg) {
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
