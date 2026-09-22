package com.tdtech.cloudcmd.im.jingxin.server.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class PostExcel implements Serializable {
    /**
     * 协同岗ID
     */
    @ExcelProperty("协同岗ID")
    private String id;

    /**
     * 协同岗名称
     */
    @ExcelProperty("协同岗名称")
    private String postName;

    /**
     * 关联人员IDs
     */
    @ExcelProperty("关联人员IDs")
    private String relatedUserIds;

    /**
     * 关联人员名称
     */
    @ExcelProperty("关联人员名称")
    private String relatedUserNames;

}