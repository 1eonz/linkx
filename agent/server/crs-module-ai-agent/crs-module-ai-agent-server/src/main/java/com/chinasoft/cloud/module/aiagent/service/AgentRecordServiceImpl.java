package com.chinasoft.cloud.module.aiagent.service;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chinasoft.cloud.framework.common.pojo.PageResult;
import com.chinasoft.cloud.framework.common.util.collection.CollectionUtils;
import com.chinasoft.cloud.framework.utils.IdWorker;
import com.chinasoft.cloud.module.aiagent.controller.admin.vo.AiSettingsVO;
import com.chinasoft.cloud.module.aiagent.controller.admin.qo.AgentRecordCursorQO;
import com.chinasoft.cloud.module.aiagent.controller.admin.qo.AgentRecordQO;
import com.chinasoft.cloud.module.aiagent.controller.admin.qo.RecordCountQO;
import com.chinasoft.cloud.module.aiagent.controller.admin.qo.RecordQO;
import com.chinasoft.cloud.module.aiagent.controller.admin.vo.AgentRecordCursorVO;
import com.chinasoft.cloud.module.aiagent.controller.admin.vo.RecordCountVO;
import com.chinasoft.cloud.module.aiagent.controller.app.qo.UserAgentHistoryQO;
import com.chinasoft.cloud.module.aiagent.controller.app.vo.UserAgentHistoryRecordVO;
import com.chinasoft.cloud.module.aiagent.controller.app.vo.UserAgentRecordVO;
import com.chinasoft.cloud.module.aiagent.dal.dataobj.AgentConfig;
import com.chinasoft.cloud.module.aiagent.dal.dataobj.AgentQueryApprove;
import com.chinasoft.cloud.module.aiagent.dal.dataobj.AgentRecord;
import com.chinasoft.cloud.module.aiagent.dal.dataobj.AgentRecordAskType;
import com.chinasoft.cloud.module.aiagent.dal.dataobj.Category;
import com.chinasoft.cloud.module.aiagent.dal.mysql.AgentConfigMapper;
import com.chinasoft.cloud.module.aiagent.dal.mysql.AgentQueryApproveMapper;
import com.chinasoft.cloud.module.aiagent.dal.mysql.AgentRecordMapper;
import com.chinasoft.cloud.module.aiagent.dal.mysql.CategoryMapper;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@Validated
public class AgentRecordServiceImpl implements AgentRecordService {
    private static final ZoneId zoneId = ZoneId.of("Asia/Shanghai");
    private static final String datePattern = "yyyy-MM-dd";
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String APPROVAL_STATUS_NO_APPROVAL = "0";
    private static final String APPROVAL_STATUS_PENDING_APPROVAL = "1";
    private static final String APPROVAL_STATUS_WAIT_CREATE = "3";
    @Resource
    private AgentRecordMapper agentRecordMapper;
    @Resource
    private AgentQueryApproveMapper agentQueryApproveMapper;
    @Resource
    private CategoryMapper categoryMapper;
    @Resource
    private AgentConfigMapper agentConfigMapper;
    @Resource
    private IdWorker idWorker;
    @Resource
    private AiAgentReplyStateService replyStateService;
    @Resource
    private GlobalsService globalsService;
    @Resource
    @Lazy
    private AiAgentAskService askService;

    private LocalDate parseLocalDate(String dateString) {
        if (dateString == null || dateString.isBlank()) {
            throw new IllegalArgumentException("日期字符串不能为null或空");
        }
        // 使用上海时区解析日期字符串

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(datePattern).withZone(zoneId);
        return LocalDate.parse(dateString, formatter);
    }

    @Override
    public void addRecord(AgentRecord agentrecord) {
        if (agentrecord.getId() == null) {
            agentrecord.setId(idWorker.nextId());
        }
        agentRecordMapper.insert(agentrecord);
    }

