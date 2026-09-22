package com.tdtech.cloudcmd.im.jingxin.server.enums;

/**
 * @author ly
 * @date 2025/8/28
 * 任务处置动作。1：认领；2：转发；3：回退；4：处置；5：完成；
 **/
public enum TasksActionEnum {
    CLAIM(1, "认领"),
    FORWARDING(2, "转发"),
    BACK_OFF(3, "回退"),
    DISPOSITION(4, "处置"),
    COMPLETED(5, "完成");

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

    TasksActionEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static TasksActionEnum matchCode(int code) {
        TasksActionEnum[] enumArray = TasksActionEnum.values();
        for (int i = 0; i < enumArray.length; i++) {
            if (enumArray[i] != null && enumArray[i].getCode() == code) {
                return enumArray[i];
            }
        }
        return null;
    }

}
