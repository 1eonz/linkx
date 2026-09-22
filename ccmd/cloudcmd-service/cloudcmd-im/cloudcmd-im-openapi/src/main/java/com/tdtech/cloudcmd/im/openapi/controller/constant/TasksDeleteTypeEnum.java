package com.tdtech.cloudcmd.im.openapi.controller.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author ly
 * @date 2025/8/28
 **/
@AllArgsConstructor
public enum TasksDeleteTypeEnum {
    REMOVE(1,"删除"),
    DISCARDED(2,"作废");

    @Getter
    private int code;
    @Getter
    private String msg;

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
