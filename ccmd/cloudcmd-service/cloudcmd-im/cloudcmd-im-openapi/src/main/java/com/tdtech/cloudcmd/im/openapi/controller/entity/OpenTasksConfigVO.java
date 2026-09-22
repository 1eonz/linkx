package com.tdtech.cloudcmd.im.openapi.controller.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 任务标准件配置请求体
 */
@Data
public class OpenTasksConfigVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "任务标准件模块名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String module;

    @Schema(description = "是否在PC显示。1：要显示；0：不显示", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer showInPC;
}