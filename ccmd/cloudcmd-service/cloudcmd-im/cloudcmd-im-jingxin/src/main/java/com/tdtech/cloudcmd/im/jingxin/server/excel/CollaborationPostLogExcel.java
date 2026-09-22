package com.tdtech.cloudcmd.im.jingxin.server.excel;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author lsc
 * @date 2025/7/14
 **/
@Data
public class CollaborationPostLogExcel implements Serializable {
    /**
     * 协同岗名称
     */
    @ExcelProperty("协同岗名称")
    private String postName;

    /**
     * 所属组织名称
     */
    @ExcelProperty("所属组织")
    private String orgName;

    /**
     * 关联人员名称
     */
    @ExcelProperty("关联人员")
    private String relatedUserNames;

    /**
     * 操作人姓名
     */
    @ExcelProperty("操作人员")
    private String operatorName;

    /**
     * 操作类型 1为创建，2为修改
     */
    @ExcelIgnore
    private Integer operationType;

    /**
     * 操作类型 1为创建，2为修改
     */
    @ExcelProperty("操作类型")
    private String operationTypeName;

    /**
     * 操作内容
     */
    @ExcelProperty("操作内容")
    private String content;

    /**
     * 操作时间
     */
    @ExcelProperty("操作时间")
    private Date operateTime;

}