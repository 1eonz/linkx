package com.chinasoft.cloud.module.aiagent.msip.enums;

import org.apache.commons.lang3.StringUtils;

/**
 * @author 告警模板——中文版
 * @date 2026/02/03
 **/
public enum AlarmTemplateZhEnum implements AlarmTemplate {
    INVOKING_THE_AI_AGENT_FAILED("5760", "调用AI智能体失败", "{{user_name}}用户调用AI智能体失败，调用内容为：{{content}}；响应为：失败。", "用户调用AI智能体失败",2, "AI调用失败", "检查AI服务状态");

    /**
     * 告警定义ID
     */
    private String alarmId;

    /**
     * 告警名称
     */
    private String alertName;

    /**
     * 模板
     */
    private String template;

    /**
     * 告警描述
     */
    private String description;

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
    private String repairSuggestion;

    AlarmTemplateZhEnum(String alarmId, String alertName, String template, String description, Integer level, String reason, String repairSuggestion) {
        this.alarmId = alarmId;
        this.alertName = alertName;
        this.template = template;
        this.description = description;
        this.level = level;
        this.reason = reason;
        this.repairSuggestion = repairSuggestion;
    }

    public static AlarmTemplateZhEnum matchAlarmId(String alarmId) {
        AlarmTemplateZhEnum[] enumArray = AlarmTemplateZhEnum.values();
        for (int i = 0; i < enumArray.length; i++) {
            if (enumArray[i] != null && StringUtils.equals(enumArray[i].getAlarmId(), alarmId)) {
                return enumArray[i];
            }
        }
        return null;
    }

    public String getAlarmId() {
        return alarmId;
    }

    public String getAlertName() {
        return alertName;
    }

    public String getTemplate() {
        return template;
    }

    public String getDescription() {
        return description;
    }

    public Integer getLevel() {
        return level;
    }

    public String getReason() {
        return reason;
    }

    public String getRepairSuggestion() {
        return repairSuggestion;
    }
}
