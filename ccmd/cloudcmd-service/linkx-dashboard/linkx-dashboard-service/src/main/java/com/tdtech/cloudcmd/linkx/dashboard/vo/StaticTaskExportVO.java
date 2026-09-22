package com.tdtech.cloudcmd.linkx.dashboard.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class StaticTaskExportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ExcelProperty("数据库表ID")
    private String id;

    @ExcelProperty("任务ID")
    private String taskId;

    @ExcelProperty("任务发起人ID")
    private String fromUserId;

    @ExcelProperty("任务发起人姓名")
    private String fromUserName;

    @ExcelProperty("任务发起人部门ID")
    private String fromUserDepartmentId;

    @ExcelProperty("任务发起人部门名称")
    private String fromUserDepartmentName;

    @ExcelProperty("任务状态")
    private String status;

    @ExcelProperty("任务关联的警信消息发送时间")
    private String msgSentTime;

    @ExcelProperty("任务关联的警信消息ID")
    private String msgSeqid;

    @ExcelProperty("任务分配的协同岗ID")
    private String postId;

    @ExcelProperty("任务分配的协同岗名称")
    private String postName;

    @ExcelProperty("任务创建时间")
    private String createTime;

    @ExcelProperty("任务逾期时间")
    private String expiredTime;

    @ExcelProperty("任务首次回复时间")
    private String responseTime;

    @ExcelProperty("任务首次回复警信用户ID")
    private String responseUserId;

    @ExcelProperty("任务首次回复警信用户姓名")
    private String responseUserName;

    @ExcelProperty("任务忽略时间")
    private String ignoreTime;

    @ExcelProperty("任务忽略操作人ID")
    private String ignoreUserId;

    @ExcelProperty("任务忽略操作人姓名")
    private String ignoreUserName;

    @ExcelProperty("任务跟踪时间")
    private String trackTime;

    @ExcelProperty("任务跟踪操作人ID")
    private String trackUserId;

    @ExcelProperty("任务跟踪操作人姓名")
    private String trackUserName;

    @ExcelProperty("任务完成时间")
    private String finishTime;

    @ExcelProperty("任务完成操作人ID")
    private String finishUserId;

    @ExcelProperty("任务完成操作人姓名")
    private String finishUserName;
}