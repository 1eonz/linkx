package com.chinasoft.cloud.module.aiagent.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "全局配置")
public class GlobalVO {

    @Schema(description = "配置ID")
    private Long id;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "值")
    private String value;
    /**
     * 备注
     */
    @Schema(description = "备注")
    private String remark;
    /**
     * 状态 0-可用 1-禁用
     */
    @Schema(description = "状态 0-可用 1-禁用")
    private Integer status;
}
