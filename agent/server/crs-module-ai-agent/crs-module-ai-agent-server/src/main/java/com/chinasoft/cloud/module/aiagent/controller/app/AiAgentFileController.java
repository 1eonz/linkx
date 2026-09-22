package com.chinasoft.cloud.module.aiagent.controller.app;

import com.chinasoft.cloud.framework.common.pojo.CommonResult;
import com.chinasoft.cloud.module.aiagent.controller.app.vo.AgentCapabilityVO;
import com.chinasoft.cloud.module.aiagent.controller.app.vo.AiFileInfoVO;
import com.chinasoft.cloud.module.aiagent.dal.dataobj.AgentConfig;
import com.chinasoft.cloud.module.aiagent.service.AiAgentConfigService;
import com.chinasoft.cloud.module.aiagent.service.AiAgentFileService;
import com.chinasoft.cloud.module.aiagent.service.AiFileInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;

/**
 * AI智能体文件上传控制器
 */
@Slf4j
@Tag(name = "AI智能体文件上传")
@RestController
@RequestMapping("/proxy/ai/v1/deepseek-zjk/xa/dk/file")
public class AiAgentFileController {

    @Resource
    private AiAgentFileService aiAgentFileService;

    @Resource
    private AiAgentConfigService aiAgentConfigService;

    /**
     * @param file 文件
     * @param agentId 智能体ID
     * @param sessionId 会话ID（前端生成，用于关联后续问答）
     * @param userId 用户标识
     * @return 文件上传结果
     */
    @PermitAll
    @Operation(summary = "上传文件")
    @PostMapping("/upload")
    public CommonResult<AiFileInfoVO> uploadFile(
        @Parameter(description = "文件") @RequestParam("file") MultipartFile file,
        @Parameter(description = "智能体ID") @RequestParam("agentId") Long agentId,
        @Parameter(description = "会话ID") @RequestParam("sessionId") String sessionId,
        @Parameter(description = "用户标识") @RequestParam(value = "userId", required = false) String userId) {

        log.info("Uploading file: agentId={}, sessionId={}, fileName={}, size={}",
            agentId, sessionId, file.getOriginalFilename(), file.getSize());

        AiFileInfo fileInfo = aiAgentFileService.uploadFile(file, agentId, sessionId, userId);
        return CommonResult.success(convertToVO(fileInfo));
    }

    @PermitAll
    @Operation(summary = "查询智能体多模态能力")
    @GetMapping("/capabilities/{agentId}")
    public CommonResult<AgentCapabilityVO> getCapabilities(
        @Parameter(description = "智能体ID") @PathVariable("agentId") Long agentId) {

        AgentConfig config = aiAgentConfigService.getById(agentId);
        if (config == null) {
            return CommonResult.success(null);
        }

        AgentCapabilityVO vo = new AgentCapabilityVO();
        vo.setAudio(config.getAudio() != null && config.getAudio() == 1);
        vo.setAudioType(CollectionUtils.isEmpty(config.getAudioTypeList()) ? Collections.emptyList() : config.getAudioTypeList());
        vo.setVideo(config.getVideo() != null && config.getVideo() == 1);
        vo.setVideoType(CollectionUtils.isEmpty(config.getVideoTypeList()) ? Collections.emptyList() : config.getVideoTypeList());
        vo.setImage(config.getImage() != null && config.getImage() == 1);
        vo.setImageType(CollectionUtils.isEmpty(config.getImageTypeList()) ? Collections.emptyList() : config.getImageTypeList());
        vo.setDocument(config.getDocument() != null && config.getDocument() == 1);
        vo.setDocumentType(CollectionUtils.isEmpty(config.getDocumentTypeList()) ? Collections.emptyList() : config.getDocumentTypeList());
        vo.setFileInterfaceId(config.getFileInterfaceId());
        vo.setHasFileInterface(config.getFileInterfaceId() != null);

        return CommonResult.success(vo);
    }

    /**
     *
     * @param sessionId 会话ID
     * @return 文件信息
     */
    @PermitAll
    @Operation(summary = "获取文件信息")
    @GetMapping("/info")
    public CommonResult<AiFileInfoVO> getFileInfo(
        @Parameter(description = "会话ID") @RequestParam("sessionId") String sessionId) {

        AiFileInfo fileInfo = aiAgentFileService.getFileInfo(sessionId);
        if (fileInfo == null) {
            return CommonResult.success(null);
        }

        return CommonResult.success(convertToVO(fileInfo));
    }

    @PermitAll
    @Operation(summary = "删除文件信息")
    @PostMapping("/delete")
    public CommonResult<Boolean> deleteFileInfo(
        @Parameter(description = "会话ID") @RequestParam("sessionId") String sessionId) {

        aiAgentFileService.deleteFileInfo(sessionId);
        return CommonResult.success(true);
    }

    private AiFileInfoVO convertToVO(AiFileInfo fileInfo) {
        AiFileInfoVO vo = new AiFileInfoVO();
        vo.setSessionId(fileInfo.getSessionId());
        vo.setFileId(fileInfo.getFileId());
        vo.setFileName(fileInfo.getFileName());
        vo.setFilePath(fileInfo.getFilePath());
        vo.setFileUrl(fileInfo.getFileUrl());
        vo.setFileCategory(fileInfo.getFileCategory());
        vo.setAgentId(fileInfo.getAgentId());
        vo.setMimeType(fileInfo.getMimeType());
        vo.setFileSize(fileInfo.getFileSize());
        return vo;
    }
}
