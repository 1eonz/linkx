package com.tdtech.cloudcmd.msip.entity;

import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * DefineActiveRecord类（告警定义 + 活动告警二合一的类）
 */
@Data
@ToString
@Slf4j
public class ActiveAlarmsClearRequest {
    private String alarmId;
    private String location = "LinkX";     //这里alarmId和location定义为唯一索引

    public ActiveAlarmsClearRequest() {
        super();
    }

    public ActiveAlarmsClearRequest(String alarmId) {
        this();
        this.setAlarmId(alarmId);
    }

}
