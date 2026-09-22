package com.tdtech.cloudcmd.im.jingxin.server.enums;

/**
 * @author ly
 * @date 2025/8/28
 * <p>
 * 任务类型，先写死，后面根据业务需求看是否写入字典灵活配置
 **/
public enum TasksTypeEnum {
    POLICE(1, "110警情"),
    INTELLIGENCE_EARLY_WARNING(2, "情报预警"),
    BASIC_CONTROL(3, "基础掌控"),
    PUBLIC_OPINION_AND_WISDOM(4, "民意智感"),
    SCHEDULE_MANAGEMENT(5, "日程管理"),
    SPECIAL_APPROVAL(6, "特办审批"),
    OFFICIAL_DOCUMENT_CIRCULATION(7, "公文流转"),
    INFORM_INFORMATION(8, "通知情报");

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

    TasksTypeEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public static TasksTypeEnum matchCode(int code) {
        TasksTypeEnum[] enumArray = TasksTypeEnum.values();
        for (int i = 0; i < enumArray.length; i++) {
            if (enumArray[i] != null && enumArray[i].getCode() == code) {
                return enumArray[i];
            }
        }
        return null;
    }

}
