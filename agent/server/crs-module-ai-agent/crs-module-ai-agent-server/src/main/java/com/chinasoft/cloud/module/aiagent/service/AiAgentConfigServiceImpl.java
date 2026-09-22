package com.chinasoft.cloud.module.aiagent.service;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chinasoft.cloud.framework.common.exception.util.ServiceExceptionUtil;
import com.chinasoft.cloud.framework.common.pojo.PageResult;
import com.chinasoft.cloud.framework.security.core.util.SecurityFrameworkUtils;
import com.chinasoft.cloud.framework.utils.IdWorker;
import com.chinasoft.cloud.module.aiagent.controller.admin.qo.AgentConfigQO;
import com.chinasoft.cloud.module.aiagent.controller.admin.vo.AiAssistantAgentDto;
import com.chinasoft.cloud.module.aiagent.controller.admin.vo.UserVirtualVO;
import com.chinasoft.cloud.module.aiagent.dal.dataobj.AgentAttachmentConfig;
import com.chinasoft.cloud.module.aiagent.dal.dataobj.AgentConfig;
import com.chinasoft.cloud.module.aiagent.dal.mysql.AgentConfigMapper;
import com.chinasoft.cloud.module.aiagent.excel.bo.AgentConfigBO;
import com.chinasoft.cloud.module.aiagent.excel.bo.ResultBO;
import com.chinasoft.cloud.module.aiagent.excel.handler.AgentConfigHandler;
import com.chinasoft.cloud.module.aiagent.excel.listener.AgentCommonReadListener;
import com.chinasoft.cloud.module.aiagent.excel.verification.AgentCommonVerification;
import com.chinasoft.cloud.module.aiagent.excel.verification.AgentConfigVerification;
import com.chinasoft.cloud.module.aiagent.jsengine.JsEngine;
import com.chinasoft.cloud.module.aiagent.msip.aop.LogReport;
import com.chinasoft.cloud.module.aiagent.msip.aop.LogReportParam;
import com.chinasoft.cloud.module.aiagent.msip.enums.OperationTypeEnum;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.service.impl.DiffParseFunction;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import javax.script.ScriptException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.chinasoft.cloud.module.aiagent.enums.ErrorCodeConstants.AGENT_CONFIG_DUPLICATED_TOKEN;
import static com.chinasoft.cloud.module.aiagent.enums.ErrorCodeConstants.FILE_UPLOAD_FAILED;
import static com.chinasoft.cloud.module.aiagent.excel.constant.AgentErrorMessage.*;
import static com.chinasoft.cloud.module.aiagent.excel.handler.AgentConfigHandler.AGENT_SHEET_NAME;
import static com.chinasoft.cloud.module.aiagent.excel.handler.AgentConfigHandler.HEAD_ROW_NUMBER;
import static com.chinasoft.cloud.module.aiagent.util.MimeTypeUtils.APPLICATION_XLSX;

@Slf4j
@Service
@Validated
public class AiAgentConfigServiceImpl implements AiAgentConfigService {

    /**
     * "关联用户"列下拉生效起始行（与表头行 HEAD_ROW_NUMBER 一致）
     */
    private static final int VIRTUAL_USER_DROPDOWN_FIRST_ROW_INDEX = HEAD_ROW_NUMBER;

    /**
     * "关联用户"列下拉生效末行
     */
    private static final int VIRTUAL_USER_DROPDOWN_LAST_ROW_INDEX = 2000;

    /**
     * "关联用户"列头文本
     */
    private static final String VIRTUAL_USER_HEADER = "关联用户";

    @Resource
    private AgentConfigMapper agentConfigMapper;
    @Resource
    private IdWorker idWorker;
    @Resource
    private JsEngine jsEngine;
    @Resource
    private VirtualUserService virtualUserService;

    @Autowired
    private AgentConfigVerification agentConfigVerification;

    @Resource
    private AgentAttachmentConfigService attachmentConfigService;

    @Resource
    private AgentConfigHandler agentConfigHandler;

