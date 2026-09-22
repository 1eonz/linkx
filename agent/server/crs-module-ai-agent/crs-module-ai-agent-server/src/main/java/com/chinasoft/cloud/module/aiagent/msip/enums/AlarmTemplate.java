package com.chinasoft.cloud.module.aiagent.msip.enums;

public interface AlarmTemplate {
    String getAlarmId();
    String getAlertName();
    String getTemplate();
    String getDescription();
    Integer getLevel();
    String getReason();
    String getRepairSuggestion();
}
