package com.tdtech.cloudcmd.linkx.dashboard.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class StaticPhotoCheckExportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ExcelProperty("数据库表ID")
    private String id;

    @ExcelProperty("核查发起人ID")
    private String checkUserId;

    @ExcelProperty("核查发起人姓名")
    private String checkUserName;

    @ExcelProperty("核查发起人部门ID")
    private String checkUserDepartmentId;

    @ExcelProperty("核查发起人部门名称")
    private String checkUserDepartmentName;

    @ExcelProperty("发起核查的协同岗ID")
    private String coopUserId;

    @ExcelProperty("发起核查的协同岗名称")
    private String coopUserName;

    @ExcelProperty("核查发起的数据明细ID")
    private String checkDataId;

    @ExcelProperty("核查发起时间")
    private String gmtCreateTime;
}