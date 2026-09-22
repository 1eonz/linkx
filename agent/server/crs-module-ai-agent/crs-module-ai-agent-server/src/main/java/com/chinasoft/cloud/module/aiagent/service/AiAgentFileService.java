package com.chinasoft.cloud.module.aiagent.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * AI智能体文件上传服务接口
 */
public interface AiAgentFileService {

    /**
     * 上传文件
     * 1. 保存文件到本地磁盘
     * 2. 转发文件到AI服务器
     * 3. 将文件信息存入Redis缓存
     */
    AiFileInfo uploadFile(MultipartFile file, Long agentId, String sessionId, String userId);

    /**
     * 从缓存获取文件信息
     * @param sessionId 会话ID
     * @return 文件信息
     */
    AiFileInfo getFileInfo(String sessionId);

    /**
     * 删除缓存中的文件信息
     * @param sessionId 会话ID
     */
    void deleteFileInfo(String sessionId);

    /**
     * 判断文件类别
     * @param fileName 文件名
     * @param agentId 智能体ID
     * @return 文件类别 audio/video/image/document
     */
    String determineFileCategory(String fileName, Long agentId);

    /**
     * 验证文件格式是否支持
     * @param fileName 文件名
     * @param agentId 智能体ID
     * @return 是否支持
     */
    boolean validateFileType(String fileName, Long agentId);
}
