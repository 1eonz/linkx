package com.chinasoft.cloud.module.aiagent.service;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chinasoft.cloud.framework.common.exception.ServiceException;
import com.chinasoft.cloud.framework.common.exception.enums.GlobalErrorCodeConstants;
import com.chinasoft.cloud.framework.common.util.object.BeanUtils;
import com.chinasoft.cloud.framework.security.core.util.SecurityFrameworkUtils;
import com.chinasoft.cloud.module.aiagent.dal.dataobj.AgentAttachmentConfig;
import com.chinasoft.cloud.module.aiagent.dal.dataobj.AgentConfig;
import com.chinasoft.cloud.module.aiagent.dal.mysql.AgentAttachmentConfigMapper;
import com.chinasoft.cloud.module.aiagent.dal.mysql.AgentConfigMapper;
import com.chinasoft.cloud.module.aiagent.excel.bo.AgentAttachmentConfigBO;
import com.chinasoft.cloud.module.aiagent.excel.bo.ResultBO;
import com.chinasoft.cloud.module.aiagent.excel.listener.AgentCommonReadListener;
import com.chinasoft.cloud.module.aiagent.excel.verification.AgentAttachmentConfigVerification;
import com.chinasoft.cloud.module.aiagent.excel.verification.AgentCommonVerification;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.chinasoft.cloud.module.aiagent.excel.constant.AttachmentErrorMessage.ERROR_MESSAGE_NAME_NOT_REPEATED;
import static com.chinasoft.cloud.module.aiagent.excel.handler.AgentConfigHandler.AGENT_ATTACHMENT_SHEET_NAME;
import static com.chinasoft.cloud.module.aiagent.excel.handler.AgentConfigHandler.HEAD_ROW_NUMBER;

/**
 * AI智能体文件上传接口配置服务实现
 */
@Slf4j
@Service
public class AgentAttachmentConfigServiceImpl implements AgentAttachmentConfigService {

    @Resource
    private AgentAttachmentConfigMapper attachmentConfigMapper;

    @Autowired
    private AgentAttachmentConfigVerification agentAttachmentConfigVerification;

    @Autowired
    private AgentConfigMapper agentConfigMapper;

    @Override
    public AgentAttachmentConfig getById(Long id) {
        if (id == null) {
            return null;
        }
        return attachmentConfigMapper.selectById(id);
    }

    @Override
    public List<AgentAttachmentConfig> listAll() {
        return attachmentConfigMapper.selectList(Wrappers.lambdaQuery(AgentAttachmentConfig.class)
                .orderByDesc(AgentAttachmentConfig::getCreateTime));
    }

    @Override
    public Long create(AgentAttachmentConfig config) {
        attachmentConfigMapper.insert(config);
        return config.getId();
    }

    @Override
    public void updateById(AgentAttachmentConfig config) {
        attachmentConfigMapper.updateById(config);
    }

    @Override
    public void deleteById(Long id) {
        // 查询被哪些智能体关联（MyBatis-Plus的逻辑删除会自动过滤已删除的记录）
        List<AgentConfig> relatedConfigs = agentConfigMapper.selectList(
                Wrappers.lambdaQuery(AgentConfig.class)
                        .eq(AgentConfig::getFileInterfaceId, id)
                        .select(AgentConfig::getId, AgentConfig::getName)
        );

        if (relatedConfigs != null && !relatedConfigs.isEmpty()) {
            // 构建关联智能体名称列表
            List<String> agentNames = relatedConfigs.stream()
                    .map(AgentConfig::getName)
                    .toList();

            String message = String.format(
                    "该文件上传接口已被 %d 个智能体关联，无法删除。关联的智能体：%s",
                    relatedConfigs.size(),
                    String.join("、", agentNames)
            );

            // 使用ServiceException，错误信息会正确返回给前端
            throw new ServiceException(GlobalErrorCodeConstants.BUSINESS_ERROR.getCode(), message);
        }

        // 未被关联，执行删除
        attachmentConfigMapper.deleteById(id);
    }

    @Override
    public ResultBO<AgentAttachmentConfig> importFromExcel(MultipartFile file) throws IOException {
        log.info("import agent attachment from excel start.");
        String userName = SecurityFrameworkUtils.getLoginUserNickname();
        LocalDateTime now = LocalDateTime.now();
        ResultBO<AgentAttachmentConfig> result = new ResultBO<>();
        AgentCommonReadListener<AgentAttachmentConfigBO> listener = new AgentCommonReadListener<>(
                config -> agentAttachmentConfigVerification.verifyParams(config, result),
                configs -> batchCreate(configs, userName, now, result));
        EasyExcel.read(file.getInputStream(), AgentAttachmentConfigBO.class, listener)
                .headRowNumber(HEAD_ROW_NUMBER)
                .autoTrim(true)
                .sheet(AGENT_ATTACHMENT_SHEET_NAME)
                .doRead();

        result.setEmpty(listener.isEmpty());
        log.info("import agent attachment from excel end.");
        return result;
    }

    private void batchCreate(List<AgentAttachmentConfigBO> configs,
                             String userName,
                             LocalDateTime now,
                             ResultBO<AgentAttachmentConfig> resultBO) {
        if (CollectionUtils.isEmpty(configs)) {
            return;
        }

        Map<String, Long> nameToSizeMap = configs.stream()
                .collect(Collectors.groupingBy(AgentAttachmentConfigBO::getName, Collectors.counting()));
        List<AgentAttachmentConfigBO> filteredConfigs = configs.stream()
                .filter(config -> {
                    boolean result = nameToSizeMap.getOrDefault(config.getName(), 0L) == 1;
                    AgentCommonVerification.assemblyErrorMsg(resultBO, config.getRowIndex(),
                            result, ERROR_MESSAGE_NAME_NOT_REPEATED);
                    return result;
                })
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(filteredConfigs)) {
            return;
        }

        List<String> filteredConfigNames = filteredConfigs.stream()
                .map(AgentAttachmentConfigBO::getName)
                .collect(Collectors.toList());
        List<String> existingConfigNames = attachmentConfigMapper
                .selectObjs(Wrappers.lambdaQuery(AgentAttachmentConfig.class)
                        .select(AgentAttachmentConfig::getName)
                        .in(AgentAttachmentConfig::getName, filteredConfigNames));
        if (!CollectionUtils.isEmpty(existingConfigNames)) {
            Set<String> existingConfigNameSet = new HashSet<>(existingConfigNames);
            filteredConfigs = filteredConfigs.stream().filter(
                            config -> {
                                boolean result = !existingConfigNameSet
                                        .contains(config.getName());
                                AgentCommonVerification.assemblyErrorMsg(resultBO, config.getRowIndex(),
                                        result, ERROR_MESSAGE_NAME_NOT_REPEATED);
                                return result;
                            })
                    .collect(Collectors.toList());
        }
        if (CollectionUtils.isEmpty(filteredConfigs)) {
            return;
        }

        List<AgentAttachmentConfig> configList = BeanUtils.toBean(filteredConfigs, AgentAttachmentConfig.class);
        configList.forEach(config -> {
            config.setCreator(userName);
            config.setCreateTime(now);
        });

        attachmentConfigMapper.insert(configList);
    }
}
