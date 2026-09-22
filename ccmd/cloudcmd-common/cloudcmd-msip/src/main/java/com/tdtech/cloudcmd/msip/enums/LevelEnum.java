package com.tdtech.cloudcmd.msip.enums;

/**
 * @author ly
 * @date 2025/8/28
 **/
public enum LevelEnum {
    LEVEL_1("1级"),
    LEVEL_2("2级"),
    LEVEL_3("3级"),
    LEVEL_4("4级");

    private String name;

    LevelEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
