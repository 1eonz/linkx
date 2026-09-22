package com.chinasoft.cloud.module.aiagent.controller.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 文件上传结果响应
 */
@Schema(description = "文件上传结果")
@Data
public class AiFileInfoVO {

    @Schema(description = "会话ID")
    private String sessionId;

    @Schema(description = "AI服务器文件标识")
    private String fileId;

    @Schema(description = "原始文件名")
    private String fileName;

    @Schema(description = "本地存储路径")
    private String filePath;

    @Schema(description = "文件访问URL")
    private String fileUrl;

    @Schema(description = "文件类别: audio/video/image/document")
    private String fileCategory;

    @Schema(description = "智能体ID")
    private Long agentId;

    @Schema(description = "MIME类型")
    private String mimeType;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;
}
