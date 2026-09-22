package com.tdtech.cloudcmd.linkx.dashboard.service.impl;

import com.tdtech.cloudcmd.im.jingxin.api.StatisticsRpcApi;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.CollaborationPostVO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.statistics.StaticCoopDutySwitchDTO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.statistics.StaticCreateGroupDTO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.statistics.StaticCursorResult;
import com.tdtech.cloudcmd.im.jingxin.api.entity.statistics.StaticTaskDTO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.statistics.StaticTaskResponseDTO;
import com.tdtech.cloudcmd.im.jingxin.client.ai.AiAgentRecordClient;
import com.tdtech.cloudcmd.im.jingxin.client.ai.AgentRecordCursorResult;
import com.tdtech.cloudcmd.linkx.dashboard.entity.StaticCoopDutySwitch;
import com.tdtech.cloudcmd.linkx.dashboard.entity.StaticCreateGroup;
import com.tdtech.cloudcmd.linkx.dashboard.entity.StaticPhotoCheck;
import com.tdtech.cloudcmd.linkx.dashboard.entity.StaticTask;
import com.tdtech.cloudcmd.linkx.dashboard.entity.StaticTaskResponse;
import com.tdtech.cloudcmd.linkx.dashboard.mapper.StaticCoopDutySwitchMapper;
import com.tdtech.cloudcmd.linkx.dashboard.mapper.StaticCreateGroupMapper;
import com.tdtech.cloudcmd.linkx.dashboard.mapper.StaticPhotoCheckMapper;
import com.tdtech.cloudcmd.linkx.dashboard.mapper.StaticTaskMapper;
import com.tdtech.cloudcmd.linkx.dashboard.mapper.StaticTaskResponseMapper;
import com.tdtech.cloudcmd.linkx.dashboard.service.IStaticSyncService;
import com.tdtech.cloudcmd.linkx.dashboard.support.StaticSyncProperties;
import com.tdtech.cloudcmd.linkx.dashboard.support.SyncWatermark;
import com.tdtech.cloudcmd.linkx.dashboard.support.WatermarkHelper;
import com.tdtech.cloudcmd.util.IdWorker;
import com.tdtech.cloudcmd.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 统计明细同步服务实现。
 * <p>
 * 4 张表（task / create_group / task_response / coop_duty_switch）走 Dubbo 拉 im-jingxin 聚合好的 DTO，
 * 复用 {@link #runCursorSync} 通用双游标循环。
 * <p>
 * tb_static_photo_check 跨体系：HTTP 拉 ai-agent 核查记录 + Dubbo 查警信(身份证→userId) + Dubbo 查协同岗，
 * 在 dashboard 侧组装后 upsert。
 * <p>
 * 水位线语义：每页拉取并 upsert 成功后，用本页最后一条的 (lastTime, lastId) 推进水位线。
 * upsert 失败则抛异常中断本轮（不推进水位线），下轮从原水位线重试。
 */
@Slf4j
@Service
public class StaticSyncServiceImpl implements IStaticSyncService {

    private static final long AGENT_CONFIG_ID_PHOTO_CHECK = -1L;
    private static final int COOP_POST_TYPE_1_14E = 1;
    private static final int CHECK_RESULT_SUCCESS = 1;
    private static final String TABLE_STATIC_TASK = "tb_static_task";
    private static final String TABLE_STATIC_CREATE_GROUP = "tb_static_create_group";
    private static final String TABLE_STATIC_TASK_RESPONSE = "tb_static_task_response";
    private static final String TABLE_STATIC_PHOTO_CHECK = "tb_static_photo_check";
    private static final String TABLE_STATIC_COOP_DUTY_SWITCH = "tb_static_coop_duty_switch";

    @Resource
    private WatermarkHelper watermarkHelper;

    @Resource
    private StaticSyncProperties properties;

    @Resource
    private IdWorker idWorker;

    @Resource
    private AiAgentRecordClient aiAgentRecordClient;

    @DubboReference
    private StatisticsRpcApi statisticsRpcApi;

    @Resource
    private StaticTaskMapper staticTaskMapper;

    @Resource
    private StaticCreateGroupMapper staticCreateGroupMapper;

    @Resource
    private StaticTaskResponseMapper staticTaskResponseMapper;

    @Resource
    private StaticPhotoCheckMapper staticPhotoCheckMapper;

    @Resource
    private StaticCoopDutySwitchMapper staticCoopDutySwitchMapper;

    // ==================== tb_static_task ====================

    @Override
    public void syncStaticTask() {
        if (!properties.isEnabled()) {
            return;
        }
        runCursorSync(TABLE_STATIC_TASK, staticTaskMapper::selectMaxWatermark, (timeAfter, idAfter) -> {
            StaticCursorResult<StaticTaskDTO> result =
                statisticsRpcApi.listStaticTaskByCursor(timeAfter, idAfter, properties.getPageSize());
            if (result != null && result.getList() != null && !result.getList().isEmpty()) {
                List<StaticTask> entities = result.getList().stream()
                    .map(this::toStaticTask).collect(Collectors.toList());
                staticTaskMapper.upsertBatch(entities);
                log.info("syncStaticTask upsert batch, size:{}", entities.size());
            }
            return result;
        });
    }

    private StaticTask toStaticTask(StaticTaskDTO dto) {
        StaticTask e = new StaticTask();
        e.setId(idWorker.nextId());
        e.setTaskId(dto.getTaskId());
        e.setFromUserId(dto.getFromUserId());
        e.setFromUserName(dto.getFromUserName());
        e.setFromUserDepartmentId(dto.getFromUserDepartmentId());
        e.setFromUserDepartmentName(dto.getFromUserDepartmentName());
        e.setStatus(dto.getStatus());
        e.setMsgSentTime(dto.getMsgSentTime());
        e.setMsgSeqid(dto.getMsgSeqid());
        e.setPostId(dto.getPostId());
        e.setPostName(dto.getPostName());
        e.setCreateTime(dto.getCreateTime());
        e.setExpiredTime(dto.getExpiredTime());
        e.setResponseTime(dto.getResponseTime());
        e.setResponseUserId(dto.getResponseUserId());
        e.setResponseUserName(dto.getResponseUserName());
        e.setIgnoreTime(dto.getIgnoreTime());
        e.setIgnoreUserId(dto.getIgnoreUserId());
        e.setIgnoreUserName(dto.getIgnoreUserName());
        e.setTrackTime(dto.getTrackTime());
        e.setTrackUserId(dto.getTrackUserId());
        e.setTrackUserName(dto.getTrackUserName());
        e.setFinishTime(dto.getFinishTime());
        e.setFinishUserId(dto.getFinishUserId());
        e.setFinishUserName(dto.getFinishUserName());
        e.setTaskUpdatedTime(dto.getTaskUpdatedTime());
        return e;
    }

    // ==================== tb_static_create_group ====================

    @Override
    public void syncStaticCreateGroup() {
        if (!properties.isEnabled()) {
            return;
        }
        runCursorSync(TABLE_STATIC_CREATE_GROUP, staticCreateGroupMapper::selectMaxWatermark,
            (timeAfter, idAfter) -> {
                StaticCursorResult<StaticCreateGroupDTO> result =
                    statisticsRpcApi.listStaticCreateGroupByCursor(timeAfter, idAfter, properties.getPageSize());
                if (result != null && result.getList() != null && !result.getList().isEmpty()) {
                    List<StaticCreateGroup> entities = result.getList().stream()
                        .map(this::toStaticCreateGroup).collect(Collectors.toList());
                    staticCreateGroupMapper.upsertBatch(entities);
                    log.info("syncStaticCreateGroup upsert batch, size:{}", entities.size());
                }
                return result;
            });
    }

    private StaticCreateGroup toStaticCreateGroup(StaticCreateGroupDTO dto) {
        StaticCreateGroup e = new StaticCreateGroup();
        e.setId(idWorker.nextId());
        e.setUserId(dto.getUserId());
        e.setUserName(dto.getUserName());
        e.setUserDepartmentId(dto.getUserDepartmentId());
        e.setUserDepartmentName(dto.getUserDepartmentName());
        e.setGroupId(dto.getGroupId());
        e.setGroupName(dto.getGroupName());
        e.setGroupType(dto.getGroupType());
        e.setGroupSubType(dto.getGroupSubType());
        e.setGroupCreateType(dto.getGroupCreateType());
        e.setGmtCreateTime(dto.getGmtCreateTime());
        e.setGroupUpdatedTime(dto.getGroupUpdatedTime());
        return e;
    }

    // ==================== tb_static_task_response ====================

    @Override
    public void syncStaticTaskResponse() {
        if (!properties.isEnabled()) {
            return;
        }
        runCursorSync(TABLE_STATIC_TASK_RESPONSE, staticTaskResponseMapper::selectMaxWatermark,
            (timeAfter, idAfter) -> {
                StaticCursorResult<StaticTaskResponseDTO> result =
                    statisticsRpcApi.listStaticTaskResponseByCursor(timeAfter, idAfter, properties.getPageSize());
                if (result != null && result.getList() != null && !result.getList().isEmpty()) {
                    List<StaticTaskResponse> entities = result.getList().stream()
                        .map(this::toStaticTaskResponse).collect(Collectors.toList());
                    staticTaskResponseMapper.upsertBatch(entities);
                    log.info("syncStaticTaskResponse upsert batch, size:{}", entities.size());
                }
                return result;
            });
    }

    private StaticTaskResponse toStaticTaskResponse(StaticTaskResponseDTO dto) {
        StaticTaskResponse e = new StaticTaskResponse();
        e.setId(idWorker.nextId());
        e.setTaskId(dto.getTaskId());
        e.setResponseUserId(dto.getResponseUserId());
        e.setResponseUserName(dto.getResponseUserName());
        e.setResponseUserDepartmentId(dto.getResponseUserDepartmentId());
        e.setResponseUserDepartmentName(dto.getResponseUserDepartmentName());
        e.setResponseCoopUserId(dto.getResponseCoopUserId());
        e.setResponseCoopUserName(dto.getResponseCoopUserName());
        e.setResponseTime(dto.getResponseTime());
        e.setResponseMsgSeqid(dto.getResponseMsgSeqid());
        e.setResponseId(dto.getResponseId());
        return e;
    }

    // ==================== tb_static_coop_duty_switch ====================

    @Override
    public void syncStaticCoopDutySwitch() {
        if (!properties.isEnabled()) {
            return;
        }
        runCursorSync(TABLE_STATIC_COOP_DUTY_SWITCH, staticCoopDutySwitchMapper::selectMaxWatermark,
            (timeAfter, idAfter) -> {
                StaticCursorResult<StaticCoopDutySwitchDTO> result =
                    statisticsRpcApi.listStaticCoopDutySwitchByCursor(timeAfter, idAfter, properties.getPageSize());
                if (result != null && result.getList() != null && !result.getList().isEmpty()) {
                    List<StaticCoopDutySwitch> entities = result.getList().stream()
                        .map(this::toStaticCoopDutySwitch).collect(Collectors.toList());
                    staticCoopDutySwitchMapper.upsertBatch(entities);
                    log.info("syncStaticCoopDutySwitch upsert batch, size:{}", entities.size());
                }
                return result;
            });
    }

    private StaticCoopDutySwitch toStaticCoopDutySwitch(StaticCoopDutySwitchDTO dto) {
        StaticCoopDutySwitch e = new StaticCoopDutySwitch();
        e.setId(idWorker.nextId());
        e.setUserId(dto.getUserId());
        e.setUserName(dto.getUserName());
        e.setUserDepartmentId(dto.getUserDepartmentId());
        e.setUserDepartmentName(dto.getUserDepartmentName());
        e.setCoopUserId(dto.getCoopUserId());
        e.setCoopUserName(dto.getCoopUserName());
        e.setSwithType(dto.getSwithType());
        e.setSwitchFlag(dto.getSwitchFlag());
        e.setGmtCreateTime(dto.getGmtCreateTime());
        e.setAttendanceId(dto.getAttendanceId());
        return e;
    }

    // ==================== tb_static_photo_check（dashboard 侧聚合） ====================

    @Override
    public void syncStaticPhotoCheck() {
        if (!properties.isEnabled()) {
            return;
        }
        String tableName = TABLE_STATIC_PHOTO_CHECK;
        SyncWatermark wm = watermarkHelper.getWatermark(tableName, staticPhotoCheckMapper::selectMaxWatermark);
        int pageSize = properties.getPageSize();
        // 协同岗 ≤300，一轮拉一次即可（不同批核查记录复用同一份协同岗映射）
        Map<Long, CoopPostRef> coopPostMap = loadCoopPostMap();

        for (int i = 0; i < properties.getMaxBatchesPerRound(); i++) {
            AgentRecordCursorResult result = aiAgentRecordClient.listRecordByCursor(
                AGENT_CONFIG_ID_PHOTO_CHECK, wm.getTime(), wm.getId(), pageSize);
            if (result == null || result.getList() == null || result.getList().isEmpty()) {
                break;
            }
            List<StaticPhotoCheck> entities = buildPhotoCheckEntities(result.getList(), coopPostMap);
            if (!entities.isEmpty()) {
                staticPhotoCheckMapper.upsertBatch(entities);
                log.info("syncStaticPhotoCheck upsert batch, fetched:{}, upserted:{}",
                    result.getList().size(), entities.size());
            }
            // 用本页最后一条推进水位线（即使部分记录因查不到 userId 被丢弃，水位线仍按源表游标推进，避免重复拉取）
            wm = new SyncWatermark(toDate(result.getLastTime()), result.getLastId());
            watermarkHelper.setWatermark(tableName, wm);
            if (!Boolean.TRUE.equals(result.getHasMore())) {
                break;
            }
        }
    }

    private List<StaticPhotoCheck> buildPhotoCheckEntities(List<AgentRecordCursorResult.AgentRecordItem> records,
        Map<Long, CoopPostRef> coopPostMap) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        // 批量查警信：身份证→userId
        List<String> idCards = records.stream()
            .map(AgentRecordCursorResult.AgentRecordItem::getIdentityCardNumber)
            .filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        Map<String, Long> idCardToUserId = statisticsRpcApi.findUserIdsByIdCards(idCards);
        if (idCardToUserId == null) {
            idCardToUserId = Collections.emptyMap();
        }

        List<StaticPhotoCheck> entities = new ArrayList<>(records.size());
        for (AgentRecordCursorResult.AgentRecordItem rec : records) {
            Long userId = idCardToUserId.get(rec.getIdentityCardNumber());
            if (userId == null) {
                // 查不到警信 userId 则记日志不入库（spec 5.5.6）
                log.warn("syncStaticPhotoCheck skip, idCard not found in im, checkDataId:{}, idCard:{}",
                    rec.getId(), rec.getIdentityCardNumber());
                continue;
            }
            StaticPhotoCheck e = new StaticPhotoCheck();
            e.setId(idWorker.nextId());
            e.setCheckUserId(userId);
            e.setCheckUserName(rec.getUserName());
            e.setCheckUserDepartmentId(parseLong(rec.getDepartmentId()));
            e.setCheckUserDepartmentName(rec.getDepartmentName());
            CoopPostRef coop = coopPostMap.get(userId);
            if (coop != null) {
                e.setCoopUserId(coop.postId);
                e.setCoopUserName(coop.postName);
            }
            e.setCheckDataId(rec.getId());
            e.setGmtCreateTime(toDate(rec.getTime()));
            e.setCheckResult(CHECK_RESULT_SUCCESS);
            entities.add(e);
        }
        return entities;
    }

    /**
     * 拉取所有 type=1 协同岗，内存构建 userId→(postId, postName) 映射。
     * related_user_ids 是逗号分隔的 userId 串，一个用户可能关联多个协同岗，任取一条。
     */
    private Map<Long, CoopPostRef> loadCoopPostMap() {
        List<CollaborationPostVO> posts = statisticsRpcApi.listCoopPostsByType(COOP_POST_TYPE_1_14E);
        if (posts == null || posts.isEmpty()) {
            return new HashMap<>();
        }
        Map<Long, CoopPostRef> result = new HashMap<>();
        for (CollaborationPostVO post : posts) {
            if (StringUtils.isBlank(post.getRelatedUserIds()) || post.getId() == null) {
                continue;
            }
            CoopPostRef ref = new CoopPostRef(post.getId(), post.getPostName());
            for (String uid : post.getRelatedUserIds().split(",")) {
                if (StringUtils.isBlank(uid)) {
                    continue;
                }
                result.putIfAbsent(Long.parseLong(uid.trim()), ref);
            }
        }
        return result;
    }

    // ==================== 通用双游标循环（4 张 RPC 表共用） ====================

    /**
     * 通用双游标增量同步循环。
     * <p>
     * 每轮：读水位线 → 调 fetchConvertUpsertStep（拉一页 + 转 entity + upsert）→ 用结果游标推进水位线 → hasMore 则继续。
     * step 内 upsert 抛异常则中断本轮，水位线不推进，下轮重试。
     *
     * @param tableName              统计表名（Redis key）
     * @param fallback               Redis 未命中时从统计表回查水位线
     * @param fetchConvertUpsertStep 拉取一页并 upsert，返回游标分页结果（含 lastTime/lastId/hasMore）
     */
    private void runCursorSync(String tableName, Supplier<SyncWatermark> fallback,
        BiFunction<Date, Long, StaticCursorResult<?>> fetchConvertUpsertStep) {
        SyncWatermark wm = watermarkHelper.getWatermark(tableName, fallback);
        for (int i = 0; i < properties.getMaxBatchesPerRound(); i++) {
            StaticCursorResult<?> result = fetchConvertUpsertStep.apply(wm.getTime(), wm.getId());
            if (result == null || result.getList() == null || result.getList().isEmpty()) {
                break;
            }
            wm = new SyncWatermark(result.getLastTime(), result.getLastId());
            watermarkHelper.setWatermark(tableName, wm);
            if (!Boolean.TRUE.equals(result.getHasMore())) {
                break;
            }
        }
    }

    // ==================== 工具 ====================

    private static Date toDate(LocalDateTime ldt) {
        return ldt == null ? null : Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * epoch 毫秒时间戳转 Date（ai-agent 服务端 time/lastTime 字段为毫秒时间戳）。
     */
    private static Date toDate(Long epochMillis) {
        return epochMillis == null ? null : new Date(epochMillis);
    }

    private static Long parseLong(String s) {
        if (StringUtils.isBlank(s)) {
            return null;
        }
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 协同岗引用（postId + postName）。 */
    private static class CoopPostRef {
        final Long postId;
        final String postName;

        CoopPostRef(Long postId, String postName) {
            this.postId = postId;
            this.postName = postName;
        }
    }
}