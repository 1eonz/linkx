package com.tdtech.cloudcmd.admin.resource.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
@Schema(description = "AI分离部署配置")
public class AiDeployConfigVO {

    @NotNull
    @Min(0)
    @Max(1)
    @Schema(description = "是否分离部署，0-否，1-是", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer separatedDeploy;

    @Schema(description = "AI分离部署地址", example = "http://10.28.64.80:30013/")
    private String groupAiHost;

    @Schema(description = "AI分离部署前端页面地址", example = "http://10.28.64.80:3100/agent/admin/")
    private String groupAiFrontendHost;
}
