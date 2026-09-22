package com.chinasoft.cloud.module.aiagent.msip.enums;

/**
 * @author ly
 * @date 2025/11/17
 **/
public enum ClassificationEnum {
    RESOURCE("资源管理");

    private String name;

    ClassificationEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
