package com.chinasoft.cloud.module.aiagent.controller.admin.co;

import com.mzt.logapi.starter.annotation.DiffLogField;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "智能体配置")
@Data
public class UpsertAgentConfigCO {

    @Schema(description = "名称")
    @NotBlank
    @DiffLogField(name = "名称")
    private String name;

    @Schema(description = "请求方式")
    @DiffLogField(name = "请求方式")
    private String httpMethod;

    @Schema(description = "智能体地址")
    @DiffLogField(name = "智能体地址")
    @NotBlank
    private String url;

    @Schema(description = "认证token")
    @DiffLogField(name = "认证token")
    @NotBlank
    private String token;

    @Schema(description = "请求头(JSON)")
    @DiffLogField(name = "请求头")
    private String header;

    @Schema(description = "Query参数(JSON)")
    @DiffLogField(name = "Query参数")
    private String query;

    @Schema(description = "Body参数(JSON)")
    @DiffLogField(name = "Body参数")
    private String body;

    @Schema(description = "请求体类型: 1-raw-json(默认)/2-raw-text/3-form-data")
    private Integer bodyType;

    @Schema(description = "是否有结束标识: 1-有(默认)/0-无")
    private Integer endFlag;

    @Schema(description = "优先级")
    @DiffLogField(name = "优先级")
    @NotNull
    private Integer priority;

    @Schema(description = "图标地址")
    @DiffLogField(name = "图标地址")
    @NotBlank
    private String avatar;

    @Schema(description = "描述")
    @DiffLogField(name = "描述")
    private String desc;

    @Schema(description = "分类")
    @DiffLogField(name = "分类")
    @NotBlank
    private String categoryIds;

    // 是否涉密 0 否 1 是
    private Integer isRestricted = 0;

    // 是否接收IM消息。0：不接收；1：接收
    private Integer receiveIm = 0;

    // 智能体的作用域。0：所有；1：仅作用于AI智能体问答；2：仅作用于IM
    private Integer scope = 1;

    private String paramScript;

    private String respScript;

    private Long virtualUserId;

    private Long createdUserId;


    // ========== 多模态能力配置 =======
    @Schema(description = "是否支持音频 0否 1是")
    private Integer audio;

    @Schema(description = "音频类型列表")
    private List<String> audioType;

    @Schema(description = "是否支持视频 0否 1是")
    private Integer video;

    @Schema(description = "视频类型列表")
    private List<String> videoType;

    @Schema(description = "是否支持图片 0否 1是")
    private Integer image;

    @Schema(description = "图片类型列表")
    private List<String> imageType;

    @Schema(description = "是否支持文档 0否 1是")
    private Integer document;

    @Schema(description = "文档类型列表")
    private List<String> documentType;

    @Schema(description = "文件上传接口配置ID")
    private Long fileInterfaceId;
}