    @Override
    public List<RecordCountVO> countRecord(RecordCountQO recordCountQO) {
        var startDate = parseLocalDate(recordCountQO.getStartTime());
        var endDate = parseLocalDate(recordCountQO.getEndTime());
        var agentConfigs = agentConfigMapper.selectList();
        var queryWrapper = new MPJLambdaWrapper<>(AgentRecord.class)//
            .in(AgentRecord::getAskType, AgentRecordAskType.getValidValues())
            .like(recordCountQO.getPersonName() != null && !recordCountQO.getPersonName().isBlank(),
                AgentRecord::getUserName, recordCountQO.getPersonName())//
            .in(recordCountQO.getIdentityCardNumber() != null && !recordCountQO.getIdentityCardNumber().isEmpty(),
                AgentRecord::getIdentityCardNumber, recordCountQO.getIdentityCardNumber())//
            .eq(StringUtils.isNotBlank(recordCountQO.getAgentName()), AgentRecord::getAgentName,
                recordCountQO.getAgentName())//
            .in(recordCountQO.getDepartmentCode() != null && !recordCountQO.getDepartmentCode().isEmpty(),
                AgentRecord::getDepartmentCode, recordCountQO.getDepartmentCode())//
            .ge(StringUtils.isNotBlank(recordCountQO.getStartTime()), AgentRecord::getTime,
                startDate.atStartOfDay(zoneId))//
            .lt(StringUtils.isNotBlank(recordCountQO.getEndTime()), AgentRecord::getTime,
                endDate.plusDays(1L).atStartOfDay(zoneId));
        if (recordCountQO.getCategory() != null && !recordCountQO.getCategory().isBlank()) {
            var categories = categoryMapper.selectList(
                Wrappers.lambdaQuery(Category.class).like(Category::getName, recordCountQO.getCategory()));
            var cids = agentConfigs.stream().filter(a -> {
                for (var ccid : a.categoryIdList()) {
                    for (var category : categories) {
                        if (Objects.equals(ccid, category.getId())) {
                            return true;
                        }
                    }
                }
                return false;
            }).map(AgentConfig::getId).toList();
            queryWrapper.in(AgentRecord::getAgentConfigId, cids);
        }

        var group = recordCountQO.getGroup();
        for (var groupEnum : group) {
            switch (groupEnum) {
                case agent -> queryWrapper.groupBy(AgentRecord::getAgentConfigId)
                    .selectAs(AgentRecord::getAgentConfigId, RecordCountVO::getAgentId)
                    .selectAs("any_value(agent_name)", RecordCountVO::getAgentName);
                case person -> queryWrapper.groupBy(AgentRecord::getIdentityCardNumber)
                    .selectAs(AgentRecord::getIdentityCardNumber, RecordCountVO::getIdentityCardNumber)
                    .selectAs("any_value(user_name)", RecordCountVO::getUserName);
                case date -> queryWrapper.groupBy("dateTime")//
                    .selectAs("DATE(time)", RecordCountVO::getDateTime);
            }
        }
        queryWrapper.selectAs("count(1)", RecordCountVO::getCount);
        var recordCountVOS = agentRecordMapper.selectJoinList(RecordCountVO.class, queryWrapper);
        log.info("count record:{} {}", recordCountQO, recordCountVOS);
        if (recordCountVOS == null || recordCountVOS.isEmpty()) {
            return Collections.emptyList();
        }
        var categories = categoryMapper.selectList();

        for (var recordCountVO : recordCountVOS) {
            if(recordCountVO.getDateTime()!=null){
                recordCountVO.setDate(DateUtil.format(recordCountVO.getDateTime(), datePattern));
            }
            var agentConfig = CollectionUtils.find(agentConfigs, AgentConfig::getId, recordCountVO.getAgentId());
            if (agentConfig == null) {
                continue;
            }
            var cs = Optional.ofNullable(agentConfig.getCategoryIds()).map(a -> a.split(",")).stream()
                .flatMap(Arrays::stream).filter(a -> !a.isBlank()).map(Long::parseLong)
                .map(cid -> CollectionUtils.find(categories, Category::getId, cid)).filter(Objects::nonNull).toList();
            recordCountVO.setCategorys(cs);
        }
        return recordCountVOS;
    }

