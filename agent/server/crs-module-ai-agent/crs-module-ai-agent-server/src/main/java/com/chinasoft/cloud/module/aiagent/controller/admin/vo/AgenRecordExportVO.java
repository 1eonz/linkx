package com.chinasoft.cloud.module.aiagent.controller.admin.vo;

import java.time.LocalDateTime;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Agent导出VO")
@Data
@ExcelIgnoreUnannotated
public class AgenRecordExportVO {

    private Long id;
    @Schema(description = "查询人")
    @ExcelProperty("查询人")
    private String userName;

    @Schema(description = "查询人身份证号")
    @ExcelProperty("查询人身份证号")
    private String identityCardNumber;

    @Schema(description = "查询内容")
    @ExcelProperty("查询内容")
    private String queryContent;

    @Schema(description = "响应内容")
    @ExcelProperty("响应内容")
    private String responseContent;

    @Schema(description = "时间")
    @ExcelProperty("时间")
    private LocalDateTime time;

    @Schema(description = "查询智能体")
    @ExcelProperty("查询智能体")
    private String agentName;

    // @Schema(description = "查询智能体编号")
    // @ExcelProperty("查询智能体编号")
    // private Long agentConfigId;

}
