package com.tdtech.cloudcmd.im.jingxin.server.enums;

/**
 * 任务URL打开方式枚举
 */
public enum TasksUrlOpenTypeEnum {
    NORMAL_H5(1, "普通H5"),
    FULLSCREEN_H5(2, "全屏H5"),
    JINGXIN_MINIAPP(3, "警信后台配置的H5小程序"),
    COLLABORATIVE_MINIAPP(4, "协同小程序");

    private final int code;
    private final String msg;

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    TasksUrlOpenTypeEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