    @Override
    public List<AgentRecord> recordListByAgentName(RecordQO recordQO) {
        LambdaQueryWrapper<AgentRecord> queryWrapper = new LambdaQueryWrapper<AgentRecord>().in(
                recordQO.getIdentityCardNumber() != null && !recordQO.getIdentityCardNumber().isEmpty(),
                AgentRecord::getIdentityCardNumber, recordQO.getIdentityCardNumber())
            .in(StringUtils.isNotBlank(recordQO.getAgentName()), AgentRecord::getAgentName, recordQO.getAgentName())
            .eq(AgentRecord::getAskType, AgentRecordAskType.AI_ASSISTANT.getValue())
            .ge(StringUtils.isNotBlank(recordQO.getStartTime()), AgentRecord::getTime, recordQO.getStartTime())
            .le(StringUtils.isNotBlank(recordQO.getEndTime()), AgentRecord::getTime, recordQO.getEndTime());
        return agentRecordMapper.selectList(queryWrapper);
    }

    @Override
    public PageResult<AgentRecord> listRecordPaged(AgentRecordQO qo) {
        return agentRecordMapper.selectPage(qo, Wrappers.lambdaQuery(AgentRecord.class)
            .like(qo.getUserName() != null && !qo.getUserName().isBlank(), AgentRecord::getUserName, qo.getUserName())
            .like(qo.getIdentityCardNumber() != null && !qo.getIdentityCardNumber().isBlank(),
                AgentRecord::getIdentityCardNumber, qo.getIdentityCardNumber())
            .like(qo.getContent() != null && !qo.getContent().isBlank(), AgentRecord::getQueryContent, qo.getContent())
            .ge(qo.getMinTime() != null && !qo.getMinTime().isBlank(), AgentRecord::getTime, qo.getMinTime())
            .le(qo.getMaxTime() != null && !qo.getMaxTime().isBlank(), AgentRecord::getTime, qo.getMaxTime())
            .in(qo.getIdList() != null && !qo.getIdList().isEmpty(), AgentRecord::getId, qo.getIdList())
            .in(AgentRecord::getAskType, AgentRecordAskType.getValidValues())
            .orderByDesc(AgentRecord::getTime));
    }

