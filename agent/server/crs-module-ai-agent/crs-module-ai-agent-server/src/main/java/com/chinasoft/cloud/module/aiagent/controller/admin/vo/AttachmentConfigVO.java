package com.chinasoft.cloud.module.aiagent.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件上传接口配置响应
 */
@Schema(description = "文件上传接口配置")
@Data
public class AttachmentConfigVO {

    @Schema(description = "配置ID")
    private Long id;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "请求方式")
    private String method;

    @Schema(description = "服务器IP地址")
    private String ip;

    @Schema(description = "服务器端口")
    private Integer port;

    @Schema(description = "服务URI路径")
    private String uri;

    @Schema(description = "header参数")
    private String header;

    @Schema(description = "query参数")
    private String query;

    @Schema(description = "body参数")
    private String body;

    @Schema(description = "文件标识字段")
    private String reponseFileFiled;

    @Schema(description = "描述")
    private String desc;

    @Schema(description = "创建人")
    private String creator;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新人")
    private String updater;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
