package com.tdtech.cloudcmd.linkx.dashboard.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class StaticCreateGroupExportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ExcelProperty("数据库表ID")
    private String id;

    @ExcelProperty("创群人ID")
    private String userId;

    @ExcelProperty("创群人姓名")
    private String userName;

    @ExcelProperty("创群人部门ID")
    private String userDepartmentId;

    @ExcelProperty("创群人部门名称")
    private String userDepartmentName;

    @ExcelProperty("群组ID")
    private String groupId;

    @ExcelProperty("群组名称")
    private String groupName;

    @ExcelProperty("群组类型")
    private String groupType;

    @ExcelProperty("建群方式")
    private String groupSubType;

    @ExcelProperty("创群时间")
    private String gmtCreateTime;
}