    @Override
    public List<UserAgentRecordVO> listUserAgentRecords(String userId) {
        if (StringUtils.isBlank(userId)) {
            return Collections.emptyList();
        }
        return agentRecordMapper.selectLatestUserAgentRecords(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUserAgentRecords(String idCard, UserAgentRecordVO userAgentRecordVO) {
        Assert.notEmpty(idCard, "用户身份证号不能为空");
        Long agentConfigId = userAgentRecordVO.getAgentConfigId();
        Assert.notNull(agentConfigId, "智能体配置id不能为空");
        log.info("删除用户智能体记录: idCard={}, agentConfigId={}", idCard, agentConfigId);
        LambdaUpdateWrapper<AgentRecord> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(AgentRecord::getDeleted, Boolean.TRUE)
                .eq(AgentRecord::getAgentConfigId, agentConfigId)
                .eq(AgentRecord::getIdentityCardNumber, idCard);
        agentRecordMapper.update(updateWrapper);
    }

    @Override
    public PageResult<UserAgentHistoryRecordVO> listUserAgentHistoryPaged(UserAgentHistoryQO qo) {
        Long agentId = qo.getResolvedAgentId();
        boolean useCursor = qo.getLastId() != null;
        var queryWrapper = Wrappers.lambdaQuery(AgentRecord.class)
            .eq(AgentRecord::getIdentityCardNumber, qo.getUserId())
            .eq(Objects.nonNull(agentId), AgentRecord::getAgentConfigId, agentId)
            .eq(AgentRecord::getAskType, AgentRecordAskType.AI_ASSISTANT.getValue())
            .lt(useCursor, AgentRecord::getId, qo.getLastId())
            .orderByDesc(AgentRecord::getId);
        int pageSize = normalizePageSize(qo.getPageSize());
        qo.setPageSize(pageSize);
        if (useCursor) {
            List<AgentRecord> list = agentRecordMapper.selectList(queryWrapper.last("LIMIT " + pageSize));
            return new PageResult<>(buildUserAgentHistoryRecords(list), (long) list.size());
        }
        PageResult<AgentRecord> pageResult = agentRecordMapper.selectPage(qo, queryWrapper);
        return new PageResult<>(buildUserAgentHistoryRecords(pageResult.getList()), pageResult.getTotal());
    }

    private List<UserAgentHistoryRecordVO> buildUserAgentHistoryRecords(List<AgentRecord> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> recordIds = records.stream().map(AgentRecord::getId).filter(Objects::nonNull).map(String::valueOf)
            .toList();
        Map<String, AgentQueryApprove> approveMap = recordIds.isEmpty() ? Collections.emptyMap()
            : agentQueryApproveMapper.selectList(Wrappers.lambdaQuery(AgentQueryApprove.class)
                .in(AgentQueryApprove::getRecordId, recordIds)).stream()
                .collect(Collectors.toMap(AgentQueryApprove::getRecordId, Function.identity(), (first, second) -> first));
        Map<Long, AiAgentReplyStateService.ReplyState> replyStateMap = replyStateService.loadReplyStateMap(records);
        AiSettingsVO settings = globalsService.getAiSettings();
        return records.stream().map(record -> {
            UserAgentHistoryRecordVO vo = new UserAgentHistoryRecordVO();
            vo.setId(record.getId());
            vo.setAgentConfigId(String.valueOf(record.getAgentConfigId()));
            vo.setQueryContent(record.getQueryContent());
            vo.setResponseContent(getResponseContentFromCache(record.getId(), record.getResponseContent()));
            AiAgentReplyStateService.ReplyState replyState = replyStateMap.get(record.getId());
            vo.setReplyPosition(replyStateService.resolveReplyPosition(record, replyState));
            vo.setReplyPaused(replyStateService.resolveReplyPaused(record, replyState));
            vo.setAttachement(record.getAttachement());
            vo.setAttachement_path(record.getAttachementPath());
            vo.setAgentName(record.getAgentName());
            vo.setUserName(record.getUserName());
            vo.setAnswerTime(record.getAnswerTime());
            vo.setQueryTime(record.getTime());
            AgentQueryApprove approve = approveMap.get(String.valueOf(record.getId()));
            fillApprovalInfo(vo, record, approve, settings);
            return vo;
        }).toList();
    }

    private String getResponseContentFromCache(Long recordId, String responseContent) {
        if (StringUtils.isBlank(responseContent)) {
            String cachedReply = askService.getCacheResult(recordId);
            if (cachedReply != null) {
                return cachedReply;
            }
        }
        return responseContent;
    }

    private void fillApprovalInfo(UserAgentHistoryRecordVO vo, AgentRecord record, AgentQueryApprove approve,
        AiSettingsVO settings) {
        if (approve != null) {
            vo.setApprovalRequired(true);
            vo.setApprovalStatus(StringUtils.defaultIfBlank(approve.getApproveState(), APPROVAL_STATUS_PENDING_APPROVAL));
            vo.setApproveResult(approve.getApprove());
            vo.setApproveUrl(approve.getApproveUrl());
            vo.setApproveDetailUrl(approve.getApproveDetailUrl());
            vo.setToLeaderUrl(approve.getToLeaderUrl());
            vo.setApproveUser(approve.getApproveUser());
            vo.setApprovalSubMode(record.getApprovalSubMode());
            return;
        }

        if (record.getApprovalEnabled() == 1) {
            // ask 阶段不再落审批表，未收到建单回调时历史记录仍应展示为“待建单”。
            vo.setApprovalRequired(true);
            vo.setApprovalStatus(APPROVAL_STATUS_WAIT_CREATE);
            vo.setApprovalSubMode(record.getApprovalSubMode());
            vo.setApproveUrl(buildApprovalCreateUrl(settings.getApprovalSystemUrl(), record.getId()));
            return;
        }
        vo.setApprovalRequired(false);
        vo.setApprovalStatus(APPROVAL_STATUS_NO_APPROVAL);
    }

    private boolean shouldRequireApproval(AiSettingsVO settings, AgentConfig config) {
        return settings != null
            && config != null
            && Objects.equals(settings.getApprovalEnabled(), 1)
            && StringUtils.isNotBlank(settings.getApprovalSystemUrl());
    }

    private String buildApprovalCreateUrl(String approvalSystemUrl, Long recordId) {
        HttpUrl httpUrl = HttpUrl.parse(approvalSystemUrl);
        if (httpUrl != null) {
            return httpUrl.newBuilder()
                .setQueryParameter("recordId", String.valueOf(recordId))
                .setQueryParameter("conversationId", String.valueOf(recordId))
                .build()
                .toString();
        }
        String separator = approvalSystemUrl.contains("?") ? "&" : "?";
        return approvalSystemUrl + separator + "recordId=" + recordId + "&conversationId=" + recordId;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
    }

    @Override
    public void deleteById(Long id) {
        agentRecordMapper.deleteById(id);
    }

    @Override
    public Optional<AgentRecord> getRecordById(Long id) {
        return Optional.ofNullable(agentRecordMapper.selectById(id));
    }

    /**
     * 按 (time, id) 双游标增量分页查询智能体记录。
     * <p>
     * 用于统计定时任务拉取，解决单字段时间游标在同秒多记录场景下漏数据的问题。
     * 首次拉取 timeAfter/idAfter 传 null，从最早记录开始；后续传上次返回的 lastTime/lastId。
     * 通过 LIMIT pageSize+1 判断是否还有更多数据。
     *
     * @param qo 游标查询参数（agentConfigId / timeAfter / idAfter / size）
     * @return 本页记录 + 下次游标（lastTime/lastId）+ hasMore 标记
     */
    @Override
    public AgentRecordCursorVO listRecordByCursor(AgentRecordCursorQO qo) {
        // 参数校验：qo 与 agentConfigId 必传，否则 eq(field, null) 会生成 = NULL 导致静默查不到数据
        Objects.requireNonNull(qo, "游标查询参数不能为空");
        Objects.requireNonNull(qo.getAgentConfigId(), "agentConfigId 不能为空（人员核查传 -1）");
        // 游标必须成对传：timeAfter 和 idAfter 要么都传要么都不传，半传会导致游标条件被跳过而全量重拉
        boolean hasTime = qo.getTimeAfter() != null;
        boolean hasId = qo.getIdAfter() != null;
        if (hasTime != hasId) {
            throw new IllegalArgumentException("timeAfter 与 idAfter 必须同时传或同时不传");
        }
        int pageSize = normalizePageSize(qo.getSize());
        var queryWrapper = Wrappers.lambdaQuery(AgentRecord.class)
            // 固定按 agentConfigId 过滤（人员核查传 -1）
            .eq(AgentRecord::getAgentConfigId, qo.getAgentConfigId())
            // 双游标：(time > timeAfter) OR (time = timeAfter AND id > idAfter)
            .and(hasTime,
                w -> w.gt(AgentRecord::getTime, qo.getTimeAfter())
                    .or(inner -> inner.eq(AgentRecord::getTime, qo.getTimeAfter())
                        .gt(AgentRecord::getId, qo.getIdAfter())))
            .orderByAsc(AgentRecord::getTime)
            .orderByAsc(AgentRecord::getId)
            // 多取 1 条用于判断 hasMore
            .last("LIMIT " + (pageSize + 1));
        List<AgentRecord> records = agentRecordMapper.selectList(queryWrapper);

        var vo = new AgentRecordCursorVO();
        boolean hasMore = records.size() > pageSize;
        List<AgentRecord> page = hasMore ? records.subList(0, pageSize) : records;
        vo.setList(page);
        vo.setHasMore(hasMore);
        // 返回本页最后一条作为下次游标
        if (!page.isEmpty()) {
            AgentRecord last = page.get(page.size() - 1);
            vo.setLastTime(last.getTime());
            vo.setLastId(last.getId());
        }
        return vo;
    }
}