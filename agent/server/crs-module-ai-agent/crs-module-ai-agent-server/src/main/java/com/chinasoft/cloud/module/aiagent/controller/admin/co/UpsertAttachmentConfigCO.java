package com.chinasoft.cloud.module.aiagent.controller.admin.co;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建/更新文件上传接口配置请求
 */
@Schema(description = "创建/更新文件上传接口配置")
@Data
public class UpsertAttachmentConfigCO {

    @Schema(description = "配置ID（更新时必填）")
    private Long id;

    @Schema(description = "名称")
    @NotBlank(message = "名称不能为空")
    private String name;

    @Schema(description = "请求方式")
    private String method;

    @Schema(description = "服务器IP地址")
    private String ip;

    @Schema(description = "服务器端口")
    private Integer port;

    @Schema(description = "服务URI路径")
    private String uri;

    @Schema(description = "header参数（JSON格式）")
    private String header;

    @Schema(description = "query参数（JSON格式）")
    private String query;

    @Schema(description = "body参数（JSON格式）")
    private String body;

    @Schema(description = "文件标识字段（响应中提取文件ID的字段名）")
    private String reponseFileFiled;

    @Schema(description = "描述")
    private String desc;
}
