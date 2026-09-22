package com.tdtech.cloudcmd.msip.entity;

import com.tdtech.cloudcmd.msip.enums.AlarmTemplate;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class AlarmTemplateRequest {

    /**
     * 告警定义ID
     */
    private String alarmId;

    /**
     * 语言，这个版本先不搞英文的
     */
    private String language = "zh";

    /**
     * 告警名称
     */
    private String alertName;

    /**
     * 模板
     */
    private String detail;

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

    public AlarmTemplateRequest() {
        super();
    }

    public AlarmTemplateRequest(AlarmTemplate alarmTemplate) {
        this();
        this.setAlarmId(alarmTemplate.getAlarmId());
        this.setAlertName(alarmTemplate.getAlertName());
        this.setDetail(alarmTemplate.getTemplate());
        this.setDescription(alarmTemplate.getDescription());
        this.setLevel(alarmTemplate.getLevel());
        this.setReason(alarmTemplate.getReason());
        this.setRepairSuggestion(alarmTemplate.getRepairSuggestion());
    }

    public AlarmTemplateRequest(AlarmTemplate alarmTemplate, String language) {
        this(alarmTemplate);
        this.setLanguage(language);
    }

}
