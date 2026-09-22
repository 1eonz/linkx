package com.tdtech.cloudcmd.linkx.dashboard.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class StaticCoopDutySwitchExportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ExcelProperty("数据库表ID")
    private String id;

    @ExcelProperty("切换发起人ID")
    private String userId;

    @ExcelProperty("切换发起人姓名")
    private String userName;

    @ExcelProperty("切换发起人部门ID")
    private String userDepartmentId;

    @ExcelProperty("切换发起人部门名称")
    private String userDepartmentName;

    @ExcelProperty("切换发起的协同岗ID")
    private String coopUserId;

    @ExcelProperty("切换发起的协同岗名称")
    private String coopUserName;

    @ExcelProperty("切换类型")
    private String swithType;

    @ExcelProperty("切换方式")
    private String switchFlag;

    @ExcelProperty("切换时间")
    private String gmtCreateTime;
}