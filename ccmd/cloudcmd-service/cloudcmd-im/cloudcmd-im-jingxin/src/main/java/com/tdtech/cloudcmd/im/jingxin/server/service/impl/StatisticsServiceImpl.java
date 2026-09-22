package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.CollaborationAttendance;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.CollaborationPostVO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.statistics.*;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImUser;
import com.tdtech.cloudcmd.im.jingxin.server.entity.*;
import com.tdtech.cloudcmd.im.jingxin.server.service.IOrganizationService;
import com.tdtech.cloudcmd.im.jingxin.server.service.IStatisticsService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.*;
import com.tdtech.cloudcmd.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 统计明细增量拉取聚合服务实现。
 * <p>
 * 各表按双游标 (时间字段, 源表唯一键) 增量拉取，聚合关联表后返回完整 DTO。
 * 双游标条件：{@code (time, id) > (timeAfter, idAfter)}，即
 * {@code time > timeAfter OR (time = timeAfter AND id > idAfter)}，保证不漏不重。
 * <p>
 * hasMore 判断：每批多查 1 条（pageSize+1），返回超过 pageSize 则有更多，截取前 pageSize 条。
 */
@Slf4j
@Service
public class StatisticsServiceImpl implements IStatisticsService {

    private static final int STATUS_TRACK = 2;
    private static final int STATUS_FINISH = 3;
    private static final int STATUS_IGNORE = 4;

    @Resource
    private CollaborationTaskResponseMapper collaborationTaskResponseMapper;

    @Resource
    private CollaborationPostMapper collaborationPostMapper;

    @Resource
    private CollaborationAttendanceMapper collaborationAttendanceMapper;

    @Resource
    private CollaborationTaskMapper collaborationTaskMapper;

    @Resource
    private CreateGroupMapper createGroupMapper;

    @Resource
    private ImService imService;

    @Resource
    private IOrganizationService organizationService;

    // ==================== 4.1 tb_static_task ====================

    @Override
    public StaticCursorResult<StaticTaskDTO> listStaticTaskByCursor(Date timeAfter, Long idAfter, int pageSize) {
        List<StaticTaskAggVO> list = collaborationTaskMapper.selectStaticTaskAggByCursor(timeAfter, idAfter, pageSize + 1);
        StaticCursorResult<StaticTaskDTO> result = new StaticCursorResult<>();
        if (list == null || list.isEmpty()) {
            result.setList(Collections.emptyList());
            result.setHasMore(false);
            return result;
        }

        boolean hasMore = list.size() > pageSize;
        List<StaticTaskAggVO> page = hasMore ? list.subList(0, pageSize) : list;

        // 校正取值逻辑，并获取需要的用户id, 用于从 im 服务批量查询姓名
        Set<Long> fallbackUserIds = refactorDataAndCollectUserIds(page);
        Map<Long, String> fallbackUserNameMap = loadUserNameMap(fallbackUserIds);

        List<StaticTaskDTO> dtoList = page.stream()
            .map(agg -> toStaticTaskDTO(agg, fallbackUserNameMap))
            .collect(Collectors.toList());
        result.setList(dtoList);

        StaticTaskAggVO last = page.get(page.size() - 1);
        result.setLastTime(last.getGmtModified());
        result.setLastId(last.getId());
        result.setHasMore(hasMore);
        return result;
    }

    private StaticTaskDTO toStaticTaskDTO(StaticTaskAggVO agg, Map<Long, String> fallbackUserNameMap) {
        StaticTaskDTO dto = new StaticTaskDTO();
        dto.setTaskId(agg.getTaskId());
        dto.setFromUserId(agg.getFromUserId());
        dto.setFromUserName(agg.getFromUserName());
        dto.setFromUserDepartmentId(agg.getFromUserDepartmentId());
        dto.setFromUserDepartmentName(agg.getFromUserDepartmentName());
        dto.setStatus(agg.getStatus());
        dto.setMsgSentTime(agg.getMsgSentTime());
        dto.setMsgSeqid(agg.getSeqid());
        dto.setPostId(agg.getPostId());
        dto.setPostName(agg.getPostName());
        dto.setCreateTime(agg.getGmtCreated());
        dto.setExpiredTime(agg.getExpiredTime());
        dto.setResponseTime(agg.getResponseTime());
        dto.setResponseUserId(agg.getResponseUserId());
        dto.setResponseUserName(agg.getResponseUserName());
        dto.setIgnoreTime(agg.getIgnoreTime());
        dto.setIgnoreUserId(agg.getIgnoreUserId());
        dto.setIgnoreUserName(StringUtils.isBlank(agg.getIgnoreUserName()) ?
                resolveFallbackName(agg.getIgnoreUserId(), fallbackUserNameMap) : agg.getIgnoreUserName());
        dto.setTrackTime(agg.getTrackTime());
        dto.setTrackUserId(agg.getTrackUserId());
        dto.setTrackUserName(StringUtils.isBlank(agg.getTrackUserName()) ?
                resolveFallbackName(agg.getTrackUserId(), fallbackUserNameMap) : agg.getTrackUserName());
        dto.setFinishTime(agg.getFinishTime());
        dto.setFinishUserId(agg.getFinishUserId());
        dto.setFinishUserName(StringUtils.isBlank(agg.getFinishUserName()) ?
                resolveFallbackName(agg.getFinishUserId(), fallbackUserNameMap) : agg.getFinishUserName());
        dto.setTaskUpdatedTime(agg.getGmtModified());
        return dto;
    }

