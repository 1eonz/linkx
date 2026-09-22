package com.tdtech.linkx.node.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * 开放数据授权DTO
 */
@Data
@Schema(description = "开放数据授权请求")
public class OpenDataGrantDTO {

    @Schema(description = "组织部门数据授权：1=授权，0=不授权")
    @Min(0)
    @Max(1)
    private Integer org;

    @Schema(description = "看板数据授权：1=授权，0=不授权")
    @Min(0)
    @Max(1)
    private Integer dashboard;

    @Schema(description = "协同用户数据授权：1=授权，0=不授权")
    @Min(0)
    @Max(1)
    private Integer coopUser;

    @Schema(description = "H5数据授权：1=授权，0=不授权")
    @Min(0)
    @Max(1)
    private Integer h5;
}
