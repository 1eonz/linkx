package com.chinasoft.cloud.module.aiagent.service;

import com.chinasoft.cloud.module.aiagent.dal.dataobj.AgentAttachmentConfig;
import com.chinasoft.cloud.module.aiagent.excel.bo.ResultBO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * AI智能体文件上传接口配置服务接口
 */
public interface AgentAttachmentConfigService {

    AgentAttachmentConfig getById(Long id);

    List<AgentAttachmentConfig> listAll();

    Long create(AgentAttachmentConfig config);

    void updateById(AgentAttachmentConfig config);

    void deleteById(Long id);

    ResultBO<AgentAttachmentConfig> importFromExcel(MultipartFile file) throws IOException;
}
