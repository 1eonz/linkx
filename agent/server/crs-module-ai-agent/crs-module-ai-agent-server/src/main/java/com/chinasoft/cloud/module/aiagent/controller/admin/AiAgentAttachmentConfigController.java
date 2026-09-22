package com.chinasoft.cloud.module.aiagent.controller.admin;

import com.alibaba.excel.util.StringUtils;
import com.chinasoft.cloud.framework.common.pojo.CommonResult;
import com.chinasoft.cloud.framework.security.core.util.SecurityFrameworkUtils;
import com.chinasoft.cloud.module.aiagent.controller.admin.co.UpsertAttachmentConfigCO;
import com.chinasoft.cloud.module.aiagent.controller.admin.vo.AttachmentConfigVO;
import com.chinasoft.cloud.module.aiagent.dal.dataobj.AgentAttachmentConfig;
import com.chinasoft.cloud.module.aiagent.service.AgentAttachmentConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Tag(name = "管理后台 - AI智能体文件上传接口配置")
@RestController
@RequestMapping("/proxy/ai/v1/aiagent/attachment-config")
public class AiAgentAttachmentConfigController {

    @Resource
    private AgentAttachmentConfigService attachmentConfigService;

    @PermitAll
    @PostMapping("/create")
    @Operation(summary = "创建文件上传接口配置")
    public CommonResult<Long> create(@Valid @RequestBody UpsertAttachmentConfigCO co) {
        AgentAttachmentConfig config = convertToEntity(co);
        String userName = SecurityFrameworkUtils.getLoginUserNickname();
        if (userName != null) {
            config.setCreator(userName);
            config.setCreateTime(LocalDateTime.now());
        }
        Long id = attachmentConfigService.create(config);
        log.info("Created attachment config: id={}, name={}", id, co.getName());
        return CommonResult.success(id);
    }

    @PermitAll
    @PutMapping("/update")
    @Operation(summary = "更新文件上传接口配置")
    public CommonResult<Boolean> update(@Valid @RequestBody UpsertAttachmentConfigCO co) {
        if (co.getId() == null) {
            return CommonResult.error(Integer.valueOf(400), "配置ID不能为空");
        }
        AgentAttachmentConfig config = convertToEntity(co);
        String userName = SecurityFrameworkUtils.getLoginUserNickname();
        if (userName != null) {
            config.setUpdater(userName);
            config.setUpdateTime(LocalDateTime.now());
        }
        attachmentConfigService.updateById(config);
        log.info("Updated attachment config: id={}", co.getId());
        return CommonResult.success(true);
    }

    @PermitAll
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除文件上传接口配置")
    public CommonResult<Boolean> delete(@Parameter(description = "配置ID") @PathVariable("id") Long id) {
        attachmentConfigService.deleteById(id);
        log.info("Deleted attachment config: id={}", id);
        return CommonResult.success(true);
    }

    @PermitAll
    @GetMapping("/get/{id}")
    @Operation(summary = "获取文件上传接口配置详情")
    public CommonResult<AttachmentConfigVO> get(@Parameter(description = "配置ID") @PathVariable("id") Long id) {
        AgentAttachmentConfig config = attachmentConfigService.getById(id);
        if (config == null) {
            return CommonResult.success(null);
        }
        return CommonResult.success(convertToVO(config));
    }

    @PermitAll
    @GetMapping("/list")
    @Operation(summary = "获取文件上传接口配置列表")
    public CommonResult<List<AttachmentConfigVO>> list() {
        List<AgentAttachmentConfig> configs = attachmentConfigService.listAll();
        List<AttachmentConfigVO> voList = configs.stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
        return CommonResult.success(voList);
    }

    private AgentAttachmentConfig convertToEntity(UpsertAttachmentConfigCO co) {
        AgentAttachmentConfig config = new AgentAttachmentConfig();
        config.setId(co.getId());
        config.setName(co.getName());
        config.setMethod(co.getMethod());
        config.setIp(co.getIp());
        config.setPort(co.getPort());
        config.setUri(co.getUri());
        config.setHeader(StringUtils.isBlank(co.getHeader()) ? null : co.getHeader());
        config.setQuery(StringUtils.isBlank(co.getQuery()) ? null : co.getQuery());
        config.setBody(StringUtils.isBlank(co.getBody()) ? null : co.getBody());
        config.setReponseFileFiled(co.getReponseFileFiled());
        config.setDesc(co.getDesc());
        return config;
    }

    private AttachmentConfigVO convertToVO(AgentAttachmentConfig config) {
        AttachmentConfigVO vo = new AttachmentConfigVO();
        vo.setId(config.getId());
        vo.setName(config.getName());
        vo.setMethod(config.getMethod());
        vo.setIp(config.getIp());
        vo.setPort(config.getPort());
        vo.setUri(config.getUri());
        vo.setHeader(config.getHeader());
        vo.setQuery(config.getQuery());
        vo.setBody(config.getBody());
        vo.setReponseFileFiled(config.getReponseFileFiled());
        vo.setDesc(config.getDesc());
        vo.setCreator(config.getCreator());
        vo.setCreateTime(config.getCreateTime());
        vo.setUpdater(config.getUpdater());
        vo.setUpdateTime(config.getUpdateTime());
        return vo;
    }
}
