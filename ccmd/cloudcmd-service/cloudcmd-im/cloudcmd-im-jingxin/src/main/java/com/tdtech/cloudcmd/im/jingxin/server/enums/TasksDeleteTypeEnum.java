package com.tdtech.cloudcmd.im.jingxin.server.enums;

/**
 * @author ly
 * @date 2025/8/28
 **/
public enum TasksDeleteTypeEnum {
    REMOVE(1,"删除"),
    DISCARDED(2,"作废");

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

    TasksDeleteTypeEnum(int code, String msg){
        this.code = code;
        this.msg = msg;
    }

    public static TasksDeleteTypeEnum matchCode(int code) {
        TasksDeleteTypeEnum[] enumArray = TasksDeleteTypeEnum.values();
        for (int i = 0; i < enumArray.length; i++) {
            if (enumArray[i] != null && enumArray[i].getCode() == code) {
                return enumArray[i];
            }
        }
        return null;
    }

}
