package com.chinasoft.cloud.module.aiagent.controller.admin;

import com.alibaba.fastjson.JSON;
import com.chinasoft.cloud.framework.common.exception.util.ServiceExceptionUtil;
import com.chinasoft.cloud.framework.common.pojo.CommonResult;
import com.chinasoft.cloud.framework.common.pojo.PageParam;
import com.chinasoft.cloud.framework.common.pojo.PageResult;
import com.chinasoft.cloud.framework.common.util.object.BeanUtils;
import com.chinasoft.cloud.framework.excel.core.util.ExcelUtils;
import com.chinasoft.cloud.module.aiagent.controller.admin.co.UpsertAgentConfigCO;
import com.chinasoft.cloud.module.aiagent.controller.admin.co.UpsertCategoryCO;
import com.chinasoft.cloud.module.aiagent.controller.admin.qo.AgentConfigQO;
import com.chinasoft.cloud.module.aiagent.controller.admin.qo.AgentRecordCursorQO;
import com.chinasoft.cloud.module.aiagent.controller.admin.qo.AgentRecordQO;
import com.chinasoft.cloud.module.aiagent.controller.admin.qo.ExportAgentRecordQO;
import com.chinasoft.cloud.module.aiagent.controller.admin.qo.RecordCountQO;
import com.chinasoft.cloud.module.aiagent.controller.admin.qo.RecordQO;
import com.chinasoft.cloud.module.aiagent.controller.admin.vo.AgenRecordExportVO;
import com.chinasoft.cloud.module.aiagent.controller.admin.vo.AgentConfigVO;
import com.chinasoft.cloud.module.aiagent.controller.admin.vo.AgentRecordCursorVO;
import com.chinasoft.cloud.module.aiagent.controller.admin.vo.AiAssistantAgentDto;
import com.chinasoft.cloud.module.aiagent.controller.admin.vo.RecordCountVO;
import com.chinasoft.cloud.module.aiagent.dal.dataobj.AgentConfig;
import com.chinasoft.cloud.module.aiagent.dal.dataobj.AgentRecord;
import com.chinasoft.cloud.module.aiagent.dal.dataobj.Category;
import com.chinasoft.cloud.module.aiagent.msip.aop.LogReport;
import com.chinasoft.cloud.module.aiagent.msip.aop.LogReportParam;
import com.chinasoft.cloud.module.aiagent.msip.enums.OperationTypeEnum;
import com.chinasoft.cloud.module.aiagent.service.AgentRecordService;
import com.chinasoft.cloud.module.aiagent.service.AiAgentConfigService;
import com.chinasoft.cloud.module.aiagent.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.chinasoft.cloud.module.aiagent.enums.ErrorCodeConstants.FILE_HAS_ABNORMAL_DATA;
import static com.chinasoft.cloud.module.aiagent.enums.ErrorCodeConstants.FILE_UPLOAD_FAILED;

@Slf4j
@Tag(name = "智能体管理")
@RestController
@RequestMapping("/proxy/ai/v1/aiagent/management")
public class AiAgentConfigController {

    @Resource
    private AiAgentConfigService agentConfigService;
    @Resource
    private AgentRecordService agentRecordService;
    @Resource
    private CategoryService categoryService;

    @PermitAll
    @PostMapping
    @Operation(summary = "新增")
    @LogReport(type = OperationTypeEnum.AGENT_INSERT)
    public CommonResult<Long> createConfig(@Valid @RequestBody @LogReportParam UpsertAgentConfigCO configCO) {
        AgentConfig config = convertToEntity(configCO);
        AiAssistantAgentDto aiAssistantAgentDto = BeanUtils.toBean(configCO, AiAssistantAgentDto.class);
        var id = agentConfigService.create(config, aiAssistantAgentDto);
        return CommonResult.success(id);
    }

    @PermitAll
    @PutMapping("/{id}")
    @Operation(summary = "更新")
    @LogReport(type = OperationTypeEnum.AGENT_UPDATE)
    public CommonResult<Void> updateConfig(@PathVariable("id") Long id,
        @Valid @RequestBody @LogReportParam UpsertAgentConfigCO configCO) {
        AgentConfig config = convertToEntity(configCO);
        config.setId(id);
        config.setType(null);
        AiAssistantAgentDto aiAssistantAgentDto = BeanUtils.toBean(configCO, AiAssistantAgentDto.class);
        agentConfigService.updateById(config, aiAssistantAgentDto);
        return CommonResult.success(null);
    }

