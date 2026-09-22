package com.chinasoft.cloud.module.aiagent.service;

import com.alibaba.fastjson.JSONObject;
import com.chinasoft.cloud.module.aiagent.enums.BodyTypeEnum;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chinasoft.cloud.framework.common.exception.util.ServiceExceptionUtil;
import com.chinasoft.cloud.framework.common.util.json.JsonUtils;
import com.chinasoft.cloud.framework.utils.IdWorker;
import com.chinasoft.cloud.module.aiagent.controller.admin.vo.AiSettingsVO;
import com.chinasoft.cloud.module.aiagent.controller.app.co.*;
import com.chinasoft.cloud.module.aiagent.controller.app.vo.AskApprovalVO;
import com.chinasoft.cloud.module.aiagent.controller.app.vo.AskDetailVO;
import com.chinasoft.cloud.module.aiagent.dal.dataobj.AgentConfig;
import com.chinasoft.cloud.module.aiagent.dal.dataobj.AgentQueryApprove;
import com.chinasoft.cloud.module.aiagent.dal.dataobj.AgentRecord;
import com.chinasoft.cloud.module.aiagent.dal.dataobj.Globals;
import com.chinasoft.cloud.module.aiagent.dal.mysql.AgentQueryApproveMapper;
import com.chinasoft.cloud.module.aiagent.dal.mysql.AgentRecordMapper;
import com.chinasoft.cloud.module.aiagent.dal.mysql.GlobalsMapper;
import com.chinasoft.cloud.module.aiagent.enums.ErrorCodeConstants;
import com.chinasoft.cloud.module.aiagent.jsengine.JsEngine;
import com.chinasoft.cloud.module.aiagent.msip.aop.LogReport;
import com.chinasoft.cloud.module.aiagent.msip.entity.OperationLog;
import com.chinasoft.cloud.module.aiagent.msip.enums.AlarmTemplateZhEnum;
import com.chinasoft.cloud.module.aiagent.msip.enums.OperationTypeEnum;
import com.chinasoft.cloud.module.aiagent.msip.util.ReportUtil;
import com.chinasoft.cloud.module.aiagent.service.AiAgentRequestParamResolver.AskRequestDefinition;
import com.chinasoft.cloud.module.aiagent.service.AiAgentRequestParamResolver.RequestInput;
import com.chinasoft.cloud.module.aiagent.util.HttpsUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okio.BufferedSource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.script.ScriptException;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@Validated
public class AiAgentAskServiceImpl implements AiAgentAskService {
//    private static final OkHttpClient client =
//        new OkHttpClient.Builder().callTimeout(5L, TimeUnit.MINUTES).readTimeout(5L, TimeUnit.MINUTES)
//            .connectTimeout(Duration.ofSeconds(10L)).build();
    // 使用 HttpsUtil 中已配置好信任所有证书的 OkHttpClient
    private static final OkHttpClient client = HttpsUtil.client;
    private static final String REDIS_CACHE_KEY_REFIX = "crs:ai-agent:ask-result:";
    private static final String REDIS_APPROVAL_STATUS_KEY_PREFIX = "crs:ai-agent:approval-status:";
    private static final String APPROVAL_STATUS_NO_APPROVAL = "0";
    private static final String APPROVAL_STATUS_PENDING_APPROVAL = "1";
    private static final String APPROVAL_STATUS_FINISHED = "2";
    private static final String APPROVAL_STATUS_WAIT_CREATE = "3";
    private static final String REDIS_APPROVAL_PUSH_TARGET_KEY_PREFIX = "crs:ai-agent:approval:push-target:";

    @Resource
    private IdWorker idWorker;
    @Resource(name = "askPool")
    private ThreadPoolTaskExecutor executorService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private AiAgentConfigService aiAgentConfigService;
    @Resource
    private AgentRecordService agentRecordService;
    @Resource
    private AgentRecordMapper agentRecordMapper;
    @Resource
    private AgentQueryApproveMapper agentQueryApproveMapper;
    @Resource
    private GlobalsMapper globalsMapper;
    @Resource
    private AgentQueryApproveService agentQueryApproveService;
    @Resource
    private GlobalsService globalsService;
    @Resource
    private ReportUtil reportUtil;
    @Resource
    private JsEngine jsEngine;
    @Resource
    private AiAgentRequestParamResolver requestParamResolver;
    @Resource
    private AiAgentWebSocketPushService webSocketPushService;
    @Resource
    private AiAgentReplyStateService replyStateService;
    @Resource
    private AiAgentFileService aiAgentFileService;

    @PostConstruct
    public void init() {
        requestParamResolver.setAiAgentFileService(aiAgentFileService);
    }

    @Override
    public Long askLegacy(AskAgentCO askAgentCO) throws ScriptException {
        return ask(askAgentCO, true).getId();
    }

    @Override
    public AskApprovalVO ask(AskAgentCO askAgentCO) throws ScriptException {
        return ask(askAgentCO, false);
    }

    private AskApprovalVO ask(AskAgentCO askAgentCO, boolean skipApproval) throws ScriptException {
        Long id = idWorker.nextId();
        log.info("ask id:{},value:{}", id, askAgentCO);

        // 获取agent的配置
        AgentConfig config = findConfig(askAgentCO.getAgent());
        if (config == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.AGENT_CONFIG_NOT_FOUND);
        }

        // 上报提问操作日志：operation 以 "imExtra" 开头，受 ai_globals.SAVE_IMEXTRA 开关控制
        reportImExtraAskLog(askAgentCO, config);

        if (!skipApproval) {
            webSocketPushService.putPushTarget(id, askAgentCO.getWsSessionId());
        }

        // 获取审核相关配置
        AiSettingsVO settings = skipApproval ? null : globalsService.getAiSettings();
        boolean approvalRequired = !skipApproval && shouldRequireApproval(settings, config);
        String approvalSubMode = skipApproval
            ? AiAgentGlobalConstants.APPROVAL_SUB_MODE_ASK_BEFORE_APPROVE
            : resolveApprovalSubMode(settings);
        // 保存对话记录
        createRecord(id, askAgentCO, config, approvalRequired, approvalSubMode);

        ApprovalCache approvalCache = buildInitialApprovalCache(id, approvalRequired, approvalSubMode);

        // 缓存会话审核配置、回答状态
        putApprovalCache(approvalCache);
        replyStateService.putReplyState(id, 0);

        // 不审核 提问后直接返回
        if (!approvalRequired) {
            submitAskTask(config, id, RequestInput.from(askAgentCO), approvalCache);
            return buildAskApprovalVO(id, approvalCache, null, config.getName(), askAgentCO.getUserName());
        }

