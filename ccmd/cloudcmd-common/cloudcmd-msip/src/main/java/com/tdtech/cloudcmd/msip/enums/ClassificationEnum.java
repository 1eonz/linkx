package com.tdtech.cloudcmd.msip.enums;

/**
 * @author ly
 * @date 2025/11/17
 **/
public enum ClassificationEnum {
    RESOURCE("资源管理"),

    THIRD("三方对接"),

    ACCOUNT("账户"),

    PERMISSION("权限"),

    SYSTEM("系统管理"),

    JINGXIN("警信扩展信息管理");

    private String name;

    ClassificationEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
