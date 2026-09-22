package com.tdtech.cloudcmd.im.jingxin.server.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tdtech.cloudcmd.im.jingxin.server.excel.DutyScheduleExcel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@Accessors(chain = true)
@TableName("tb_duty_schedule")
public class DutySchedule implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /**
     * 人员ID
     */
    private Long userId;

    /**
     * 用户名称
     */
    private String userName;

    private Long departmentId;

    private String departmentName;

    /**
     * 值班开始日期
     */
    private LocalDate dutyStartDate;

    /**
     * 值班开始时间
     */
    private LocalTime dutyStartTime;

    /**
     * 值班结束日期
     */
    private LocalDate dutyEndDate;
    /**
     * 值班结束时间
     */
    private LocalTime dutyEndTime;

    /**
     * 排班类型标识，对应 tb_duty_type.type。
     */
    private Long dutyType;

    /**
     * 值班内容/职责
     */
    private String dutyContent;

    /**
     * 导入批次号
     */
    private Long importBatch;

    /**
     * 创建时间
     */
    private Date gmtCreated;

    /**
     * 最后修改时间
     */
    private Date gmtModified;


    /**
     * 导入人员ID
     */
    private Long importUserId;

    /**
     * 导入人员姓名
     */
    private String importUserName;

    public DutySchedule(DutyScheduleExcel excel) {
        super();
        Date now = new Date();
        this.userId = Long.parseLong(excel.getUserId());
        this.userName = excel.getUserName();
        this.dutyStartDate = LocalDate.parse(excel.getDutyStartDate());
        this.dutyStartTime = LocalTime.parse(excel.getDutyStartTime());
        this.dutyEndDate = LocalDate.parse(excel.getDutyEndDate());
        this.dutyEndTime = LocalTime.parse(excel.getDutyEndTime());
        this.dutyType = Long.valueOf(excel.getDutyType());
        this.dutyContent = excel.getDutyContent();
        this.gmtCreated = now;
        this.gmtModified = now;
    }
}
