package com.chinasoft.cloud.module.aiagent.controller.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 智能体能力响应
 */
@Schema(description = "智能体多模态能力")
@Data
public class AgentCapabilityVO {

    @Schema(description = "音频能力")
    private Boolean audio;

    @Schema(description = "支持的音频格式")
    private List<String> audioType;

    @Schema(description = "视频能力")
    private Boolean video;

    @Schema(description = "支持的视频格式")
    private List<String> videoType;

    @Schema(description = "图片能力")
    private Boolean image;

    @Schema(description = "支持的图片格式")
    private List<String> imageType;

    @Schema(description = "文档能力")
    private Boolean document;

    @Schema(description = "支持的文档格式")
    private List<String> documentType;

    @Schema(description = "文件上传接口配置ID")
    private Long fileInterfaceId;

    @Schema(description = "是否有文件上传接口")
    private Boolean hasFileInterface;
}
