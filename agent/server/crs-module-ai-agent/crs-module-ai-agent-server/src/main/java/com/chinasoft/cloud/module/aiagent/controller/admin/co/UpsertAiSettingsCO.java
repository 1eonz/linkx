package com.chinasoft.cloud.module.aiagent.controller.admin.co;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "AI系统设置")
@Data
public class UpsertAiSettingsCO {

    @Schema(description = "是否开启审批模式 0-否 1-是")
    private Integer approvalEnabled;

    @Schema(description = "审批子模式 0-先问后审 1-先审后答")
    private String approvalSubMode;

    @Schema(description = "审批系统地址")
    private String approvalSystemUrl;
}
