package com.tdtech.cloudcmd.msip.enums;

/**
 * @author ly
 * @date 2025/11/17
 **/
public enum LicenseEnum {
    ALL("LINKXBS", "警务协同平台基本软件"),
    GROUP_COLLABORATION("LINKXGCF", "群组协同功能"),
    TASK_COORDINATION("LINKXTCF", "任务协同功能"),
    BUSINESS_COLLABORATION("LINKXBCF", "业务协同功能"),
    COMMUNICATION_COLLABORATION("LINKXCCF", "通信协同功能"),
    AI_COLLABORATION("LINKXACF", "AI协同功能"),
    STATISTICS_BOARD("LINKXSDF", "统计看板功能"),
    NORTHBOUND_DATA("LINKXNDI", "北向数据接口"),
    ACTIVE_STANDBY("LINKXPSF", "主备功能"),
    ACCESSABLE_NUMBER("LINKXNum", "可接入数量");

    /**
     * 模块编码
     */
    private String code;

    /**
     * 描述
     */
    private String desc;


    LicenseEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
