package com.chinasoft.cloud.module.aiagent.msip.enums;

/**
 * @author ly
 * @date 2025/8/28
 **/
public enum OperationTypeEnum {
    AGENT_INSERT("创建智能体", "创建了%s智能体", SubClassificationEnum.AGENT, LevelEnum.LEVEL_1),

    AGENT_UPDATE("修改智能体", "修改了%s智能体", SubClassificationEnum.AGENT, LevelEnum.LEVEL_2),

    AGENT_DELETE("删除智能体", "删除了%s智能体", SubClassificationEnum.AGENT, LevelEnum.LEVEL_2),

    AGENT_IM_ASK("接收智能体提问", "imExtra:用户%s提问%s智能体,参数:%s", SubClassificationEnum.AGENT, LevelEnum.LEVEL_2);

    private String name;
    private String desc;
    private SubClassificationEnum parent;
    private LevelEnum level;

    OperationTypeEnum(String name, String desc, SubClassificationEnum parent, LevelEnum level) {
        this.name = name;
        this.desc = desc;
        this.parent = parent;
        this.level = level;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public LevelEnum getLevel() {
        return level;
    }

    public SubClassificationEnum getParent() {
        return parent;
    }
}