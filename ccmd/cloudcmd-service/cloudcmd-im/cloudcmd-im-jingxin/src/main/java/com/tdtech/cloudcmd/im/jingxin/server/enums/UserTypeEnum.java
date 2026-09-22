package com.tdtech.cloudcmd.im.jingxin.server.enums;

/**
 * @author: S063874
 * @date: 2026-01-15 15:34
 */
public enum UserTypeEnum {


    REGULAR_USER("1", "普通用户"),

    AGENT_USER( "2", "智能体用户");

    private String code;
    private String  desc;

    UserTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

}
