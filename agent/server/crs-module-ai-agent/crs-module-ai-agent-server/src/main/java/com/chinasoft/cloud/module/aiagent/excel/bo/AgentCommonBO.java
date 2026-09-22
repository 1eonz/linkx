package com.chinasoft.cloud.module.aiagent.excel.bo;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = false)
public class AgentCommonBO {
    /**
     * Header参数
     */
    @ExcelProperty("Header参数")
    private String header;

    /**
     * Query参数
     */
    @ExcelProperty("Query参数")
    private String query;

    /**
     * Body参数
     */
    @ExcelProperty("Body参数")
    private String body;

    @ExcelProperty(value = "Body参数类型")
    private String bodyTypeStr;

    @ExcelProperty(value = "是否有结束标识")
    private String endFlagStr;

    /**
     * 当前行数据在excel文件中的行数
     */
    @ExcelIgnore
    private Integer rowIndex;
}
