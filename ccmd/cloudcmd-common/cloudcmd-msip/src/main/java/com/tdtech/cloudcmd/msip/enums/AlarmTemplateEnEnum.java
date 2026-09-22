package com.tdtech.cloudcmd.msip.enums;

import org.apache.commons.lang.StringUtils;

/**
 * @author 告警模板——英文版
 * @date 2026/02/03
 **/
public enum AlarmTemplateEnEnum implements AlarmTemplate {
    INSUFFICIENT_DISK_SPACE("5100", "Group Archive Space Insufficient Alarm", "Insufficient archive disk space {{size}}", "Insufficient archive disk space", 2, "Insufficient remaining disk space in group archive path", "Expand space; Delete redundant files; Redirect to new disk space (parameter-configurable switching supported)"),
    IM_SERVER_IS_DISCONNECTED("5200", "Disconnected from IM Server", "Server disconnected from IM: {{exception}}", "Server disconnected from IM", 1, "LinkX disconnected from IM server", "Check network connection; Verify account credentials"),
    THE_AGENT_IS_DISCONNECTED("5210", "Disconnected from Agent", "Server disconnected from agent", "Server disconnected from agent", 2, "LinkX disconnected from agent", "Check network connection; Verify account credentials"),
    THIRD_PARTY_SYSTEM_DISCONNECTION("5220", "Disconnected from Third-Party System", "Server disconnected from third-party system", "Server disconnected from third-party system", 2, "LinkX disconnected from third-party system", "Check network connection; Verify account credentials"),
    TOO_MANY_REQUESTS("5300", "Concurrent Access Exceeds Threshold", "Third-party server concurrent access exceeds threshold for {{business_name}} business: {{limit_count}}", "Third-party server concurrent access exceeds threshold", 3, "Excessive concurrent access to third-party interface", "Notify third-party to control concurrent access or adjust LinkX specifications"),
    EXCEEDED_OVER80("5400", "Usage Reaches Threshold", "Usage reaches 80% threshold", "Usage reaches threshold", 1, "LINKXNum control item has reached threshold", "License control item has reached threshold,建议 reduce connected users"),
    EXCEEDED_OVER100("5500", "Usage Reaches Threshold", "Usage reaches 100% threshold", "Usage reaches threshold", 1, "LINKXNum control item has reached threshold", "License control item has reached threshold,建议 reduce connected users"),
    SYSTEM_ACCOUNT_ERROR("5510", "System Account Login Exception", "Please check if the {{account}} virtual account password for IM service is correct", "System account login exception", 2, "Account login failed", "Please check if account credentials are correct"),
    DATABASE_ERROR("5511", "Database Connection Exception", "Database connection exception", "Please check if database account credentials are correct", 2, "Database connection failed", "Please check if database account credentials are correct"),
    SUPER_ADMINISTRATOR_ACCOUNT_LOCKOUT("5520", "Super Administrator Account Locked", "Super administrator account {{id_card}} is locked", "Super administrator account is locked", 3, "Super administrator account password incorrect too many times", "Wait for lock timeout or contact development support for unlocking"),
    ADMINISTRATOR_ACCOUNT_LOCKOUT("5530", "Administrator Account Locked", "Administrator account is locked", "Administrator account is locked", 4, "Administrator account password incorrect too many times", "Wait for lock timeout or super administrator assistance for unlocking"),
    IM_SERVER_IP_ADDRESS_IS_NOT_CONFIGURED("5601", "IM Server IP Not Configured", "IM server IP configuration not set", "IM server IP configuration not set", 1, "IM server IP configuration not detected", "Set IM server IP in police collaboration management backend"),
    IM_SERVER_PORT_IS_NOT_CONFIGURED("5602", "IM Server Port Not Configured", "IM server port configuration not set", "IM server port configuration not set", 1, "IM server port configuration not detected", "Set IM server port in police collaboration management backend"),
    IM_SERVER_ACCOUNT_AND_PASSWORD_IS_NOT_CONFIGURED("5603", "IM Server Account Credentials Not Configured", "IM server account credentials not set", "IM server account credentials not set", 1, "IM server account credentials configuration not detected", "Set IM server account credentials in police collaboration management backend"),
    INVOKING_THE_IM_TO_CREATE_A_GROUP_FAILED("5700", "IM Group Creation Failed", "User {{user_name}} failed to create group, query content: {{content}}; response: failed.", "User failed to create IM group", 2, "IM invocation failed", "Check IM service status"),
    INVOKING_THE_IM_TO_QUERY_CHAT_RECORDS_FAILED("5701", "IM Chat History Query Failed", "User {{user_name}} failed to query chat history, query content: {{content}}; response: failed.", "User failed to query IM chat history", 2, "IM invocation failed", "Check IM service status"),
    INVOKING_THE_IM_TO_QUERY_USER_INFO_FAILED("5702", "IM User Info Query Failed", "User {{user_name}} failed to query IM info, query content: {{content}}; response: failed.", "User failed to query IM user info", 2, "IM invocation failed", "Check IM service status"),
    INVOKING_THE_IM_TO_SEND_MSG_FAILED("5703", "IM Message Sending Failed", "User {{user_name}} failed to send IM message, content: {{content}}; response: failed.", "User failed to send IM message", 2, "IM invocation failed", "Check IM service status"),
    INVOKING_THE_AI_AGENT_FAILED("5760", "AI Agent Invocation Failed", "User {{user_name}} failed to invoke AI agent, content: {{content}}; response: failed.", "User failed to invoke AI agent", 2, "AI invocation failed", "Check AI service status"),
    INVOKING_PERSONNEL_CHECK_FAILED("5761", "Personnel Verification Failed", "User {{user_name}} failed to invoke personnel verification, content: {{content}}; response: failed.", "User failed to invoke personnel verification", 2, "Personnel verification failed", "Check personnel verification system status"),
    INVOKING_THE_ALARM_TICKET_SYSTEM_FAILED("5762", "Alarm Ticket System Invocation Failed", "User {{user_name}} failed to invoke alarm ticket system, content: {{content}}; response: failed.", "User failed to invoke alarm ticket system", 2, "Alarm ticket data invocation failed", "Check alarm ticket system status");


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

    AlarmTemplateEnEnum(String alarmId, String alertName, String template, String description, Integer level, String reason, String repairSuggestion) {
        this.alarmId = alarmId;
        this.alertName = alertName;
        this.template = template;
        this.description = description;
        this.level = level;
        this.reason = reason;
        this.repairSuggestion = repairSuggestion;
    }

    public static AlarmTemplateEnEnum matchAlarmId(String alarmId) {
        AlarmTemplateEnEnum[] enumArray = AlarmTemplateEnEnum.values();
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
