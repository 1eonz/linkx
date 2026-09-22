package com.tdtech.cloudcmd.enums;

/**
 * @author :布控任务类型
 */
public enum NotifyTypeEnum {

    CREATE("CREATE", "新增"),

    UPDATE("UPDATE", "修改"),

    DELETE("DELETE", "删除"),

    STATUS_CHANGE("STATUS_CHANGE", "状态变更");

    private String code;
    private String msg;

    private NotifyTypeEnum(String code, String msg) {
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
