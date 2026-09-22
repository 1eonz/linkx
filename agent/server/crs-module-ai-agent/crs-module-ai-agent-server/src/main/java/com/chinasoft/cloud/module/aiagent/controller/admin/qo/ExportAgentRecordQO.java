package com.chinasoft.cloud.module.aiagent.controller.admin.qo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "智能体统计")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExportAgentRecordQO {

    @Schema(description = "用户名称（模糊查询）")
    private String userName;

    @Schema(description = "身份证号(模糊查询)")
    private String identityCardNumber;

    @Schema(description = "输入问题（模糊查询）")
    private String content;

    @Schema(description = "时间范围")
    private String minTime;

    @Schema(description = "时间范围")
    private String maxTime;

    @Schema(description = "id逗号分割")
    private String ids;
}
