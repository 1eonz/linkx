package com.chinasoft.cloud.module.aiagent.controller.app;

import com.chinasoft.cloud.framework.common.pojo.CommonResult;
import com.chinasoft.cloud.framework.common.pojo.PageParam;
import com.chinasoft.cloud.framework.common.pojo.PageResult;
import com.chinasoft.cloud.framework.tenant.core.aop.TenantIgnore;
import com.chinasoft.cloud.module.aiagent.controller.admin.qo.AgentConfigQO;
import com.chinasoft.cloud.module.aiagent.controller.admin.qo.RecordCountQO;
import com.chinasoft.cloud.module.aiagent.controller.admin.qo.RecordQO;
import com.chinasoft.cloud.module.aiagent.controller.admin.vo.RecordCountVO;
import com.chinasoft.cloud.module.aiagent.controller.app.co.*;
import com.chinasoft.cloud.module.aiagent.controller.app.qo.UserAgentHistoryQO;
import com.chinasoft.cloud.module.aiagent.controller.app.vo.AskApprovalVO;
import com.chinasoft.cloud.module.aiagent.controller.app.vo.AskDetailVO;
import com.chinasoft.cloud.module.aiagent.controller.app.vo.UserAgentHistoryRecordVO;
import com.chinasoft.cloud.module.aiagent.controller.app.vo.UserAgentRecordVO;
import com.chinasoft.cloud.module.aiagent.dal.dataobj.AgentConfig;
import com.chinasoft.cloud.module.aiagent.dal.dataobj.AgentRecord;
import com.chinasoft.cloud.module.aiagent.dal.dataobj.AgentRecordAskType;
import com.chinasoft.cloud.module.aiagent.dal.dataobj.Category;
import com.chinasoft.cloud.module.aiagent.service.AgentRecordService;
import com.chinasoft.cloud.module.aiagent.service.AiAgentAskService;
import com.chinasoft.cloud.module.aiagent.service.AiAgentConfigService;
import com.chinasoft.cloud.module.aiagent.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.web.bind.annotation.*;

import javax.script.ScriptException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Tag(name = "AI")
@RestController
@RequestMapping("/proxy/ai/v1/deepseek-zjk")
public class AiAgentController {
    @Resource
    private AiAgentAskService aiAgentAskService;
    @Resource
    private AiAgentConfigService configService;
    @Resource
    private CategoryService categoryService;
    @Resource
    private AgentRecordService agentRecordService;

    @PermitAll
    @Operation(summary = "发起询问")
    @PostMapping(value = "/xa/dk/contend")
    public CommonResult<Long> xaask(@Valid @RequestBody AskAgentCO param) throws ScriptException {
        trimContent(param);
        resolveAskType(param);
        return CommonResult.success(aiAgentAskService.askLegacy(param));
    }

    @PermitAll
    @Operation(summary = "发起询问 V2")
    @PostMapping(value = "/xa/dk/v2/contend")
    public CommonResult<AskApprovalVO> xaaskV2(@Valid @RequestBody AskAgentCO param) throws ScriptException {
        trimContent(param);
        resolveAskType(param);
        return CommonResult.success(aiAgentAskService.ask(param));
    }

    private void trimContent(AskAgentCO param) {
        if (param.getContent() != null && !param.getContent().isEmpty()
            && param.getContent().charAt(param.getContent().length() - 1) == '\u2005') {
            param.setContent(param.getContent().substring(0, param.getContent().length() - 1));
        }
    }

    private void resolveAskType(AskAgentCO param) {
        if (!AgentRecordAskType.isValid(param.getAskType())) {
            param.setAskType(AgentRecordAskType.AI_ASSISTANT.getValue());
        }
    }

    @PermitAll
    @Operation(summary = "询问结果")
    @GetMapping(value = "/xa/dk/detail")
    public CommonResult<String> xadetail(@RequestParam(name = "id") Long id) {
        AskDetailVO detail = aiAgentAskService.getReply(id);
        return CommonResult.success(detail == null ? null : detail.getReply());
    }

    @PermitAll
    @Operation(summary = "询问结果 V2")
    @GetMapping(value = "/xa/dk/v2/detail")
    public CommonResult<AskDetailVO> xadetailV2(@RequestParam(name = "id") Long id) {
        return CommonResult.success(aiAgentAskService.getReply(id));
    }

    @PermitAll
    @Operation(summary = "AI审核信息")
    @GetMapping(value = "/xa/dk/v2/approval/detail")
    public CommonResult<AskApprovalVO> approvalDetail(@RequestParam(name = "id") Long id) {
        return CommonResult.success(aiAgentAskService.getApproval(id));
    }

    @PermitAll
    @Operation(summary = "修改回复读取状态")
    @PostMapping(value = "/xa/dk/v2/reply/read-state")
    public CommonResult<Boolean> updateReplyReadState(@Valid @RequestBody UpdateReplyReadStateCO readStateCO) {
        return CommonResult.success(aiAgentAskService.updateReplyReadState(readStateCO));
    }

    @PermitAll
    @TenantIgnore
    @Operation(summary = "AI审批建单回调")
    @PostMapping(value = "/xa/dk/approval/create/callback")
    public CommonResult<Boolean> approvalCreatedCallback(@Valid @RequestBody ApproveCreatedCallbackCO callbackCO) {
        aiAgentAskService.approveCreatedCallback(callbackCO);
        return CommonResult.success(true);
    }

