package com.tdtech.cloudcmd.admin.util.enums;

import java.util.Arrays;

public enum CreateGroupType {

    /**
     * 自定义建群
     */
    CUSTOM(1, "CREATE_GROUP_TYPE_CUSTOM"),

    /**
     * 一键建群
     */
    ONE_CLICK(2, "CREATE_GROUP_TYPE_ONE_CLICK"),

    /**
     * 职能建群
     */
    FUNCTIONAL(3, "CREATE_GROUP_TYPE_FUNCTIONAL"),

    /**
     * 一键调度
     */
    ONE_CLICK_SCHEDULING(4, "CREATE_GROUP_TYPE_ONE_CLICK_SCHEDULING");

    private final Integer code;
    private final String name;

    CreateGroupType(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public Integer getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static CreateGroupType codeOf(Integer code) {
        return Arrays.stream(values())
                .filter(type -> type.code.equals(code))
                .findFirst()
                .orElse(null);
    }

    public static CreateGroupType nameOf(String name) {
        return Arrays.stream(values())
                .filter(type -> type.name.equals(name))
                .findFirst()
                .orElse(null);
    }
}
