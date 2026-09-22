package com.chinasoft.cloud.module.aiagent.msip.entity;

import com.chinasoft.cloud.module.aiagent.msip.enums.AlertDefineEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * DefineActiveRecord类（告警定义 + 活动告警二合一的类）
 */
@Data
@ToString
@Slf4j
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DefineActiveRecord {
    /**
     * 告警定义ID，由数字组成的字符串，MSIP平台分配LinkX的告警ID范围：[5000-5999]；告警等级1-5(数字越小越严重)
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
     * 范围：1 ~ 5，数字越小越严重
     * 1（critical）、2（major）、3（minor）、4（warning）、5（indeterminate）
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

    /**
     * 告警详情
     */
    private String detail;

    /**
     * 告警来源对象类标识
     */
    private String oriSysTag = "LinkX";

    /**
     * 告警来源IP
     */
    private String ip;

    /**
     * 定位信息
     */
    private String location;

    public DefineActiveRecord() {
        super();
    }

    public DefineActiveRecord(AlertDefineEnum alertDefineEnum, String errorInfo) {
        super();
        this.setAlertDefineId(alertDefineEnum.getAlertDefineId());
        this.setAlertName(alertDefineEnum.getAlertName());
        this.setDescription(alertDefineEnum.getDescription());
        this.setLevel(alertDefineEnum.getLevel());
        this.setReason(alertDefineEnum.getReason());
        this.setRepairSug(alertDefineEnum.getRepairSug());
        this.setDetail(errorInfo);
        this.setIp(System.getenv("SERVER_IP"));
    }

    public DefineActiveRecord(AlertDefineEnum alertDefineEnum, String errorInfo, String... param) {
        this(alertDefineEnum, errorInfo);
        String detailTemplate = alertDefineEnum.getDetailTemplate();
        boolean isNeedFormat = Objects.nonNull(param) && param.length > 0 && detailTemplate.contains("%s");
        if (isNeedFormat) {
            try {
                String formatValue = String.format(detailTemplate, param);
                this.setDetail(formatValue);
            } catch (Exception e) {
                log.error("DefineActiveRecord format description error: {}", e.getMessage());
            }
        }
    }


}