    /**
     * 收集 fallback 场景需要查警信的 user_id。
     * <p>SQL 已关联出 track/finish/ignore 的 history，无 history 时字段为 null，据此判断是否走 fallback：
     * <ul>
     *   <li>track 无 history 时一定走 fallback（track_time 永远要有值）</li>
     *   <li>ignore/finish 仅 task 处于对应终态且无 history 时才走 fallback</li>
     * </ul>
     */
    private Set<Long> refactorDataAndCollectUserIds(List<StaticTaskAggVO> page) {
        Set<Long> userIds = new HashSet<>();
        for (StaticTaskAggVO agg : page) {
            // 对于忽略状态, 无 history 时, 取 gmt_modified + user_id
            if (agg.getStatus() == STATUS_IGNORE && agg.getIgnoreTime() == null) {
                agg.setIgnoreTime(agg.getGmtModified());
                agg.setIgnoreUserId(agg.getUserId());
                userIds.add(agg.getUserId());
            }
            // 对于办结状态, 无 history 时, 取 gmt_modified + user_id
            if (agg.getStatus() == STATUS_FINISH && agg.getFinishTime() == null) {
                agg.setFinishTime(agg.getGmtModified());
                agg.setFinishUserId(agg.getUserId());
                userIds.add(agg.getUserId());
            }
            // 对于跟踪状态, 有首次回复取首次回复的相关信息，否则取 task 创建时间 + user_id
            if (agg.getTrackTime() == null) {
                agg.setTrackTime(agg.getResponseTime() != null ? agg.getResponseTime() : agg.getGmtCreated());
                agg.setTrackUserId(agg.getResponseUserId() != null ? agg.getResponseUserId() : agg.getUserId());
                userIds.add(agg.getTrackUserId());
            }
        }
        return userIds;
    }

    private String resolveFallbackName(Long userId, Map<Long, String> fallbackUserNameMap) {
        if (userId == null) {
            return null;
        }
        return fallbackUserNameMap.get(userId);
    }

    // ==================== 4.2 tb_static_create_group ====================

    @Override
    public StaticCursorResult<StaticCreateGroupDTO> listStaticCreateGroupByCursor(Date timeAfter, Long idAfter,
        int pageSize) {
        List<StaticCreateGroupAggVO> list =
            createGroupMapper.selectStaticCreateGroupAggByCursor(timeAfter, idAfter, pageSize + 1);

        StaticCursorResult<StaticCreateGroupDTO> result = new StaticCursorResult<>();
        if (list == null || list.isEmpty()) {
            result.setList(Collections.emptyList());
            result.setHasMore(false);
            return result;
        }

        boolean hasMore = list.size() > pageSize;
        List<StaticCreateGroupAggVO> page = hasMore ? list.subList(0, pageSize) : list;

        // department_id 字段实为 IM 部门 code，code→id 需查警信 IM 服务（跨服务 RPC，无法 SQL 联表）
        Set<String> deptCodes = page.stream().map(StaticCreateGroupAggVO::getDepartmentId)
            .filter(StringUtils::isNotBlank).collect(Collectors.toSet());
        Map<String, Long> deptCodeToIdMap = loadDeptCodeToIdMap(deptCodes);

        List<StaticCreateGroupDTO> dtoList = page.stream()
            .map(agg -> toStaticCreateGroupDTO(agg, deptCodeToIdMap))
                .filter(a -> a.getUserDepartmentId() != null)
            .collect(Collectors.toList());
        result.setList(dtoList);

        StaticCreateGroupAggVO last = page.get(page.size() - 1);
        result.setLastTime(last.getUpdateTime());
        result.setLastId(last.getGroupId());
        result.setHasMore(hasMore);
        return result;
    }

