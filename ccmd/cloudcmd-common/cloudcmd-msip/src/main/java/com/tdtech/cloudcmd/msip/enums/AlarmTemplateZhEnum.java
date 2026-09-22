package com.tdtech.cloudcmd.msip.enums;

import org.apache.commons.lang.StringUtils;

/**
 * @author 告警模板——中文版
 * @date 2026/02/03
 **/
public enum AlarmTemplateZhEnum implements AlarmTemplate {
    INSUFFICIENT_DISK_SPACE("5100", "群组归档空间不足告警", "归档磁盘空间不足{{size}}%", "归档磁盘空间不足", 2, "群组归档所在路径剩余磁盘空间不足", "扩展空间；删除多余文件；重新指向新的磁盘空间（支持参数可配切换）"),
    IM_SERVER_IS_DISCONNECTED("5200", "与警信服务器断链", "服务器与警信断连：{{exception}}", "服务器与警信断连", 1, "LinkX和警信服务器断连", "排查网络连接正常与否；检查账号密码是否正确"),
    THE_AGENT_IS_DISCONNECTED("5210", "与agent智能体断链", "服务器与agent智能体断连", "服务器与agent智能体断连", 2, "LinkX和agent智能体断连", "排查网络连接正常与否；检查账号密码是否正确"),
    THIRD_PARTY_SYSTEM_DISCONNECTION("5220", "与三方系统断链", "服务器与三方系统断连", "服务器与三方系统断连", 2, "LinkX和三方系统断连", "排查网络连接正常与否；检查账号密码是否正确"),
    TOO_MANY_REQUESTS("5300", "并发访问超出阈值", "三方服务器并发访问超过{{business_name}}业务阈值：{{limit_count}}", "三方服务器并发访问超过阈值", 3, "三方接口访问并发度过高", "通知三方做好并发访问控制或调整LinkX规格"),
    EXCEEDED_OVER80("5400", "使用量达到阈值", "使用量达到80%阈值", "使用量达到阈值", 1, "LINKXNum控制项已达到阈值", "license控制项已达到阈值，建议减少接入用户"),
    EXCEEDED_OVER100("5500", "使用量达到阈值", "使用量达到100%阈值", "使用量达到阈值", 1, "LINKXNum控制项已达到阈值", "license控制项已达到阈值，建议减少接入用户"),
    //    SYSTEM_ACCOUNT_LOCKOUT("5510", "系统账号锁定", "系统账号锁定", 2, "系统账号密码错误次数过多", "等待锁定超时或联系开发支持解锁"),
    SYSTEM_ACCOUNT_ERROR("5510", "系统账号登录异常", "请检查警信业务的{{account}}虚拟账号密码是否正确", "系统账号登录异常", 2, "账号登录失败", "请检查账号密码是否正确"),
    DATABASE_ERROR("5511", "数据库连接异常", "数据库连接异常", "请检查数据库账号密码是否正确", 2, "数据库连接失败", "请检查数据库账号密码是否正确"),
    SUPER_ADMINISTRATOR_ACCOUNT_LOCKOUT("5520", "超级管理员账号锁定", "超级管理员账号{{id_card}}被锁定", "超级管理员账号被锁定", 3, "超级管理员账号密码错误次数过多", "等待锁定超时或联系开发支持解锁"),
    ADMINISTRATOR_ACCOUNT_LOCKOUT("5530", "管理员账号锁定", "管理员账号被锁定", "管理员账号被锁定", 4, "管理员账号密码错误次数过多", "等待锁定超时或超级管理员协助解除锁定"),
    IM_SERVER_IP_ADDRESS_IS_NOT_CONFIGURED("5601", "警信服务器对接IP信息未配置", "警信服务器对接IP信息未设置", "警信服务器对接IP信息未设置", 1, "未检测到警信服务器的服务器IP配置", "在警务协同管理后台设置警信服务器IP"),
    IM_SERVER_PORT_IS_NOT_CONFIGURED("5602", "警信服务器对接端口信息未配置", "警信服务器对接端口信息未设置", "警信服务器对接端口信息未设置", 1, "未检测到警信服务器的服务器端口配置", "在警务协同管理后台设置警信服务器端口"),
    IM_SERVER_ACCOUNT_AND_PASSWORD_IS_NOT_CONFIGURED("5603", "警信服务器对接账号密码信息未配置", "警信服务器对接账号密码信息未设置", "警信服务器对接账号密码信息未设置", 1, "未检测到警信服务器的服务器账号密码配置", "在警务协同管理后台设置警信服务器账号密码"),
    INVOKING_THE_IM_TO_CREATE_A_GROUP_FAILED("5700", "调用IM建群失败", "{{user_name}}用户建群失败，查询内容为：{{content}}；响应为：失败。", "用户调用IM建群失败", 2, "调用IM失败", "检查IM服务状态"),
    INVOKING_THE_IM_TO_QUERY_CHAT_RECORDS_FAILED("5701", "调用IM查询聊天记录失败", "{{user_name}}用户查询聊天记录数据失败，查询内容为：{{content}}；响应为：失败。", "用户调用IM查询聊天记录失败", 2, "调用IM失败", "检查IM服务状态"),
    INVOKING_THE_IM_TO_QUERY_USER_INFO_FAILED("5702", "调用IM查询用户信息失败", "{{user_name}}用户查询IM信息失败，查询内容为：{{content}}；响应为：失败。", "用户调用IM查询用户信息失败", 2, "调用IM失败", "检查IM服务状态"),
    INVOKING_THE_IM_TO_SEND_MSG_FAILED("5703", "调用IM发送消息失败", "{{user_name}}用户发送IM信息失败，发送内容为：{{content}}；响应为：失败。", "用户调用IM发送消息失败", 2, "调用IM失败", "检查IM服务状态"),
    INVOKING_THE_AI_AGENT_FAILED("5760", "调用AI智能体失败", "{{user_name}}用户调用AI智能体失败，调用内容为：{{content}}；响应为：失败。", "用户调用AI智能体失败", 2, "AI调用失败", "检查AI服务状态"),
    INVOKING_PERSONNEL_CHECK_FAILED("5761", "调用人员核查失败", "{{user_name}}用户调用人员核查失败，调用内容为：{{content}}；响应为：失败。", "用户调用人员核查失败", 2, "人员核查调用失败", "检查人员核查系统服务状态"),
    INVOKING_THE_ALARM_TICKET_SYSTEM_FAILED("5762", "调用警单系统失败", "{{user_name}}用户调用警单系统失败，调用内容为：{{content}}；响应为：失败。", "用户调用警单系统失败", 2, "警单数据调用失败", "检查警单系统服务状态");


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