    @PermitAll
    @TenantIgnore
    @Operation(summary = "AI审批状态回调")
    @PostMapping(value = "/xa/dk/approval/callback")
    public CommonResult<Boolean> approvalCallback(@Valid @RequestBody ApproveStatusCallbackCO callbackCO)
        throws ScriptException {
        aiAgentAskService.approveCallback(callbackCO);
        return CommonResult.success(true);
    }

    @PermitAll
    @Operation(summary = "智能体列表")
    @GetMapping("/xa/dk/agents")
    public CommonResult<List<Map<String, ?>>> configs(AgentConfigQO qo) {
        qo.setPageSize(PageParam.PAGE_SIZE_NONE);
        qo.setType(1);
        // AI 问答入口仅展示 作用于AI问答(scope=0或1) 的智能体，排除 IM-only(scope=2)
        qo.setScopeList(List.of(0, 1));
        var result = configService.listPaged(qo);
        List<Map<String, ?>> confs = getConfigs(result);
        return CommonResult.success(confs);
    }

    private static @NonNull List<Map<String, ?>> getConfigs(PageResult<AgentConfig> result) {
        return Optional.ofNullable(result)
                .map(PageResult::getList)
                .orElse(List.of())
                .stream()
                .filter(Objects::nonNull)
                .map(c -> {
                    log.info("agent: {}", c);
                    Map<String, Object> map = new HashMap<>();
                    map.put("index", c.getId());
                    map.put("name", c.getName());
                    map.put("token", c.getToken());
                    map.put("picUrl", c.getAvatar());
                    map.put("priority", c.getPriority());
                    map.put("desc", c.getDesc());
                    map.put("isRestricted", c.getIsRestricted());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @PermitAll
    @Operation(summary = "更新用户所有待处理记录的wsSessionId")
    @PostMapping(value = "/xa/dk/approval/status/update")
    public CommonResult<Boolean> updateApprovalStatus(@Valid @RequestBody ApproveStatusUpdateCO param) {
        return CommonResult.success(aiAgentAskService.updateApprovalStatus(param));
    }

    @PermitAll
    @Operation(summary = "查询所有分类")
    @GetMapping("/xa/dk/category/list")
    public CommonResult<List<Category>> categoryList() {
        return CommonResult.success(categoryService.categoryList());
    }

    @PermitAll
    @PostMapping("/record")
    @Operation(summary = "新增记录")
    public CommonResult<Void> addRecord(@RequestBody AgentRecord agentRecord) {
        agentRecord.setTime(LocalDateTime.now());
        log.info("addRecord agentRecord: {}", agentRecord);
        agentRecordService.addRecord(agentRecord);
        return CommonResult.success(null);
    }

    @PermitAll
    @PostMapping("/record/history/list")
    @Operation(summary = "按智能体查询记录")
    public CommonResult<List<AgentRecord>> recordListByAgentName(@RequestBody RecordQO recordQO) {
        log.info("recordListByAgentName recordQO: {}", recordQO);
        return CommonResult.success(agentRecordService.recordListByAgentName(recordQO));
    }

    @PermitAll
    @GetMapping("/xa/dk/record/user/list")
    @Operation(summary = "根据用户ID查询用户使用的智能体列表")
    public CommonResult<List<UserAgentRecordVO>> listUserAgentRecords(@RequestParam("userId") String userId) {
        return CommonResult.success(agentRecordService.listUserAgentRecords(userId));
    }

    @PermitAll
    @DeleteMapping("/xa/dk/record/user/{idCard}/list")
    @Operation(summary = "根据智能体id删除用户使用的智能体列表")
    @Parameter(name = "idCard", description = "用户身份证号码", required = true)
    public CommonResult<Void> deleteUserAgentRecords(@PathVariable("idCard") String idCard, @RequestBody UserAgentRecordVO userAgentRecordVO) {
        agentRecordService.deleteUserAgentRecords(idCard, userAgentRecordVO);
        return CommonResult.success(null);
    }
    @PermitAll
    @GetMapping("/xa/dk/record/history/page")
    @Operation(summary = "按用户和智能体分页查询对话历史")
    public CommonResult<PageResult<UserAgentHistoryRecordVO>> listUserAgentHistoryPaged(@Valid UserAgentHistoryQO qo) {
        return CommonResult.success(agentRecordService.listUserAgentHistoryPaged(qo));
    }

    @PermitAll
    @GetMapping("/xa/dk/v2/record/history/page")
    @Operation(summary = "按用户和智能体分页查询对话历史V2")
    public CommonResult<PageResult<UserAgentHistoryRecordVO>> listUserAgentHistoryPagedV2(@Valid UserAgentHistoryQO qo) {
        return CommonResult.success(agentRecordService.listUserAgentHistoryPaged(qo));
    }

    @PermitAll
    @PostMapping("/record/history/count")
    @Operation(summary = "按智能体统计")
    public CommonResult<List<RecordCountVO>> count(@RequestBody RecordCountQO countQO) {
        log.info("count recordQO: {}", countQO);
        return CommonResult.success(agentRecordService.countRecord(countQO));
    }
}