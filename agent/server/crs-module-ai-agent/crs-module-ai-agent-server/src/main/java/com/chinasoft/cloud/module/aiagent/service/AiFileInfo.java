package com.chinasoft.cloud.module.aiagent.service;

import lombok.Data;

import java.io.Serializable;

/**
 * AI文件信息缓存对象
 * 用于在Redis中存储文件上传信息，关联会话ID
 */
@Data
public class AiFileInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * AI服务器返回的文件标识
     */
    private String fileId;

    /**
     * 本地存储路径
     */
    private String filePath;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 文件访问URL
     */
    private String fileUrl;

    /**
     * 文件类别: audio/video/image/document
     */
    private String fileCategory;

    /**
     * 智能体ID
     */
    private Long agentId;

    /**
     * 用户标识
     */
    private String userId;

    /**
     * MIME类型
     */
    private String mimeType;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;
}
