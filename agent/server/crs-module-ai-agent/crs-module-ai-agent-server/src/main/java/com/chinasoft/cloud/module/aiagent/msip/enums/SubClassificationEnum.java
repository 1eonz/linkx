package com.chinasoft.cloud.module.aiagent.msip.enums;

/**
 * @author ly
 * @date 2025/8/28
 **/
public enum SubClassificationEnum {
    AGENT("智能体", ClassificationEnum.RESOURCE);

    private String name;
    private ClassificationEnum parent;

    SubClassificationEnum(String name, ClassificationEnum parent) {
        this.name = name;
        this.parent = parent;
    }

    public String getName() {
        return name;
    }

    public ClassificationEnum getParent() {
        return parent;
    }
}
