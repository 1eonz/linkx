package com.chinasoft.cloud.module.aiagent.msip.enums;

/**
 * @author ly
 * @date 2025/11/17
 **/
public enum AlertDefineEnum {
    INVOKING_THE_AI_AGENT_FAILED("5760", "调用AI智能体失败", "用户调用AI智能体失败", "%s用户调用AI智能体失败，调用内容为：%s；响应为：失败。",2, "AI调用失败", "检查AI服务状态");

    /**
     * 告警定义ID
     */
    private String alertDefineId;

    /**
     * 告警名称
     */
    private String alertName;

    /**
     * 告警描述
     */
    private String description;

    /**
     * 告警详情，统一使用占位符，然后格式化上报给msip
     */
    private String detailTemplate;

    /**
     * 告警等级1-5(数字越小越严重)
     */
    private Integer level;

    /**
     * 告警原因
     */
    private String reason;

    /**
     * 修复建议
     */
    private String repairSug;

    AlertDefineEnum(String alertDefineId, String alertName, String description, Integer level, String reason, String repairSug) {
        this.alertDefineId = alertDefineId;
        this.alertName = alertName;
        this.description = description;
        this.level = level;
        this.reason = reason;
        this.repairSug = repairSug;
    }

    AlertDefineEnum(String alertDefineId, String alertName, String description, String detailTemplate, Integer level, String reason, String repairSug) {
        this(alertDefineId, alertName, description, level, reason, repairSug);
        this.detailTemplate = detailTemplate;
    }

    public String getAlertDefineId() {
        return alertDefineId;
    }

    public String getAlertName() {
        return alertName;
    }

    public String getDescription() {
        return description;
    }

    public String getDetailTemplate() {
        return detailTemplate;
    }

    public Integer getLevel() {
        return level;
    }

    public String getReason() {
        return reason;
    }

    public String getRepairSug() {
        return repairSug;
    }
}