    private void loadscript(String id, String script) {
        try {
            jsEngine.loadJs(id, script);
        } catch (ScriptException e) {
            log.error("load script error:{} {}", id, script, e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(AgentConfig conf, AiAssistantAgentDto aiAssistantAgentDto) {
        var cnt = agentConfigMapper.selectCount(Wrappers.lambdaQuery(AgentConfig.class)
                .eq(AgentConfig::getName, conf.getName())
                .or(StringUtils.isNotBlank(conf.getToken()))
                .eq(StringUtils.isNotBlank(conf.getToken()), AgentConfig::getToken, conf.getToken()));
        if (cnt > 0) {
            throw ServiceExceptionUtil.exception(AGENT_CONFIG_DUPLICATED_TOKEN);
        }

        var id = idWorker.nextId();
        conf.setId(id);
        updateConf(conf);
        agentConfigMapper.insert(conf);
        loadScriptsWhenCreateOrUpdate(conf);
        LogRecordContext.putVariable("ai-agent-config", conf);
        if (aiAssistantAgentDto.getVirtualUserId() != null) {
            // 如果指定了关联的虚拟用户，则创建虚拟用户绑定关系
            aiAssistantAgentDto.setAgentId(id);
            aiAssistantAgentDto.setId(null);
            virtualUserService.create(aiAssistantAgentDto);
        }
        return id;
    }

    private void loadScriptsWhenCreateOrUpdate(AgentConfig conf) {
        loadscript(conf.getId() + JsEngine.PARAM_SUFFIX,
                StringUtils.isBlank(conf.getParamScript()) ? null : conf.getParamScript());
        log.info("load param script {} {}", conf.getId(), conf.getName());
        loadscript(conf.getId() + JsEngine.RESPONSE_SUFFIX,
                StringUtils.isBlank(conf.getRespScript()) ? null : conf.getRespScript());
        log.info("load response script {} {}", conf.getId(), conf.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateById(AgentConfig conf, AiAssistantAgentDto aiAssistantAgentDto) {
        var old = agentConfigMapper.selectById(conf.getId());
        if (old == null) {
            log.warn("old config not found for id {}", conf.getId());
            return;
        }
        var cnt = agentConfigMapper.selectCount(Wrappers.lambdaQuery(AgentConfig.class).and(w1 -> {
            w1.eq(AgentConfig::getName, conf.getName());
            if (StringUtils.isNotBlank(conf.getToken())) {
                w1.or().eq(AgentConfig::getToken, conf.getToken());
            }
        }).ne(AgentConfig::getId, conf.getId()));
        if (cnt > 0) {
            throw ServiceExceptionUtil.exception(AGENT_CONFIG_DUPLICATED_TOKEN);
        }
        updateConf(conf);
        agentConfigMapper.updateById(conf);
        loadScriptsWhenCreateOrUpdate(conf);
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, conf);
        LogRecordContext.putVariable("ai-agent-config", old);
        var data = virtualUserService.page(null, null, null, conf.getId(), null);
        if (aiAssistantAgentDto.getCreatedUserId() == null) {
            aiAssistantAgentDto.setCreatedUserId(0L);
        }
        if (data.getTotal() == 1) {
            aiAssistantAgentDto.setId(data.getRecords().get(0).getId());
            aiAssistantAgentDto.setAgentId(conf.getId());
            virtualUserService.update(aiAssistantAgentDto);
        } else if (data.getTotal() == 0 && aiAssistantAgentDto.getVirtualUserId() != null) {
            aiAssistantAgentDto.setAgentId(conf.getId());
            aiAssistantAgentDto.setId(null);
            virtualUserService.create(aiAssistantAgentDto);
        }
    }

    @Override
    public void deleteById(Long id) {
        var old = agentConfigMapper.selectById(id);
        if (old == null) {
            log.warn("old config not found for id {}", id);
            return;
        }
        agentConfigMapper.deleteById(id);
        // 3. 记录操作日志上下文
        LogRecordContext.putVariable("ai-agent-config", old);
    }

    @Override
    @LogReport(type = OperationTypeEnum.AGENT_DELETE)
    @Transactional(rollbackFor = Exception.class)
    public void deleteAgentConfig(@LogReportParam AgentConfig conf) {
        agentConfigMapper.deleteById(conf.getId());
        loadscript(conf.getId() + JsEngine.PARAM_SUFFIX, null);
        log.info("load param script {} {}", conf.getId(), conf.getName());
        loadscript(conf.getId() + JsEngine.RESPONSE_SUFFIX, null);
        log.info("load response script {} {}", conf.getId(), conf.getName());
        // 3. 记录操作日志上下文
        LogRecordContext.putVariable("ai-agent-config", conf);
        var data = virtualUserService.page(null, null, null, conf.getId(), null);
        if (data.getTotal() == 1) {
            AiAssistantAgentDto aiAssistantAgentDto = new AiAssistantAgentDto();
            aiAssistantAgentDto.setId(data.getRecords().get(0).getId());
            aiAssistantAgentDto.setAgentId(conf.getId());
            aiAssistantAgentDto.setVirtualUserId(null);
            aiAssistantAgentDto.setCreatedUserId(0L);
            virtualUserService.update(aiAssistantAgentDto);
        }
    }

    @Override
    public PageResult<AgentConfig> listPaged(AgentConfigQO qo) {
        return agentConfigMapper.selectPage(qo,
                Wrappers.lambdaQuery(AgentConfig.class).eq(qo.getType() != null, AgentConfig::getType, qo.getType())
                        .like(Objects.nonNull(qo.getCategoryId()), AgentConfig::getCategoryIds, qo.getCategoryId())
                        .like(StringUtils.isNotBlank(qo.getName()), AgentConfig::getName, qo.getName())
                        .in(!CollectionUtils.isEmpty(qo.getScopeList()), AgentConfig::getScope, qo.getScopeList())
                        .orderByAsc(AgentConfig::getPriority).orderByAsc(AgentConfig::getId));
    }

    @Override
    public AgentConfig getById(Long id) {
        return agentConfigMapper.selectById(id);
    }

    @Override
    public AgentConfig getOneByPriority() {
        return agentConfigMapper.getOneByPriority();
    }

    @Override
    public Long countByCategoryId(Long categoryId) {
        return agentConfigMapper.selectCount(
                Wrappers.lambdaQuery(AgentConfig.class).like(AgentConfig::getCategoryIds, categoryId));
    }

    void updateConf(AgentConfig conf) {
        if (StringUtils.isBlank(conf.getHeader())) {
            conf.setHeader(null);
        }
        if (StringUtils.isBlank(conf.getQuery())) {
            conf.setQuery(null);
        }
        if (conf.getBodyType() == null) {
            conf.setBodyType(null);
        if (conf.getEndFlag() == null) {
            conf.setEndFlag(null);
        }
        }
        if (StringUtils.isBlank(conf.getBody())) {
            conf.setBody(null);
        }
    }

    @Override
    public void template(HttpServletResponse response) throws IOException {
        response.setContentType(APPLICATION_XLSX);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String fileName = URLEncoder.encode("AI智能体导入模板", StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        response.setHeader("Content-disposition",
                "attachment;filename*=utf-8''" + fileName + ".xlsx");

        // 查询虚拟用户列表，用于"关联用户"列下拉选项
        List<String> virtualUserNames = loadVirtualUserNames();

        try (InputStream templateInputStream = getClass()
                .getResourceAsStream("/templates/ai_agent_template.xlsx")) {
            InputStream templateWithDropdown = buildTemplateWithVirtualUserDropdown(
                    templateInputStream, virtualUserNames);
            EasyExcel.write(response.getOutputStream())
                    .withTemplate(templateWithDropdown)
                    .sheet()
                    .doWrite(Collections.emptyList());
        }
    }

    /**
     * 在模板基础上给"关联用户"列挂虚拟用户下拉，仿照排班模板构建方式
     * 列位置按列头文本动态匹配，模板原有下拉保持不变
     */
    private InputStream buildTemplateWithVirtualUserDropdown(InputStream tpl,
                                                              List<String> virtualUserNames) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(tpl);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet mainSheet = workbook.getSheet(AGENT_SHEET_NAME);
            if (mainSheet != null && !CollectionUtils.isEmpty(virtualUserNames)) {
                int colIndex = findColumnByHeader(mainSheet, VIRTUAL_USER_HEADER);
                if (colIndex >= 0) {
                    String[] names = virtualUserNames.toArray(new String[0]);
                    DataValidationHelper helper = mainSheet.getDataValidationHelper();
                    DataValidationConstraint constraint = helper.createExplicitListConstraint(names);
                    CellRangeAddressList range = new CellRangeAddressList(VIRTUAL_USER_DROPDOWN_FIRST_ROW_INDEX,
                            VIRTUAL_USER_DROPDOWN_LAST_ROW_INDEX, colIndex, colIndex);
                    DataValidation validation = helper.createValidation(constraint, range);
                    // 关联用户为可选项，允许留空不选
                    validation.setSuppressDropDownArrow(true);
                    validation.setShowErrorBox(true);
                    validation.setShowPromptBox(true);
                    validation.createPromptBox("提示", "可不选；已选则必须从下拉项中选");
                    mainSheet.addValidationData(validation);
                }
            }
            workbook.write(outputStream);
            return new ByteArrayInputStream(outputStream.toByteArray());
        }
    }

    /**
     * 按列头文本查找列索引
     */
    private int findColumnByHeader(Sheet sheet, String header) {
        Row headerRow = sheet.getRow(HEAD_ROW_NUMBER - 1);
        if (headerRow == null) {
            return -1;
        }
        for (int c = 0; c < headerRow.getLastCellNum(); c++) {
            Cell cell = headerRow.getCell(c);
            if (cell != null && cell.getCellType() == CellType.STRING
                    && header.equals(cell.getStringCellValue())) {
                return c;
            }
        }
        return -1;
    }

    /**
     * 查询未绑定的虚拟用户名称，用于模板"关联用户"列下拉选项
     * 已绑定智能体的虚拟用户（agentId 有值）不返回，避免重复绑定
     * 查询失败时返回空列表，避免影响模板下载
     */
    private List<String> loadVirtualUserNames() {
        try {
            List<UserVirtualVO> virtualUsers = virtualUserService.listVirtualUsers(null);
            if (CollectionUtils.isEmpty(virtualUsers)) {
                return Collections.emptyList();
            }
            // 只返回未绑定的虚拟用户（agentId 为空表示未被智能体绑定），一个虚拟用户不能重复绑定
            return virtualUsers.stream()
                    .filter(v -> v.getAgentId() == null)
                    .map(UserVirtualVO::getUserName)
                    .filter(StringUtils::isNotBlank)
                    .distinct()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("查询虚拟用户列表失败，关联用户下拉将为空", e);
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<String> importAgentsAndAttachmentsFromExcel(MultipartFile file) throws IOException {
        if (Objects.isNull(file) || file.isEmpty()) {
            throw ServiceExceptionUtil.exception(FILE_UPLOAD_FAILED);
        }

        log.info("import agent and agent attachment from excel start. {}", file.getName());
        ResultBO<AgentAttachmentConfig> agentAttachmentConfigResult = attachmentConfigService.importFromExcel(file);
        ResultBO<AgentConfig> agentConfigResult = importFromExcel(file);
        List<String> result = getResult(agentAttachmentConfigResult, agentConfigResult);
        log.info("import agent and agent attachment from excel end.");
        return result;
    }

    private List<String> getResult(ResultBO<AgentAttachmentConfig> agentAttachmentConfigResult,
                                   ResultBO<AgentConfig> agentConfigResult) {
        if (agentAttachmentConfigResult.isEmpty() && agentConfigResult.isEmpty()) {
            throw ServiceExceptionUtil.exception(FILE_UPLOAD_FAILED);
        }

        List<String> result = new ArrayList<>();
        result.addAll(agentAttachmentConfigResult.getRowIndexToErrorMsgMap().values());
        result.addAll(agentConfigResult.getRowIndexToErrorMsgMap().values());
        return result;
    }

    public ResultBO<AgentConfig> importFromExcel(MultipartFile file) throws IOException {
        log.info("import agent from excel start.");
        ResultBO<AgentConfig> result = new ResultBO<>();
        AgentCommonReadListener<AgentConfigBO> listener = new AgentCommonReadListener<>(
                config -> agentConfigVerification.verifyParams(config, result),
                configs -> batchCreate(configs, result));
        EasyExcel.read(file.getInputStream(), AgentConfigBO.class, listener)
                .headRowNumber(HEAD_ROW_NUMBER)
                .autoTrim(true)
                .sheet(AGENT_SHEET_NAME)
                .doRead();

        result.setEmpty(listener.isEmpty());
        log.info("import agent from excel end.");
        return result;
    }

    private void batchCreate(List<AgentConfigBO> configs, ResultBO<AgentConfig> resultBO) {
        if (CollectionUtils.isEmpty(configs)) {
            return;
        }

        Map<String, Long> nameToSizeMap = configs.stream()
                .collect(Collectors.groupingBy(AgentConfigBO::getName, Collectors.counting()));
        List<AgentConfigBO> filteredConfigs = configs.stream()
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
                .map(AgentConfigBO::getName).collect(Collectors.toList());
        List<String> existingConfigNames = agentConfigMapper
                .selectObjs(Wrappers.lambdaQuery(AgentConfig.class)
                        .select(AgentConfig::getName)
                        .in(AgentConfig::getName, filteredConfigNames));
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

        List<AgentConfig> agentConfigs = agentConfigHandler.handler(filteredConfigs, resultBO);
        if (CollectionUtils.isEmpty(agentConfigs)) {
            return;
        }
        // 绑定校验：在入库前过滤掉关联用户校验不通过的配置，与其他校验保持一致（报错就不入库）
        // pendingBindings 收集校验通过且需要绑定的 (rowIndex, virtualUserId, AgentConfig) 三元组
        List<PendingBinding> pendingBindings = new ArrayList<>();
        List<AgentConfig> validAgentConfigs = filterByVirtualUserBinding(
                filteredConfigs, agentConfigs, resultBO, pendingBindings);
        if (CollectionUtils.isEmpty(validAgentConfigs)) {
            return;
        }
        agentConfigMapper.insert(validAgentConfigs);
        validAgentConfigs.forEach(this::loadScriptsWhenCreateOrUpdate);
        // 入库成功后再建立绑定关系（校验已通过，只处理建立失败的情况）
        createVirtualUserBindings(pendingBindings, resultBO);
    }

    /**
     * 待绑定三元组：行号 + 虚拟用户ID + 智能体配置
     * 在 filterByVirtualUserBinding 中收集，传给 createVirtualUserBindings 直接使用，避免二次查询和名称匹配
     */
    private static class PendingBinding {
        final Integer rowIndex;
        final Long virtualUserId;
        final AgentConfig agentConfig;

        PendingBinding(Integer rowIndex, Long virtualUserId, AgentConfig agentConfig) {
            this.rowIndex = rowIndex;
            this.virtualUserId = virtualUserId;
            this.agentConfig = agentConfig;
        }
    }

    /**
     * 在入库前对"关联用户"列做校验过滤，与名称重复等校验保持一致：报错的不入库
     * 校验规则：一个虚拟用户不能重复绑定（已被其他智能体绑定，或本批次内已填写过）
     * 校验通过的配置原样保留，校验失败的记录错误并过滤掉
     * 同时收集校验通过且需要绑定的三元组到 pendingBindings，供入库后建立绑定关系
     *
     * @param pendingBindings 接收需要建立绑定的三元组（调用方传入空列表）
     * @return 校验通过的 AgentConfig 列表（顺序与 filteredConfigs 中通过项一致）
     */
    private List<AgentConfig> filterByVirtualUserBinding(List<AgentConfigBO> configs, List<AgentConfig> agentConfigs,
                                                          ResultBO<AgentConfig> resultBO,
                                                          List<PendingBinding> pendingBindings) {
        boolean hasVirtualUser = configs.stream()
                .anyMatch(c -> StringUtils.isNotBlank(c.getVirtualUser()));
        if (!hasVirtualUser) {
            return agentConfigs;
        }
        // 一次查询同时构建：名称→id 映射 + 已绑定 id 集合（agentId 有值即表示已绑定）
        Map<String, Long> nameToIdMap;
        Set<Long> boundVirtualUserIds;
        try {
            List<UserVirtualVO> virtualUsers = virtualUserService.listVirtualUsers(null);
            if (CollectionUtils.isEmpty(virtualUsers)) {
                nameToIdMap = Collections.emptyMap();
                boundVirtualUserIds = Collections.emptySet();
            } else {
                nameToIdMap = virtualUsers.stream()
                        .filter(v -> v.getUserName() != null)
                        .collect(Collectors.toMap(UserVirtualVO::getUserName, UserVirtualVO::getId, (a, b) -> a));
                boundVirtualUserIds = virtualUsers.stream()
                        .filter(v -> v.getId() != null && v.getAgentId() != null)
                        .map(UserVirtualVO::getId)
                        .collect(Collectors.toSet());
            }
        } catch (Exception e) {
            // 查询失败时为安全起见，跳过绑定校验，全部放行入库（不建立绑定，由后续手动处理）
            log.error("查询虚拟用户列表失败，跳过绑定校验，不建立绑定关系", e);
            return agentConfigs;
        }
        // 批次内已绑定的虚拟用户，用于批次内去重
        Set<Long> boundInBatch = new HashSet<>();
        List<AgentConfig> validAgentConfigs = new ArrayList<>();
        for (int i = 0; i < configs.size(); i++) {
            AgentConfigBO bo = configs.get(i);
            AgentConfig cfg = agentConfigs.get(i);
            String virtualUserName = bo.getVirtualUser();
            // 关联用户为空：可选字段，正常入库
            if (StringUtils.isBlank(virtualUserName)) {
                validAgentConfigs.add(cfg);
                continue;
            }
            Long virtualUserId = nameToIdMap.get(virtualUserName);
            if (virtualUserId == null) {
                AgentCommonVerification.assemblyErrorMsg(resultBO, bo.getRowIndex(), false, ERROR_MESSAGE_VIRTUAL_USER);
                continue;
            }
            // 一个虚拟用户不能重复绑定：已被其他智能体绑定，或本批次内已处理过
            if (boundVirtualUserIds.contains(virtualUserId) || !boundInBatch.add(virtualUserId)) {
                AgentCommonVerification.assemblyErrorMsg(resultBO, bo.getRowIndex(), false, ERROR_MESSAGE_VIRTUAL_USER_BOUND);
                continue;
            }
            validAgentConfigs.add(cfg);
            // 收集待绑定三元组，入库后直接使用，避免二次查询和名称匹配
            pendingBindings.add(new PendingBinding(bo.getRowIndex(), virtualUserId, cfg));
        }
        return validAgentConfigs;
    }

    /**
     * 建立智能体与虚拟用户的绑定关系，复用新增配置逻辑（virtualUserService.create）
     * 前置校验已在 filterByVirtualUserBinding 完成，这里只处理建立失败的情况
     */
    private void createVirtualUserBindings(List<PendingBinding> pendingBindings, ResultBO<AgentConfig> resultBO) {
        if (CollectionUtils.isEmpty(pendingBindings)) {
            return;
        }
        Long createdUserId = SecurityFrameworkUtils.getLoginUserId();
        if (createdUserId == null) {
            createdUserId = 0L;
        }
        for (PendingBinding binding : pendingBindings) {
            try {
                AiAssistantAgentDto dto = new AiAssistantAgentDto();
                dto.setAgentId(binding.agentConfig.getId());
                dto.setVirtualUserId(binding.virtualUserId);
                dto.setCreatedUserId(createdUserId);
                virtualUserService.create(dto);
            } catch (Exception e) {
                log.error("建立智能体与虚拟用户绑定关系失败, agentId:{}, virtualUserId:{}",
                        binding.agentConfig.getId(), binding.virtualUserId, e);
                AgentCommonVerification.assemblyErrorMsg(resultBO, binding.rowIndex, false, ERROR_MESSAGE_VIRTUAL_USER_BIND);
            }
        }
    }
}