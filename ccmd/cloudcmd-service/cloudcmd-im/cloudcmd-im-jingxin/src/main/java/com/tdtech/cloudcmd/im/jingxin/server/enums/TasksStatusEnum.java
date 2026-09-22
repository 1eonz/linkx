package com.tdtech.cloudcmd.im.jingxin.server.enums;

/**
 * @author ly
 * @date 2025/8/28
 **/
public enum TasksStatusEnum {
    TO_BE_PROCESSED(1,"待处理"),
    IN_PROGRESS(2,"进行中"),

    COMPLETED(3,"已完成"),

    DISCARDED(4,"已作废");

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

    TasksStatusEnum(int code, String msg){
        this.code = code;
        this.msg = msg;
    }

    public static TasksStatusEnum matchCode(int code) {
        TasksStatusEnum[] enumArray = TasksStatusEnum.values();
        for (int i = 0; i < enumArray.length; i++) {
            if (enumArray[i] != null && enumArray[i].getCode() == code) {
                return enumArray[i];
            }
        }
        return null;
    }

}
