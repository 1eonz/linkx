package com.tdtech.cloudcmd.msip.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.ToString;

/**
 * ActiveRecord类（活动告警类）
 */
@Data
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ActiveRecord {
    /**
     * 告警定义ID，由数字组成的字符串，MSIP平台分配LinkX的告警ID范围：[5000-5999]；告警等级1-5(数字越小越严重)
     */
    private String alertDefineId;

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

    public ActiveRecord() {
        super();
    }

    public ActiveRecord(DefineActiveRecord defineActiveRecord) {
        super();
        this.setAlertDefineId(defineActiveRecord.getAlertDefineId());
        this.setDetail(defineActiveRecord.getDetail());
        this.setOriSysTag(defineActiveRecord.getOriSysTag());
        this.setIp(System.getenv("SERVER_IP"));
    }

}