    private AgentConfig convertToEntity(UpsertAgentConfigCO configCO) {
        AgentConfig config = BeanUtils.toBean(configCO, AgentConfig.class);
        config.setAudioType(listToJson(configCO.getAudioType()));
        config.setVideoType(listToJson(configCO.getVideoType()));
        config.setImageType(listToJson(configCO.getImageType()));
        config.setDocumentType(listToJson(configCO.getDocumentType()));
        if (StringUtils.isBlank(config.getHeader())) {
            config.setHeader(null);
        }
        if (StringUtils.isBlank(config.getQuery())) {
            config.setQuery(null);
        }
        if (StringUtils.isBlank(config.getBody())) {
            config.setBody(null);
        }
        return config;
    }

    private String listToJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return JSON.toJSONString(list);
    }

    @PermitAll
    @DeleteMapping("/{id}")
    @Operation(summary = "删除")
    public CommonResult<Void> updateConfig(@PathVariable("id") Long id) {
        AgentConfig old = agentConfigService.getById(id);
        if (old == null) {
            log.warn("old config not found for id {}", id);
        } else {
            agentConfigService.deleteAgentConfig(old);
        }
        return CommonResult.success(null);
    }

    @PermitAll
    @GetMapping("/page")
    @Operation(summary = "分页查询")
    public CommonResult<PageResult<AgentConfigVO>> listPaged(AgentConfigQO configQo) {
        PageResult<AgentConfig> dbResult = agentConfigService.listPaged(configQo);
        List<AgentConfig> dataList = dbResult.getList();
        PageResult<AgentConfigVO> resultPage = new PageResult<>(dbResult.getTotal());
        resultPage.setList(
            dataList.stream().map(data -> BeanUtils.toBean(data, AgentConfigVO.class)).collect(Collectors.toList()));
        if (CollectionUtils.isNotEmpty(resultPage.getList())) {
            List<Long> categoryIdList =
                resultPage.getList().stream().filter(data -> StringUtils.isNotBlank(data.getCategoryIds()))
                    .flatMap(data -> Arrays.stream(data.getCategoryIds().split(","))).distinct().map(Long::valueOf)
                    .collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(categoryIdList)) {
                Map<Long, String> categoryMap = categoryService.findByIdList(categoryIdList).stream()
                    .collect(Collectors.toMap(Category::getId, Category::getName));
                resultPage.getList().forEach(data -> {
                    String categoryIds = data.getCategoryIds();
                    if (StringUtils.isNotBlank(categoryIds)) {
                        String categoryNames = Arrays.stream(categoryIds.split(","))
                            .map(categoryId -> categoryMap.get(Long.valueOf(categoryId)))
                            .collect(Collectors.joining(","));
                        data.setCategoryName(categoryNames);
                    }
                });
            }
        }
        return CommonResult.success(resultPage);
    }

    @PermitAll
    @GetMapping("/byid/{id}")
    @Operation(summary = "ID查询")
    public CommonResult<AgentConfig> listPaged(@PathVariable("id") Long id) {
        return CommonResult.success(agentConfigService.getById(id));
    }

    @PermitAll
    @GetMapping("/all")
    @Operation(summary = "查全部")
    public CommonResult<List<AgentConfig>> listAll() {
        var qo = new AgentConfigQO();
        qo.setPageSize(PageParam.PAGE_SIZE_NONE);
        var agentConfigPageResult = agentConfigService.listPaged(qo);
        return CommonResult.success(agentConfigPageResult.getList());
    }

    @PermitAll
    @GetMapping("/count")
    @Operation(summary = "获取智能体数量")
    public CommonResult<Long> getAgentCount() {
        AgentConfigQO qo = new AgentConfigQO();
        qo.setPageSize(PageParam.PAGE_SIZE_NONE);
        PageResult<AgentConfig> result = agentConfigService.listPaged(qo);
        return CommonResult.success(result.getTotal());
    }

    @PermitAll
    @PostMapping("/record")
    @Operation(summary = "新增记录")
    public CommonResult<Void> addRecord(@RequestBody AgentRecord agentRecord) {
        agentRecord.setTime(LocalDateTime.now());
        log.info("addRecord agentRecord: {}", agentRecord);
        agentRecordService.addRecord(agentRecord);
        if (agentRecord.getAnswerTime() == null) {
            agentRecord.setAnswerTime(LocalDateTime.now());
        }
        return CommonResult.success(null);
    }

    @PermitAll
    @PostMapping("/history/list")
    @Operation(summary = "按智能体名称查询记录")
    public CommonResult<List<AgentRecord>> recordListByAgentName(@RequestBody RecordQO recordQO) {
        log.info("recordListByAgentName recordQO: {}", recordQO);
        return CommonResult.success(agentRecordService.recordListByAgentName(recordQO));
    }

    @PermitAll
    @PostMapping("/history/count")
    @Operation(summary = "按智能体名称统计")
    public CommonResult<List<RecordCountVO>> count(@RequestBody RecordCountQO countQO) {
        log.info("count recordQO: {}", countQO);
        return CommonResult.success(agentRecordService.countRecord(countQO));
    }

    @PermitAll
    @GetMapping("/record")
    @Operation(summary = "查询记录")
    public CommonResult<PageResult<AgentRecord>> listRecord(AgentRecordQO statisticsQO) {
        return CommonResult.success(agentRecordService.listRecordPaged(statisticsQO));
    }

    @PermitAll
    @GetMapping("/record/cursor")
    @Operation(summary = "游标分页查询记录", description = "按 (time, id) 双游标增量拉取，用于统计定时任务。agentConfigId=-1 表示人员核查记录。首次拉取 timeAfter/idAfter 传 null，后续传上次返回的 lastTime/lastId。size 为每页大小。")
    public CommonResult<AgentRecordCursorVO> listRecordByCursor(AgentRecordCursorQO qo) {
        log.info("listRecordByCursor qo: {}", qo);
        AgentRecordCursorVO result = agentRecordService.listRecordByCursor(qo);
        log.info("listRecordByCursor result size: {}, hasMore: {}, lastTime: {}, lastId: {}",
            result.getList() == null ? 0 : result.getList().size(), result.getHasMore(),
            result.getLastTime(), result.getLastId());
        return CommonResult.success(result);
    }

    @PermitAll
    @DeleteMapping("/record/{id}")
    @Operation(summary = "删除记录")
    public CommonResult<PageResult<AgentRecord>> deleteRecordById(@PathVariable("id") Long id) {
        agentRecordService.deleteById(id);
        return CommonResult.success(null);
    }

    @PermitAll
    @GetMapping(value = "/record/export")
    @Operation(summary = "导出记录")
    public void exportSmsTemplateExcel(ExportAgentRecordQO statisticsQO, HttpServletResponse response)
        throws IOException {
        var bean = BeanUtils.toBean(statisticsQO, AgentRecordQO.class);
        bean.setPageSize(PageParam.PAGE_SIZE_NONE);
        if (statisticsQO.getIds() != null && !statisticsQO.getIds().isBlank()) {
            var split = statisticsQO.getIds().split(",");
            bean.setIdList(
                Arrays.stream(split).filter(a -> !a.isBlank()).map(Long::parseLong).collect(Collectors.toList()));
        }
        var agentRecordPageResult = agentRecordService.listRecordPaged(bean);
        var target = BeanUtils.toBean(agentRecordPageResult.getList(), AgenRecordExportVO.class);
        ExcelUtils.write(response, "查询统计.xlsx", "查询统计", AgenRecordExportVO.class, target);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public CommonResult<String> handleDuplicateKeyException(DuplicateKeyException ex) {
        log.warn("唯一键冲突", ex);
        return CommonResult.error(-1, "数据重复，请检查输入内容");
    }

    @PermitAll
    @GetMapping("/category/list")
    @Operation(summary = "查询所有分类")
    public CommonResult<List<Category>> categoryList() {
        return CommonResult.success(categoryService.categoryList());
    }

    @PermitAll
    @PostMapping("/category/saveOrUpdate/batch")
    @Operation(summary = "新增分类")
    public CommonResult<Void> saveOrUpdateCategoryBatch(@Valid @RequestBody List<UpsertCategoryCO> categoryCOList) {
        categoryService.saveOrUpdateBatch(categoryCOList);
        return CommonResult.success(null);
    }

    @PermitAll
    @DeleteMapping("/category/{id}")
    @Operation(summary = "删除分类")
    public CommonResult<Void> deleteCategory(@PathVariable("id") Long id) {
        categoryService.deleteById(id);
        return CommonResult.success(null);
    }

    @PermitAll
    @Operation(summary = "AI智能体模板下载")
    @GetMapping("/template")
    public void template(HttpServletResponse response) throws IOException {
        agentConfigService.template(response);
    }

    @PermitAll
    @Operation(summary = "AI智能体导入")
    @PostMapping("/import")
    public CommonResult<List<String>> importAgentsAndAttachmentsFromExcel(MultipartFile file) {
        try {
            List<String> result = agentConfigService.importAgentsAndAttachmentsFromExcel(file);
            return CollectionUtils.isEmpty(result)
                    ? CommonResult.success(null)
                    : CommonResult.error(FILE_HAS_ABNORMAL_DATA.getCode(), null, result);
        } catch (Exception exception) {
            log.error("importAgentsAndAttachmentsFromExcel error.", exception);
            throw ServiceExceptionUtil.exception(FILE_UPLOAD_FAILED);
        }
    }
}