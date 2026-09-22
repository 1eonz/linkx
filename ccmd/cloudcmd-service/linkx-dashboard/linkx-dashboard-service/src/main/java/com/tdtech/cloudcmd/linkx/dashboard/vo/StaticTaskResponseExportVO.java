package com.tdtech.cloudcmd.linkx.dashboard.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class StaticTaskResponseExportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ExcelProperty("数据库表ID")
    private String id;

    @ExcelProperty("回复的任务ID")
    private String taskId;

    @ExcelProperty("任务回复人ID")
    private String responseUserId;

    @ExcelProperty("任务回复人姓名")
    private String responseUserName;

    @ExcelProperty("任务回复人部门ID")
    private String responseUserDepartmentId;

    @ExcelProperty("任务回复人部门名称")
    private String responseUserDepartmentName;

    @ExcelProperty("回复的任务协同岗ID")
    private String responseCoopUserId;

    @ExcelProperty("回复的任务协同岗名称")
    private String responseCoopUserName;

    @ExcelProperty("任务回复时间")
    private String responseTime;

    @ExcelProperty("任务回复的警信消息ID")
    private String responseMsgSeqid;
}