package com.tdtech.cloudcmd.im.jingxin.server.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class DutyScheduleExcel implements Serializable {

    /**
     * 人员ID
     */
    @ExcelProperty("人员ID*")
    private String userId;

    /**
     * 姓名
     */
    @ExcelProperty("姓名*")
    private String userName;

    /**
     * 值班开始日期
     */
    @ExcelProperty("值班开始日期*")
    private String dutyStartDate;

    /**
     * 值班开始时间
     */
    @ExcelProperty("值班开始时间*")
    private String dutyStartTime;

    /**
     * 值班结束日期
     */
    @ExcelProperty("值班结束日期*")
    private String dutyEndDate;

    /**
     * 值班结束时间
     */
    @ExcelProperty("值班结束时间*")
    private String dutyEndTime;

    /**
     * 排班类型名称；兼容历史模板填写排班类型标识。
     */
    @ExcelProperty("排班类型*")
    private String dutyType;

    /**
     * 值班内容/职责
     */
    @ExcelProperty("值班内容/职责")
    private String dutyContent;

    /**
     * 行号
     */
    private Integer rowNum;
}
