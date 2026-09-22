package com.tdtech.cloudcmd.im.jingxin.server.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class DutyScheduleVO extends DutySchedule {

    /**
     * 用户支持的协同岗名称
     */
    private String postName;

    /**
     * 用户头像
     */
    private String avatar;

    /**
     * 部门全路径
     */
    private String fullPathName;

    /**
     * 排班类型名称。
     */
    private String dutyTypeName;
}