    private StaticCreateGroupDTO toStaticCreateGroupDTO(StaticCreateGroupAggVO agg, Map<String, Long> deptCodeToIdMap) {
        StaticCreateGroupDTO dto = new StaticCreateGroupDTO();
        Long ownerId = StringUtils.isNotBlank(agg.getOwnerId()) ? Long.parseLong(agg.getOwnerId()) : null;
        dto.setUserId(ownerId);
        dto.setUserName(agg.getOwnerName());
        dto.setUserDepartmentId(
            StringUtils.isBlank(agg.getDepartmentId()) ? null : deptCodeToIdMap.get(agg.getDepartmentId()));
        dto.setUserDepartmentName(agg.getDepartmentName());
        dto.setGroupId(agg.getGroupId());
        dto.setGroupName(agg.getGroupName());
        dto.setGroupType(agg.getGroupType());
        dto.setGroupSubType(agg.getSource());
        // 恒为 0（手工建群，将来 openApi 再加）
        dto.setGroupCreateType(0);
        dto.setGmtCreateTime(agg.getCreateTime());
        dto.setGroupUpdatedTime(agg.getUpdateTime());
        return dto;
    }

    /**
     * 按 IM 部门 code 批量查警信，构建 code→id 映射。
     * <p>department_id 字段实为 IM 部门 code（非 id），权威来源是警信 IM 服务，不能查 tb_organization。
     */
    private Map<String, Long> loadDeptCodeToIdMap(Set<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return new HashMap<>();
        }
        List<Organization> orgs = organizationService.list(
            Wrappers.lambdaQuery(Organization.class).in(Organization::getCode, codes));
        if (orgs == null || orgs.isEmpty()) {
            return new HashMap<>();
        }
        return orgs.stream().collect(Collectors.toMap(
            Organization::getCode, Organization::getId, (a, b) -> a));
    }

    // ==================== 4.3 tb_static_task_response ====================

    @Override
    public StaticCursorResult<StaticTaskResponseDTO> listStaticTaskResponseByCursor(Date timeAfter, Long idAfter,
        int pageSize) {
        List<StaticTaskResponseAggVO> list =
            collaborationTaskResponseMapper.selectStaticTaskResponseAggByCursor(timeAfter, idAfter, pageSize + 1);

        StaticCursorResult<StaticTaskResponseDTO> result = new StaticCursorResult<>();
        if (list == null || list.isEmpty()) {
            result.setList(Collections.emptyList());
            result.setHasMore(false);
            return result;
        }

        boolean hasMore = list.size() > pageSize;
        List<StaticTaskResponseAggVO> page = hasMore ? list.subList(0, pageSize) : list;

        List<StaticTaskResponseDTO> dtoList = page.stream()
            .map(this::toStaticTaskResponseDTO)
            .collect(Collectors.toList());
        result.setList(dtoList);

        StaticTaskResponseAggVO last = page.get(page.size() - 1);
        result.setLastTime(last.getGmtCreated());
        result.setLastId(last.getId());
        result.setHasMore(hasMore);
        return result;
    }

    private StaticTaskResponseDTO toStaticTaskResponseDTO(StaticTaskResponseAggVO agg) {
        StaticTaskResponseDTO dto = new StaticTaskResponseDTO();
        dto.setTaskId(agg.getTaskId());
        dto.setResponseUserId(agg.getUserId());
        dto.setResponseUserName(agg.getUserName());
        dto.setResponseUserDepartmentId(agg.getDepartmentId());
        dto.setResponseUserDepartmentName(agg.getDepartmentName());
        dto.setResponseCoopUserId(agg.getPostId());
        dto.setResponseCoopUserName(agg.getPostName());
        dto.setResponseTime(agg.getGmtCreated());
        dto.setResponseMsgSeqid(agg.getSeqid());
        dto.setResponseId(agg.getId());
        return dto;
    }

    // ==================== 4.4 tb_static_coop_duty_switch ====================

    @Override
    public StaticCursorResult<StaticCoopDutySwitchDTO> listStaticCoopDutySwitchByCursor(Date timeAfter, Long idAfter,
        int pageSize) {
        // 双游标拉取 tb_collaboration_attendance（上下岗流水只追加，用 create_time 做游标）
        List<CollaborationAttendance> list = collaborationAttendanceMapper.selectList(
            Wrappers.lambdaQuery(CollaborationAttendance.class)
                .and(timeAfter != null, w -> w
                    .gt(CollaborationAttendance::getCreateTime, timeAfter)
                    .or(n -> n.eq(CollaborationAttendance::getCreateTime, timeAfter)
                        .gt(CollaborationAttendance::getId, idAfter)))
                .orderByAsc(CollaborationAttendance::getCreateTime)
                .orderByAsc(CollaborationAttendance::getId)
                .last("LIMIT " + (pageSize + 1)));

        StaticCursorResult<StaticCoopDutySwitchDTO> result = new StaticCursorResult<>();
        if (list == null || list.isEmpty()) {
            result.setList(Collections.emptyList());
            result.setHasMore(false);
            return result;
        }

        boolean hasMore = list.size() > pageSize;
        List<CollaborationAttendance> page = hasMore ? list.subList(0, pageSize) : list;

        List<StaticCoopDutySwitchDTO> dtoList = page.stream()
            .map(this::toStaticCoopDutySwitchDTO)
            .collect(Collectors.toList());
        result.setList(dtoList);

        CollaborationAttendance last = page.get(page.size() - 1);
        result.setLastTime(last.getCreateTime());
        result.setLastId(last.getId());
        result.setHasMore(hasMore);
        return result;
    }

    private StaticCoopDutySwitchDTO toStaticCoopDutySwitchDTO(CollaborationAttendance att) {
        StaticCoopDutySwitchDTO dto = new StaticCoopDutySwitchDTO();
        // 仅 switchType=0（手工切换）时 person_id 才是操作人，其余留空
        boolean isManual = att.getSwitchType() != null && att.getSwitchType() == 0;
        if (isManual) {
            dto.setUserId(att.getPersonId());
            dto.setUserName(att.getPersonName());
            dto.setUserDepartmentId(att.getOrgId());
            dto.setUserDepartmentName(att.getOrgName());
        }
        dto.setCoopUserId(att.getPostId());
        dto.setCoopUserName(att.getPostName());
        dto.setSwithType(att.getSwitchType());
        dto.setSwitchFlag(convertSwitchFlag(att.getType()));
        dto.setGmtCreateTime(att.getCreateTime());
        dto.setAttendanceId(att.getId());
        return dto;
    }

    /**
     * 源表 type（String）转 switch_flag：上岗→0，下岗→1，其余 null。
     */
    private Integer convertSwitchFlag(String type) {
        if ("上岗".equals(type)) {
            return 0;
        }
        if ("下岗".equals(type)) {
            return 1;
        }
        return null;
    }

    // ==================== 公共工具 ====================

    /**
     * 按 post_id 批量查 tb_collaboration_post，构建 id→postName 映射。
     */
    private Map<Long, String> loadPostNameMap(Set<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return new HashMap<>();
        }
        List<CollaborationPost> posts = collaborationPostMapper.selectByIds(postIds);
        return posts.stream().collect(Collectors.toMap(
            CollaborationPost::getId, CollaborationPost::getPostName, (a, b) -> a));
    }

    /**
     * 按 user_id 批量查警信，构建 id→name 映射。
     */
    private Map<Long, String> loadUserNameMap(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new HashMap<>();
        }
        List<ImUser> users = imService.findUserInfo(new ArrayList<>(userIds));
        if (users == null || users.isEmpty()) {
            return new HashMap<>();
        }
        return users.stream().collect(Collectors.toMap(
            ImUser::getId, ImUser::getName, (a, b) -> a));
    }

    // ==================== 4.5 协同岗查询（供 tb_static_photo_check 使用） ====================

    @Override
    public List<CollaborationPostVO> listCoopPostsByType(Integer type) {
        List<CollaborationPost> posts = collaborationPostMapper.selectList(
            Wrappers.lambdaQuery(CollaborationPost.class).eq(CollaborationPost::getType, type));
        if (posts == null || posts.isEmpty()) {
            return Collections.emptyList();
        }
        return posts.stream().map(this::toCollaborationPostVO).collect(Collectors.toList());
    }

    private CollaborationPostVO toCollaborationPostVO(CollaborationPost post) {
        CollaborationPostVO vo = new CollaborationPostVO();
        vo.setId(post.getId());
        vo.setPostName(post.getPostName());
        vo.setFileId(post.getFileId());
        vo.setIconUrl(post.getIconUrl());
        vo.setOrgId(post.getOrgId());
        vo.setOrgCode(post.getOrgCode());
        vo.setOrgName(post.getOrgName());
        vo.setRelatedUserIds(post.getRelatedUserIds());
        vo.setRelatedUserNames(post.getRelatedUserNames());
        vo.setOperationType(post.getOperationType());
        vo.setSource(post.getSource());
        vo.setOperatorId(post.getOperatorId());
        vo.setOperatorName(post.getOperatorName());
        vo.setOperateTime(post.getOperateTime());
        vo.setUpdateTime(post.getUpdateTime());
        vo.setType(post.getType());
        return vo;
    }

    @Override
    public Map<String, Long> findUserIdsByIdCards(List<String> idCards) {
        return imService.findUserIdMapByIdCards(idCards);
    }
}