        // 审批单由前端 H5 创建，ask 阶段只返回建单地址；本地审批表等第三方建单回调后再落库。
        approvalCache.setApprovalStatus(APPROVAL_STATUS_WAIT_CREATE);
        approvalCache.setApproveUrl(buildApprovalCreateUrl(settings.getApprovalSystemUrl(), id));
        putApprovalCache(approvalCache);
        // 先问后审：提问立即提交；先审后问：等审批状态回调通过后再提交。
        if (shouldSubmitAskImmediately(approvalCache)) {
            submitAskTask(config, id, RequestInput.from(askAgentCO), approvalCache);
        }
        return buildAskApprovalVO(id, approvalCache, null, config.getName(), askAgentCO.getUserName());
    }

    @Override
    public AskDetailVO getReply(Long id) {
        // redis中获取回答
        String cachedReply = stringRedisTemplate.opsForValue().get(REDIS_CACHE_KEY_REFIX + id);
        ApprovalCache approvalCache = getApprovalCache(id);
        // redis不存在 缓存过期 重新加载
        if (approvalCache == null) {
            approvalCache = loadApprovalCache(id);
            putApprovalCache(approvalCache);
        }
        // 获取回答内容
        String reply = resolveReply(id, cachedReply, approvalCache);
        // 判断数据库是否已有最终回答
        boolean hasFinalResp = hasFinalResponse(id);
        // 获取并更新回答状态
        AiAgentReplyStateService.ReplyState replyState = replyStateService.resolveReplyState(id, reply,
            canReturnReplyDirectly(approvalCache), hasFinalResp);
        AskDetailVO detailVO = new AskDetailVO();
        detailVO.setId(id);
        detailVO.setReply(reply);
        if (hasFinalResp) {
            detailVO.setReplyPosition(-1);
            detailVO.setReplyPaused(false);
            // 数据库已有回答，确保状态字段也更新（处理用户暂停后继续生成的场景）
            if (replyState.getReplyPosition() == null
                    || replyState.getReplyPosition() != -1
                    || !Boolean.FALSE.equals(replyState.getReplyPaused())) {
                replyStateService.updateReadState(id, -1, Boolean.FALSE);
            }
        } else {
            detailVO.setReplyPosition(replyState.getReplyPosition());
            detailVO.setReplyPaused(replyState.getReplyPaused());
        }
        AgentRecord record = agentRecordMapper.selectById(id);
        if (record != null) {
            detailVO.setAgentName(record.getAgentName());
            detailVO.setUserName(record.getUserName());
            detailVO.setAnswerTime(record.getAnswerTime());
            detailVO.setQueryTime(record.getTime());
        }
        return detailVO;
    }

    @Override
    public Boolean updateReplyReadState(UpdateReplyReadStateCO readStateCO) {
        if (readStateCO == null || readStateCO.getId() == null) {
            return false;
        }
        replyStateService.updateReadState(readStateCO.getId(), readStateCO.getReplyPosition(),
            readStateCO.getReplyPaused());
        return true;
    }

    @Override
    public AskApprovalVO getApproval(Long id) {
        ApprovalCache approvalCache = getApprovalCache(id);
        if (approvalCache == null) {
            approvalCache = loadApprovalCache(id);
            putApprovalCache(approvalCache);
        }
        AgentQueryApprove approveRecord = agentQueryApproveMapper.selectOne(Wrappers.lambdaQuery(AgentQueryApprove.class)
            .eq(AgentQueryApprove::getRecordId, String.valueOf(id))
            .last("limit 1"));
        return buildApprovalVO(id, approvalCache, approveRecord);
    }

    private AskApprovalVO buildApprovalVO(Long id, ApprovalCache approvalCache, AgentQueryApprove approveRecord) {
        AskApprovalVO approvalVO = new AskApprovalVO();
        approvalVO.setId(id);
        approvalVO.setApprovalRequired(approvalCache != null && Boolean.TRUE.equals(approvalCache.getApprovalRequired()));
        approvalVO.setApprovalStatus(approvalCache == null ? APPROVAL_STATUS_NO_APPROVAL : approvalCache.getApprovalStatus());
        approvalVO.setApprovalSubMode(approvalCache == null ? null : approvalCache.getApprovalSubMode());
        approvalVO.setApproveResult(approvalCache == null ? null : approvalCache.getApproveResult());
        approvalVO.setApproveNo(approvalCache == null ? null : approvalCache.getApproveNo());
        approvalVO.setApproveUrl(approvalCache == null ? null : approvalCache.getApproveUrl());
        approvalVO.setApproveDetailUrl(approvalCache == null ? null : approvalCache.getApproveDetailUrl());
        approvalVO.setToLeaderUrl(approvalCache == null ? null : approvalCache.getToLeaderUrl());
        if (approveRecord != null) {
            approvalVO.setApprovalRequired(true);
            // 已收到第三方建单/状态回调后，审批表是状态源；避免旧缓存继续显示“待建单”。
            approvalVO.setApprovalStatus(normalizeApprovalStatus(
                StringUtils.defaultIfBlank(approveRecord.getApproveState(), APPROVAL_STATUS_PENDING_APPROVAL),
                approveRecord.getApprove()));
            approvalVO.setApproveResult(approveRecord.getApprove());
            approvalVO.setApproveNo(firstNonBlank(approvalVO.getApproveNo(), approveRecord.getApproveNo()));
            approvalVO.setApproveUrl(firstNonBlank(approvalVO.getApproveUrl(), approveRecord.getApproveUrl()));
            approvalVO.setApproveDetailUrl(firstNonBlank(approvalVO.getApproveDetailUrl(), approveRecord.getApproveDetailUrl()));
            approvalVO.setToLeaderUrl(firstNonBlank(approvalVO.getToLeaderUrl(), approveRecord.getToLeaderUrl()));
            approvalVO.setApproveDescription(approveRecord.getApproveDescription());
            approvalVO.setApproveTime(approveRecord.getApproveTime());
            approvalVO.setApproveUser(approveRecord.getApproveUser());
        }
        return approvalVO;
    }

    private AskApprovalVO buildAskApprovalVO(Long id, ApprovalCache approvalCache, AgentQueryApprove approveRecord,
        String agentName, String userName) {
        AskApprovalVO approvalVO = buildApprovalVO(id, approvalCache, approveRecord);
        approvalVO.setAgentName(agentName);
        approvalVO.setUserName(userName);
        approvalVO.setQueryTime(agentRecordService.getRecordById(id).map(AgentRecord::getTime).orElse(null));
        return approvalVO;
    }

    @Override
    public void approveCreatedCallback(ApproveCreatedCallbackCO callbackCO) {
        log.info("approveCreatedCallback callbackCO:{}", callbackCO);
        ApprovalCallbackData callbackData = ApprovalCallbackData.from(callbackCO);
        CallbackApproveRecord callbackRecord = loadOrCreateCallbackRecord(callbackData, true);
        AgentQueryApprove approveRecord = callbackRecord.getApproveRecord();
        log.info("approveCreatedCallback recordId:{}, approveNo:{}, created:{}, approveState:{}",
            callbackCO.getRecordId(), approveRecord.getApproveNo(),
            callbackRecord.isCreated(), approveRecord.getApproveState());
        if (!callbackRecord.isCreated()) {
            updateApproveCreatedRecord(approveRecord, callbackData);
        }
        ApprovalCache approvalCache = getApprovalCache(callbackCO.getRecordId());
        if (approvalCache == null) {
            approvalCache = loadApprovalCache(callbackCO.getRecordId());
        }
        mergeApprovalCache(approvalCache, approveRecord);
        putApprovalCache(approvalCache);
        // 第三方完成建单后，前端收到该消息即可关闭审批 H5 页面。
        AgentRecord agentRecord = agentRecordMapper.selectById(callbackCO.getRecordId());
        if (agentRecord == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.RECORD_NOT_FOUND);
        }
        log.info("approveCreatedCallback wsPush recordId:{}, approveNo:{}, approvalStatus:{}",
            callbackCO.getRecordId(), approveRecord.getApproveNo(), approvalCache.getApprovalStatus());
        webSocketPushService.sendApprovalCreated(agentRecord, approvalCache.getApprovalStatus(),
            approvalCache.getApproveResult(), approveRecord.getApproveNo(), approveRecord.getApproveDetailUrl(),
            approveRecord.getToLeaderUrl(), approveRecord.getApproveUser());
    }

    @Override
    public void approveCallback(ApproveStatusCallbackCO callbackCO) throws ScriptException {
        log.info("approveCallback callbackCO:{}", callbackCO);
        ApprovalCallbackData callbackData = ApprovalCallbackData.from(callbackCO);
        CallbackApproveRecord callbackRecord = loadOrCreateCallbackRecord(callbackData, true);
        AgentQueryApprove approveRecord = callbackRecord.getApproveRecord();
        log.info("approveCallback recordId:{}, approveNo:{}, created:{}, approveState:{}",
            callbackCO.getRecordId(), approveRecord.getApproveNo(),
            callbackRecord.isCreated(), approveRecord.getApproveState());
        // 回写申请状态
        boolean firstFinishedCallback = callbackRecord.isCreated()
            && Objects.equals(approveRecord.getApproveState(), APPROVAL_STATUS_FINISHED);
        if (!callbackRecord.isCreated()) {
            firstFinishedCallback = updateApproveRecord(approveRecord, callbackData);
        }
        ApprovalCache approvalCache = getApprovalCache(callbackCO.getRecordId());
        if (approvalCache == null) {
            approvalCache = loadApprovalCache(callbackCO.getRecordId());
        }
        // 更新缓存
        mergeApprovalCache(approvalCache, approveRecord);
        putApprovalCache(approvalCache);
        // 先审后问 1发送提问请求给智能体 2发送websocket消息给前端
        if (shouldSubmitAskAfterApproval(approvalCache, approveRecord, firstFinishedCallback)) {
            AgentRecord agentRecord = agentRecordMapper.selectById(callbackCO.getRecordId());
            if (agentRecord == null) {
                throw ServiceExceptionUtil.exception(ErrorCodeConstants.RECORD_NOT_FOUND);
            }
            AgentConfig config = aiAgentConfigService.getById(agentRecord.getAgentConfigId());
            if (config == null) {
                throw ServiceExceptionUtil.exception(ErrorCodeConstants.AGENT_CONFIG_NOT_FOUND);
            }
            submitAskTask(config, agentRecord.getId(), RequestInput.from(agentRecord), approvalCache);
        }
        // 状态回调均通知前端；已处理过的完成态重复回调不再重复推送。
        AgentRecord agentRecord = agentRecordMapper.selectById(callbackCO.getRecordId());
        if (agentRecord == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.RECORD_NOT_FOUND);
        }
        boolean shouldPush = firstFinishedCallback
            || !Objects.equals(approveRecord.getApproveState(), APPROVAL_STATUS_FINISHED);
        log.info("approveCallback wsPush recordId:{}, shouldPush:{}, firstFinished:{}, approveState:{}",
            callbackCO.getRecordId(), shouldPush, firstFinishedCallback, approveRecord.getApproveState());
        if (shouldPush) {
            webSocketPushService.sendApprovalChanged(agentRecord, approvalCache.getApprovalStatus(),
                approvalCache.getApproveResult(), approveRecord.getApproveNo(), approveRecord.getApproveDetailUrl(),
                approveRecord.getApproveUser());
        }
    }

    private CallbackApproveRecord loadOrCreateCallbackRecord(ApprovalCallbackData callbackData,
        boolean fillCallbackData) {
        Long recordId = resolveCallbackRecordId(callbackData);
        if (recordId == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.APPROVAL_RECORD_NOT_FOUND);
        }
        AgentRecord agentRecord = agentRecordMapper.selectById(recordId);
        if (agentRecord == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.RECORD_NOT_FOUND);
        }
        AgentQueryApprove approveRecord =
            agentQueryApproveService.getByRecordId(String.valueOf(recordId));
        if (approveRecord != null) {
            return new CallbackApproveRecord(approveRecord, false);
        }
        // 兼容第三方跳过建单回调、直接回调审批状态的情况。
        AgentQueryApprove created = new AgentQueryApprove();
        created.setRecordId(String.valueOf(recordId));
        if (fillCallbackData) {
            fillApproveRecordFromCallback(created, callbackData, true);
        } else {
            created.setApproveState(APPROVAL_STATUS_PENDING_APPROVAL);
        }
        agentQueryApproveService.create(created);
        return new CallbackApproveRecord(created, true);
    }

    private Long resolveCallbackRecordId(ApprovalCallbackData callbackData) {
        if (callbackData == null) {
            return null;
        }
        return callbackData.getRecordId();
    }

    private AgentConfig findConfig(Long agentConfigId) {
        if (agentConfigId != null) {
            return aiAgentConfigService.getById(agentConfigId);
        } else {
            return aiAgentConfigService.getOneByPriority();
        }
    }

    /**
     * 上报"智能体提问"操作日志
     * <p>
     * 受 ai_globals.SAVE_IMEXTRA 开关控制：
     * <ul>
     *   <li>SAVE_IMEXTRA=true（忽略大小写）：上报；</li>
     *   <li>为 null / 空串 / false / 不存在 / 读取异常：不上报。</li>
     * </ul>
     */
    private void reportImExtraAskLog(AskAgentCO askAgentCO, AgentConfig config) {
        if (!shouldSaveImExtra()) {
            log.info("skip reportImExtraAskLog, SAVE_IMEXTRA disabled");
            return;
        }
        try {
            OperationLog operationLog = new OperationLog(OperationTypeEnum.AGENT_IM_ASK);
            String operation = String.format(operationLog.getOperation(),
                askAgentCO.getUserName() == null ? "" : askAgentCO.getUserName(),
                config.getName() == null ? "" : config.getName(),
                    askAgentCO.getImExtra() == null ? "" : JSONObject.toJSONString(askAgentCO.getImExtra()));
            operationLog.setOperation(operation);
            reportUtil.saveOperationLog(operationLog);
        } catch (Exception e) {
            log.error("reportImExtraAskLog error, userName:{}, agentName:{}",
                askAgentCO.getUserName(), config.getName(), e);
        }
    }

    /**
     * 读取 ai_globals.SAVE_IMEXTRA 配置，仅 {@code "true"}（忽略大小写）时返回 true
     */
    private boolean shouldSaveImExtra() {
        String saveImextra = globalsService.findByName("SAVE_IMEXTRA");
        return "true".equalsIgnoreCase(saveImextra);
    }

    private void createRecord(Long id, AskAgentCO askAgentCO, AgentConfig config, boolean approvalRequired, String approvalSubMode) {
        try {
            var agentRecord = new AgentRecord();
            agentRecord.setId(id);
            agentRecord.setAgentConfigId(config.getId());
            agentRecord.setAgentName(config.getName());
            agentRecord.setIdentityCardNumber(askAgentCO.getUserID());
            agentRecord.setUserName(askAgentCO.getUserName());
            agentRecord.setQueryContent(askAgentCO.getContent());
            agentRecord.setTime(LocalDateTime.now());
            agentRecord.setDepartmentId(askAgentCO.getDepartmentId());
            agentRecord.setDepartmentCode(askAgentCO.getDepartmentCode());
            agentRecord.setDepartmentName(askAgentCO.getDepartmentName());
            if (StringUtils.isNotBlank(askAgentCO.getSessionId())) {
                AiFileInfo fileInfo = aiAgentFileService.getFileInfo(askAgentCO.getSessionId());
                if (fileInfo != null) {
                    agentRecord.setAttachement(fileInfo.getFileId());
                    agentRecord.setAttachementPath(fileInfo.getFilePath());
                }
            }
            agentRecord.setApprovalEnabled(approvalRequired ? 1 : 0);
            agentRecord.setApprovalSubMode(approvalRequired ? approvalSubMode : null);
            agentRecord.setAskType(askAgentCO.getAskType());
            agentRecord.setImSessionId(askAgentCO.getImSessionId());
            agentRecordService.addRecord(agentRecord);
        } catch (Exception e) {
            log.error(
                    "save record error, id:{}, askAgentCO: {}, config: {}",
                    id, JsonUtils.toJsonString(askAgentCO), JsonUtils.toJsonString(config), e
            );
            throw new IllegalStateException("add record error", e);
        }
    }

    private boolean shouldRequireApproval(AiSettingsVO settings, AgentConfig config) {
        return settings != null
            && config != null
            && Objects.equals(settings.getApprovalEnabled(), 1)
            && StringUtils.isNotBlank(settings.getApprovalSystemUrl());
    }

    private String resolveApprovalSubMode(AiSettingsVO settings) {
        return StringUtils.defaultIfBlank(settings.getApprovalSubMode(),
            AiAgentGlobalConstants.APPROVAL_SUB_MODE_ASK_BEFORE_APPROVE);
    }

    private String buildApprovalCreateUrl(String approvalSystemUrl, Long recordId) {
        HttpUrl httpUrl = HttpUrl.parse(approvalSystemUrl);
        if (httpUrl != null) {
            // 第三方 H5 建单页必须拿到本系统对话 ID，后续两个回调也以该 ID 关联本地记录。
            return httpUrl.newBuilder()
                .setQueryParameter("recordId", String.valueOf(recordId))
                .setQueryParameter("conversationId", String.valueOf(recordId))
                .build()
                .toString();
        }
        String separator = approvalSystemUrl.contains("?") ? "&" : "?";
        return approvalSystemUrl + separator + "recordId=" + recordId + "&conversationId=" + recordId;
    }

    private void updateApproveCreatedRecord(AgentQueryApprove approveRecord, ApprovalCallbackData callbackData) {
        approveRecord.setApproveNo(firstNonBlank(callbackData.getApproveNo(), approveRecord.getApproveNo()));
        approveRecord.setApproveDetailUrl(firstNonBlank(callbackData.getApproveDetailUrl(),
            approveRecord.getApproveDetailUrl()));
        approveRecord.setToLeaderUrl(firstNonBlank(callbackData.getToLeaderUrl(), approveRecord.getToLeaderUrl()));
        approveRecord.setApproveState(normalizeApprovalStatus(
            firstNonBlank(callbackData.getApprovalStatus(), approveRecord.getApproveState()), null));
        approveRecord.setApproveDescription(firstNonBlank(callbackData.getApproveDescription(),
            approveRecord.getApproveDescription()));
        approveRecord.setApproveTime(firstNonBlank(callbackData.getApproveTime(), approveRecord.getApproveTime()));
        approveRecord.setApproveUser(firstNonBlank(callbackData.getApproveUser(), approveRecord.getApproveUser()));
        agentQueryApproveService.updateById(approveRecord);
    }

    private boolean updateApproveRecord(AgentQueryApprove approveRecord, ApprovalCallbackData callbackData) {
        AgentQueryApprove updateRecord = copyApproveRecord(approveRecord);
        fillApproveRecordFromCallback(updateRecord, callbackData, true);
        if (Objects.equals(updateRecord.getApproveState(), APPROVAL_STATUS_FINISHED)) {
            int updated = agentQueryApproveMapper.update(null, Wrappers.lambdaUpdate(AgentQueryApprove.class)
                .eq(AgentQueryApprove::getId, approveRecord.getId())
                .and(wrapper -> wrapper.isNull(AgentQueryApprove::getApproveState)
                    .or().ne(AgentQueryApprove::getApproveState, APPROVAL_STATUS_FINISHED))
                .set(AgentQueryApprove::getApproveNo, updateRecord.getApproveNo())
                .set(AgentQueryApprove::getApproveUrl, updateRecord.getApproveUrl())
                .set(AgentQueryApprove::getApproveDetailUrl, updateRecord.getApproveDetailUrl())
                .set(AgentQueryApprove::getToLeaderUrl, updateRecord.getToLeaderUrl())
                .set(AgentQueryApprove::getApproveState, updateRecord.getApproveState())
                .set(AgentQueryApprove::getApprove, updateRecord.getApprove())
                .set(AgentQueryApprove::getApproveDescription, updateRecord.getApproveDescription())
                .set(AgentQueryApprove::getApproveTime, updateRecord.getApproveTime())
                .set(AgentQueryApprove::getApproveUser, updateRecord.getApproveUser()));
            if (updated > 0) {
                copyApproveRecordValues(updateRecord, approveRecord);
                return true;
            }
            return false;
        }
        agentQueryApproveService.updateById(updateRecord);
        copyApproveRecordValues(updateRecord, approveRecord);
        return false;
    }

    private void fillApproveRecordFromCallback(AgentQueryApprove approveRecord, ApprovalCallbackData callbackData,
        boolean keepExistingValue) {
        approveRecord.setApproveNo(keepExistingValue
            ? firstNonBlank(callbackData.getApproveNo(), approveRecord.getApproveNo()) : callbackData.getApproveNo());
        approveRecord.setApproveDetailUrl(keepExistingValue
            ? firstNonBlank(callbackData.getApproveDetailUrl(), approveRecord.getApproveDetailUrl())
            : callbackData.getApproveDetailUrl());
        approveRecord.setToLeaderUrl(keepExistingValue
            ? firstNonBlank(callbackData.getToLeaderUrl(), approveRecord.getToLeaderUrl())
            : callbackData.getToLeaderUrl());
        Integer approveResult = callbackData.getApproveResult();
        approveRecord.setApproveState(normalizeApprovalStatus(
            firstNonBlank(callbackData.getApprovalStatus(), keepExistingValue ? approveRecord.getApproveState() : null),
            approveResult));
        approveRecord.setApprove(approveResult);
        approveRecord.setApproveDescription(keepExistingValue
            ? firstNonBlank(callbackData.getApproveDescription(), approveRecord.getApproveDescription())
            : callbackData.getApproveDescription());
        approveRecord.setApproveTime(keepExistingValue
            ? firstNonBlank(callbackData.getApproveTime(), approveRecord.getApproveTime()) : callbackData.getApproveTime());
        approveRecord.setApproveUser(keepExistingValue
            ? firstNonBlank(callbackData.getApproveUser(), approveRecord.getApproveUser()) : callbackData.getApproveUser());
    }

    private AgentQueryApprove copyApproveRecord(AgentQueryApprove source) {
        AgentQueryApprove target = new AgentQueryApprove();
        copyApproveRecordValues(source, target);
        return target;
    }

    private void copyApproveRecordValues(AgentQueryApprove source, AgentQueryApprove target) {
        target.setId(source.getId());
        target.setRecordId(source.getRecordId());
        target.setApproveNo(source.getApproveNo());
        target.setApproveUrl(source.getApproveUrl());
        target.setApproveDetailUrl(source.getApproveDetailUrl());
        target.setToLeaderUrl(source.getToLeaderUrl());
        target.setApproveState(source.getApproveState());
        target.setApprove(source.getApprove());
        target.setApproveDescription(source.getApproveDescription());
        target.setApproveTime(source.getApproveTime());
        target.setApproveUser(source.getApproveUser());
    }

    private void submitAskTask(AgentConfig config, Long id, RequestInput input, ApprovalCache approvalCache)
        throws ScriptException {
        // 防止重复提交
        if (hasFinalResponse(id)) {
            log.info("ask already submitted, recordId:{}", id);
            return;
        }
        // 构造agent请求的相关配置
        AskRequestDefinition requestDefinition = buildRequestDefinition(config, input);
        executorService.execute(() -> {
            try {
                // 发送请求
                doAsk(config, id, requestDefinition);
                reportUtil.clearAlarm2MSIP(AlarmTemplateZhEnum.INVOKING_THE_AI_AGENT_FAILED);
            } catch (Exception e) {
                log.error("id:{} error", id, e);
                putCacheResult(id, "[ERROR]");
                saveContentAndUpdateAnswerTime(id, "[ERROR]");
                reportUtil.saveAlarm2MSIP(AlarmTemplateZhEnum.INVOKING_THE_AI_AGENT_FAILED, input.getUserName(),
                    input.getContent());
            }
        });
    }

    private void doAsk(AgentConfig agentConfig, Long id, AskRequestDefinition requestDefinition)
        throws IOException, ScriptException {
        log.info("config:{}", agentConfig);
        Request request = buildHttpRequest(agentConfig, requestDefinition);
        log.info(" id:{} req:{} param:{}", id, request, requestDefinition);
        Integer endFlag = requestDefinition.getEndFlag() != null ? requestDefinition.getEndFlag() : 1;
        StringBuilder sb;
        try (Response response = client.newCall(request).execute()) {
            updateAnswerTime(id);
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            var responseBody = response.body();
            if (responseBody == null) {
                throw new IOException("response empty " + response);
            }
            BufferedSource source = responseBody.source();
            sb = new StringBuilder();
            if (Integer.valueOf(0).equals(endFlag)) {
                doAskWithoutEndFlag(agentConfig, id, source, sb);
            } else {
                doAskWithEndFlag(agentConfig, id, source, sb);
            }
        }
        // 保存最终结果到DB
        saveContent(id);
    }

    private void saveContent(Long id) {
        String content = stringRedisTemplate.opsForValue().get(REDIS_CACHE_KEY_REFIX + id);
        if (StringUtils.isNotBlank(content)) {
            AgentRecord updateRecord = new AgentRecord();
            updateRecord.setId(id);
            updateRecord.setResponseContent(content);
            agentRecordMapper.updateById(updateRecord);
        } else {
            log.warn("Redis cache not found for recordId:{}, skip update", id);
        }

    }

    private void doAskWithEndFlag(AgentConfig agentConfig, Long id, BufferedSource source, StringBuilder sb)
        throws IOException, ScriptException {
        while (!source.exhausted()) {
            String line = source.readUtf8Line();
            if (line == null) {
                continue;
            }
            log.info("id:{} line:{}", id, line);
            var data = explainXAData(sb, agentConfig, line);
            if (data == null || data.getEvent() == null) {
                continue;
            }
            switch (data.getEvent()) {
                case "message":
                    sb.append(data.getAnswer());
                    break;
                case "message_end":
                    if ("succeeded".equals(data.getStatus())) {
                        sb.append("[end]");
                    } else {
                        // log日志, 添加[ERROR]标识，避免前端一直查询
                        log.error("id message_end:{} error:{}", id, data.error);
                        sb.append("[ERROR]");
                        putCacheResult(id, sb.toString());
                        markFinished(id);
//                        throw new RuntimeException(data.error);
                        return;
                    }
                    break;
                default:
                    log.warn("unknown event type:{}", data);
                    continue;
            }
            log.trace("id:{} result:{}", id, sb);
            putCacheResult(id, sb.toString());
            if ("message_end".equals(data.getEvent()) && "succeeded".equals(data.getStatus())) {
                // 判断如果暂停了，就不更新暂停位置
                markFinished(id);
            }
        }
    }

    private void doAskWithoutEndFlag(AgentConfig agentConfig, Long id, BufferedSource source, StringBuilder sb)
        throws IOException, ScriptException {
        while (!source.exhausted()) {
            String line = source.readUtf8Line();
            if (line == null) {
                continue;
            }
            log.info("id:{} line:{}", id, line);
            var data = explainXAData(sb, agentConfig, line);
            if (data == null || data.getEvent() == null) {
                continue;
            }
            switch (data.getEvent()) {
                case "message":
                    sb.append(data.getAnswer());
                    break;
                case "error":
                    sb.append(data.getError()).append("[ERROR]");
                    putCacheResult(id, sb.toString());
                    markFinished(id);
                    return;
                default:
                    log.warn("unknown event type:{}", data);
                    continue;
            }
            log.trace("id:{} result:{}", id, sb);
            putCacheResult(id, sb.toString());
        }
        sb.append("[end]");
        putCacheResult(id, sb.toString());
        markFinished(id);
    }

    private void markFinished(Long id) {
        AgentRecord agentRecord = agentRecordMapper.selectById(id);
        log.info("id:{} record:{}", id, agentRecord);
        replyStateService.putReplyState(id, -1);
    }


    private void saveContentAndUpdateAnswerTime(Long id, String content) {
        try {
            AgentRecord updateRecord = new AgentRecord();
            updateRecord.setId(id);
            updateRecord.setResponseContent(content);
            updateRecord.setAnswerTime(LocalDateTime.now());
            agentRecordMapper.updateById(updateRecord);
        } catch (Exception e) {
            log.error("saveContentAndUpdateAnswerTime error, id:{}", id, e);
        }
    }

    private void updateAnswerTime(Long id) {
        try {
            AgentRecord updateRecord = new AgentRecord();
            updateRecord.setId(id);
            updateRecord.setAnswerTime(LocalDateTime.now());
            agentRecordMapper.updateById(updateRecord);
        } catch (Exception e) {
            log.error("update answer_time error, id:{}", id, e);
        }
    }

    private XAMessage explainXAData(StringBuilder history, AgentConfig conf, String line) throws ScriptException {
        if (conf.getRespScript() == null || conf.getRespScript().isBlank()) {
            if (!line.startsWith("data:")) {
                return null;
            }
            var json = line.substring("data:".length()).strip();
            var xaMessage = JSON.parseObject(json, XAMessage.class);
            xaMessage.setStatus("succeeded");
            return xaMessage;
        } else {
            var data = jsEngine.runJs(conf.getId() + JsEngine.RESPONSE_SUFFIX, line,
                JsonUtils.toJsonString(Map.of("context", conf, "history", history.toString())));
            if (data == null) {
                return null;
            }
            return JsonUtils.convert(data, XAMessage.class);
        }
    }

    public void putCacheResult(Long id, String value) {
        stringRedisTemplate.opsForValue().set(REDIS_CACHE_KEY_REFIX + id, value, Duration.ofMinutes(30L));
        replyStateService.putReplyState(id, value == null ? 0 : value.length());
    }
    @Override
    public String getCacheResult(Long id) {
        return stringRedisTemplate.opsForValue().get(REDIS_CACHE_KEY_REFIX + id);
    }

    @Override
    public Boolean updateApprovalStatus(ApproveStatusUpdateCO co) {
        Long agentId = co.getResolvedAgentId();
        List<AgentRecord> records = agentRecordMapper.selectList(Wrappers.lambdaQuery(AgentRecord.class)
                .eq(AgentRecord::getIdentityCardNumber, co.getUserID())
                .eq(Objects.nonNull(agentId), AgentRecord::getAgentConfigId, agentId)
                .orderByDesc(AgentRecord::getId));
        if (records.isEmpty()) {
            log.info("No records found for userID:{} and agentId:{}", co.getUserID(), agentId);
            return false;
        }
        //由于可能存在关联的审批记录还未建单，所以这里更新除审批通过之外的所有单的sessionId
        List<String> approveRecordIds = records.stream().map(r -> String.valueOf(r.getId())).toList();
        List<AgentQueryApprove> pendingApproves = agentQueryApproveMapper.selectList(
                Wrappers.lambdaQuery(AgentQueryApprove.class)
                        .eq(AgentQueryApprove::getApproveState, APPROVAL_STATUS_FINISHED)
                        .in(AgentQueryApprove::getRecordId, approveRecordIds));
        if (!pendingApproves.isEmpty()) {
            Set<String> finishedRecordIds = pendingApproves.stream().map(AgentQueryApprove::getRecordId).collect(Collectors.toSet());
            records.stream().filter(r -> !finishedRecordIds.contains(String.valueOf(r.getId())))
                    .forEach(r -> {
                        String pushTargetKey = REDIS_APPROVAL_PUSH_TARGET_KEY_PREFIX + r.getId();
                        if (stringRedisTemplate.hasKey(pushTargetKey)) {
                            webSocketPushService.putPushTarget(r.getId(), co.getWsSessionId());
                        }
            });

        }
        return true;
    }

    private void putApprovalCache(ApprovalCache approvalCache) {
        if (approvalCache == null || approvalCache.getRecordId() == null) {
            return;
        }
        stringRedisTemplate.opsForValue().set(REDIS_APPROVAL_STATUS_KEY_PREFIX + approvalCache.getRecordId(),
            JsonUtils.toJsonString(approvalCache), Duration.ofMinutes(60L));
    }

    private ApprovalCache getApprovalCache(Long id) {
        String cacheValue = stringRedisTemplate.opsForValue().get(REDIS_APPROVAL_STATUS_KEY_PREFIX + id);
        if (StringUtils.isBlank(cacheValue)) {
            return null;
        }
        return JSON.parseObject(cacheValue, ApprovalCache.class);
    }

    private String normalizeApprovalStatus(String approvalStatus, Integer approveResult) {
        if (StringUtils.isBlank(approvalStatus)) {
            return approveResult == null ? APPROVAL_STATUS_PENDING_APPROVAL : APPROVAL_STATUS_FINISHED;
        }
        String normalized = approvalStatus.trim();
        if (Objects.equals(normalized, APPROVAL_STATUS_PENDING_APPROVAL)) {
            return normalized;
        }
        if (Objects.equals(normalized, APPROVAL_STATUS_WAIT_CREATE)) {
            return normalized;
        }
        if (Objects.equals(normalized, APPROVAL_STATUS_FINISHED)) {
            return approveResult == null ? APPROVAL_STATUS_PENDING_APPROVAL : APPROVAL_STATUS_FINISHED;
        }
        String lowerCaseState = normalized.toLowerCase();
        if (lowerCaseState.contains("finish") || lowerCaseState.contains("complete")
            || lowerCaseState.contains("done")) {
            return approveResult == null ? APPROVAL_STATUS_PENDING_APPROVAL : APPROVAL_STATUS_FINISHED;
        }
        if (lowerCaseState.contains("pending") || lowerCaseState.contains("processing")
            || lowerCaseState.contains("approving") || lowerCaseState.contains("review")) {
            return APPROVAL_STATUS_PENDING_APPROVAL;
        }
        if (approveResult != null) {
            return APPROVAL_STATUS_FINISHED;
        }
        return APPROVAL_STATUS_PENDING_APPROVAL;
    }

    private String resolveReply(Long id, String cachedReply, ApprovalCache approvalCache) {
        if (canReturnReplyDirectly(approvalCache)) {
            if (cachedReply != null) {
                return cachedReply;
            }
            AgentRecord agentRecord = agentRecordMapper.selectById(id);
            String dbReply =
                agentRecord == null || agentRecord.getResponseContent() == null ? "" : agentRecord.getResponseContent();
            if (StringUtils.isNotBlank(dbReply)) {
                // DB 已落最终回答，回填缓存时同步标记为完成。
                stringRedisTemplate.opsForValue().set(REDIS_CACHE_KEY_REFIX + id, dbReply, Duration.ofMinutes(30L));
                replyStateService.putReplyState(id, -1);
            }
            return dbReply;
        }
        return "";
    }

    private boolean canReturnReplyDirectly(ApprovalCache approvalCache) {
        if (approvalCache == null || !Boolean.TRUE.equals(approvalCache.getApprovalRequired())
            || Objects.equals(approvalCache.getApprovalStatus(), APPROVAL_STATUS_NO_APPROVAL)) {
            return true;
        }
        if (isAskBeforeApprove(approvalCache.getApprovalSubMode())) {
            return true;
        }
        return Objects.equals(approvalCache.getApprovalStatus(), APPROVAL_STATUS_FINISHED)
            && Objects.equals(approvalCache.getApproveResult(), 0);
    }

    private boolean shouldSubmitAskImmediately(ApprovalCache approvalCache) {
        return approvalCache == null
            || !Boolean.TRUE.equals(approvalCache.getApprovalRequired())
            || isAskBeforeApprove(approvalCache.getApprovalSubMode());
    }

    private ApprovalCache loadApprovalCache(Long id) {
        AgentQueryApprove approveRecord = agentQueryApproveMapper.selectOne(Wrappers.lambdaQuery(AgentQueryApprove.class)
            .eq(AgentQueryApprove::getRecordId, String.valueOf(id))
            .last("limit 1"));
        Map<String, String> settingsMap = getReplySettingsMap();
        ApprovalCache approvalCache = new ApprovalCache();
        approvalCache.setRecordId(id);
        approvalCache.setApprovalSubMode(StringUtils.defaultIfBlank(settingsMap.get(AiAgentGlobalConstants.APPROVAL_SUB_MODE),
            AiAgentGlobalConstants.APPROVAL_SUB_MODE_ASK_BEFORE_APPROVE));
        if (approveRecord == null) {
            fillCacheWithoutApproveRecord(id, approvalCache);
            return approvalCache;
        }
        approvalCache.setApprovalRequired(Boolean.TRUE);
        mergeApprovalCache(approvalCache, approveRecord);
        return approvalCache;
    }

    private void fillCacheWithoutApproveRecord(Long id, ApprovalCache approvalCache) {
        AgentRecord agentRecord = agentRecordMapper.selectById(id);
        AgentConfig config = agentRecord == null ? null : aiAgentConfigService.getById(agentRecord.getAgentConfigId());
        AiSettingsVO settings = globalsService.getAiSettings();
        if (shouldRequireApproval(settings, config)) {
            approvalCache.setApprovalRequired(Boolean.TRUE);
            approvalCache.setApprovalStatus(APPROVAL_STATUS_WAIT_CREATE);
            approvalCache.setApproveUrl(buildApprovalCreateUrl(settings.getApprovalSystemUrl(), id));
            return;
        }
        approvalCache.setApprovalRequired(Boolean.FALSE);
        approvalCache.setApprovalStatus(APPROVAL_STATUS_NO_APPROVAL);
    }

    private Map<String, String> getReplySettingsMap() {
        List<Globals> settings = globalsMapper.selectList(Wrappers.lambdaQuery(Globals.class)
            .in(Globals::getName, Arrays.asList(AiAgentGlobalConstants.APPROVAL_SUB_MODE)));
        Map<String, String> settingsMap = new LinkedHashMap<>();
        settings.forEach(item -> settingsMap.put(item.getName(), item.getValue()));
        return settingsMap;
    }

    private ApprovalCache buildInitialApprovalCache(Long recordId, boolean approvalRequired, String approvalSubMode) {
        ApprovalCache approvalCache = new ApprovalCache();
        approvalCache.setRecordId(recordId);
        approvalCache.setApprovalRequired(approvalRequired);
        approvalCache.setApprovalSubMode(approvalSubMode);
        approvalCache.setApprovalStatus(approvalRequired ? APPROVAL_STATUS_PENDING_APPROVAL : APPROVAL_STATUS_NO_APPROVAL);
        return approvalCache;
    }

    private void mergeApprovalCache(ApprovalCache approvalCache, AgentQueryApprove approveRecord) {
        if (approvalCache == null || approveRecord == null) {
            return;
        }
        approvalCache.setApprovalRequired(Boolean.TRUE);
        approvalCache.setApprovalStatus(normalizeApprovalStatus(approveRecord.getApproveState(), approveRecord.getApprove()));
        approvalCache.setApproveResult(approveRecord.getApprove());
        approvalCache.setApproveNo(approveRecord.getApproveNo());
        approvalCache.setApproveUrl(approveRecord.getApproveUrl());
        approvalCache.setApproveDetailUrl(approveRecord.getApproveDetailUrl());
        approvalCache.setToLeaderUrl(approveRecord.getToLeaderUrl());
    }

    private boolean shouldSubmitAskAfterApproval(ApprovalCache approvalCache, AgentQueryApprove approveRecord,
        boolean firstFinishedCallback) {
        return approvalCache != null
            && firstFinishedCallback
            && !hasFinalResponse(approvalCache.getRecordId())
            && !isAskBeforeApprove(approvalCache.getApprovalSubMode())
            && Objects.equals(normalizeApprovalStatus(approveRecord.getApproveState(), approveRecord.getApprove()),
                APPROVAL_STATUS_FINISHED)
            && Objects.equals(approveRecord.getApprove(), 0);
    }

    private boolean hasFinalResponse(Long recordId) {
        if (recordId == null) {
            return false;
        }
        AgentRecord agentRecord = agentRecordMapper.selectById(recordId);
        return agentRecord != null && StringUtils.isNotBlank(agentRecord.getResponseContent());
    }

    private boolean isAskBeforeApprove(String approvalSubMode) {
        return Objects.equals(StringUtils.defaultIfBlank(approvalSubMode,
            AiAgentGlobalConstants.APPROVAL_SUB_MODE_ASK_BEFORE_APPROVE),
            AiAgentGlobalConstants.APPROVAL_SUB_MODE_ASK_BEFORE_APPROVE);
    }

    private Request buildHttpRequest(AgentConfig conf, AskRequestDefinition requestDefinition) {
        Request.Builder builder = new Request.Builder().url(buildRequestUrl(conf, requestDefinition.getQuery()));
        requestDefinition.getHeaders().forEach(builder::header);
        if (StringUtils.isNotBlank(conf.getToken()) && !requestDefinition.getHeaders().containsKey("Authorization")) {
            builder.header("Authorization", "Bearer " + conf.getToken());
        }
        String method = StringUtils.defaultIfBlank(requestDefinition.getHttpMethod(), conf.getHttpMethod());
        if (StringUtils.isBlank(method)) {
            method = "POST";
        }
        String normalizedMethod = method.toUpperCase();
        RequestBody requestBody = null;
        Integer bodyType = requestDefinition.getBodyType() != null ? requestDefinition.getBodyType() : BodyTypeEnum.RAW_JSON.getType();
        if (!requiresEmptyBody(normalizedMethod)) {
            if (BodyTypeEnum.FORM_DATA.getType().equals(bodyType)) {
                MultipartBody.Builder multipartBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);
                requestDefinition.getFormDataParts().forEach(multipartBuilder::addFormDataPart);
                requestBody = multipartBuilder.build();
            } else if (BodyTypeEnum.RAW_TEXT.getType().equals(bodyType)) {
                requestBody = RequestBody.create(
                    StringUtils.defaultString(requestDefinition.getBody()).getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    MediaType.parse("text/plain; charset=utf-8"));
            } else {
                String contentType = requestDefinition.getHeaders().getOrDefault("Content-Type", "application/json");
                requestBody = RequestBody.create(
                    StringUtils.defaultString(requestDefinition.getBody()).getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    MediaType.parse(contentType));
            }
        }
        return builder.method(normalizedMethod, requestBody).build();
    }

    private String buildRequestUrl(AgentConfig conf, Map<String, Object> queryMap) {
        if (queryMap == null || queryMap.isEmpty()) {
            return conf.getUrl();
        }
        HttpUrl httpUrl = HttpUrl.parse(conf.getUrl());
        if (httpUrl == null) {
            throw new IllegalArgumentException("invalid request url: " + conf.getUrl());
        }
        HttpUrl.Builder builder = httpUrl.newBuilder();
        queryMap.forEach((key, value) -> appendQueryParam(builder, key, value));
        return builder.build().toString();
    }

    private void appendQueryParam(HttpUrl.Builder builder, String key, Object value) {
        if (StringUtils.isBlank(key) || value == null) {
            return;
        }
        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                if (item != null) {
                    builder.addQueryParameter(key, String.valueOf(item));
                }
            }
            return;
        }
        builder.addQueryParameter(key, String.valueOf(value));
    }

    private boolean requiresEmptyBody(String method) {
        return "GET".equals(method) || "HEAD".equals(method) || "DELETE".equals(method);
    }

    private String buildXAParam(AgentConfig conf, String value) throws ScriptException {
        if (conf.getParamScript() == null || conf.getParamScript().isBlank()) {
            return JsonUtils.toJsonString(
                Map.of("inputs", Collections.emptyMap(), "query", value, "response_mode", "streaming",
                    "conversation_id", "", "user", "abc-123"));
        } else {
            var data = jsEngine.runJs(conf.getId() + JsEngine.PARAM_SUFFIX, value, JsonUtils.toJsonString(conf));
            if (data == null) {
                throw new ScriptException("script run error");
            }
            return JsonUtils.toJsonString(data);
        }
    }

    private AskRequestDefinition buildRequestDefinition(AgentConfig conf, RequestInput input) throws ScriptException {
        if (useLegacyRequest(conf)) {
            return AskRequestDefinition.legacy(conf.getHttpMethod(), buildXAParam(conf, input.getContent()));
        }
        AskRequestDefinition definition = requestParamResolver.resolve(conf, input);
        if (StringUtils.isNotBlank(conf.getParamScript())) {
            Object data =
                jsEngine.runJs(conf.getId() + JsEngine.PARAM_SUFFIX, input.getContent(), JsonUtils.toJsonString(conf));
            if (data == null) {
                throw new ScriptException("script run error");
            }
            requestParamResolver.mergeScriptResult(definition, data);
        }
        return definition;
    }

    private boolean useLegacyRequest(AgentConfig conf) {
        return StringUtils.isNotBlank(conf.getToken())
            && StringUtils.isBlank(conf.getHttpMethod())
            && StringUtils.isBlank(conf.getHeader())
            && StringUtils.isBlank(conf.getQuery())
            && StringUtils.isBlank(conf.getBody());
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    @Getter
    @Setter
    private static class ApprovalCallbackData {

        private Long recordId;
        private String approveNo;
        private String approveDetailUrl;
        private String toLeaderUrl;
        private String approvalStatus;
        private Integer approveResult;
        private String approveDescription;
        private String approveTime;
        private String approveUser;

        private static ApprovalCallbackData from(ApproveCreatedCallbackCO callbackCO) {
            ApprovalCallbackData data = new ApprovalCallbackData();
            data.setRecordId(callbackCO.getRecordId());
            data.setApproveNo(callbackCO.getApproveNo());
            data.setApproveDetailUrl(callbackCO.getApproveDetailUrl());
            data.setToLeaderUrl(callbackCO.getToLeaderUrl());
            data.setApprovalStatus(APPROVAL_STATUS_PENDING_APPROVAL);
            data.setApproveUser(callbackCO.getApproveUser());
            return data;
        }

        private static ApprovalCallbackData from(ApproveStatusCallbackCO callbackCO) {
            ApprovalCallbackData data = new ApprovalCallbackData();
            data.setRecordId(callbackCO.getRecordId());
            data.setApproveNo(callbackCO.getApproveNo());
            data.setApproveDetailUrl(callbackCO.getApproveDetailUrl());
            data.setToLeaderUrl(callbackCO.getToLeaderUrl());
            data.setApprovalStatus(callbackCO.getApprovalStatus());
            data.setApproveResult(callbackCO.getApproveResult());
            data.setApproveDescription(callbackCO.getApproveDescription());
            data.setApproveTime(callbackCO.getApproveTime());
            data.setApproveUser(callbackCO.getApproveUser());
            return data;
        }
    }

    @Getter
    private static class CallbackApproveRecord {

        private final AgentQueryApprove approveRecord;
        private final boolean created;

        private CallbackApproveRecord(AgentQueryApprove approveRecord, boolean created) {
            this.approveRecord = approveRecord;
            this.created = created;
        }
    }

    @Getter
    @Setter
    @ToString
    private static class ApprovalCache {

        private Long recordId;
        private Boolean approvalRequired;
        private String approvalStatus;
        private Integer approveResult;
        private String approveNo;
        private String approveUrl;
        private String approveDetailUrl;
        private String toLeaderUrl;
        private String approvalSubMode;
    }

    @Getter
    @Setter
    @ToString
    public static class XAMessage {

        private String event;
        private String answer;
        private String status;
        private String error;

    }

}