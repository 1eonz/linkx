package com.tdtech.cloudcmd.im.jingxin.server.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class IMUserExcel implements Serializable {
    /**
     * 部门ID
     */
    @ExcelProperty("部门ID")
    private String deptId;

    /**
     * 组织部门名称
     */
    @ExcelProperty("组织部门名称")
    private String deptName;

    /**
     * 人员ID
     */
    @ExcelProperty("人员ID")
    private String id;

    /**
     * 姓名
     */
    @ExcelProperty("姓名")
    private String name;

    /**
     * 所属协同岗名称
     */
    @ExcelProperty("所属协同岗名称")
    private String postName;
}