package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tdtech.cloudcmd.auth.service.IIMUserRPCService;
import com.tdtech.cloudcmd.im.jingxin.api.entity.ai.RecordCountReq;
import com.tdtech.cloudcmd.im.jingxin.api.entity.ai.RecordCountResp;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.CollaborationAttendance;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.CollaborationDispositionVO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.CollaborationPostOnlineDurationVO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.CollaborationPostOnlineVO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.CollaborationReplyDurationVO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.CollaborationReplyTotalVO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.CollaborationStatisticsVO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.CollaborationTaskCountResponse;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.GroupCreationCountVO;
import com.tdtech.cloudcmd.im.jingxin.client.ImHttpClient;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImDepartment;
import com.tdtech.cloudcmd.im.jingxin.server.aiclient.ReactAiAgentClient;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationAttendanceSwitch;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationPost;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationPostGroup;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationTask;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationTaskStatisticsVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.PersonnelVerificationVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.UserCreationCountVO;
import com.tdtech.cloudcmd.im.jingxin.server.service.CollaborationPostService;
import com.tdtech.cloudcmd.im.jingxin.server.service.CollaborationStatisticsService;
import com.tdtech.cloudcmd.im.jingxin.server.service.ICollaborationTaskService;
import com.tdtech.cloudcmd.im.jingxin.server.service.ITasksExpiredService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.CollaborationAttendanceMapper;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.CollaborationAttendanceSwitchMapper;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.CollaborationPostGroupMapper;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.CollaborationPostMapper;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.CollaborationTaskMapper;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.CreateGroupMapper;
import com.tdtech.cloudcmd.mysql.entity.CcmdPage;
import com.tdtech.cloudcmd.mysql.entity.CcmdPageParam;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.DateFormatUtil;
import com.tdtech.cloudcmd.util.ListUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author lsc
 * @date 2025/7/18
 **/
@Service
@Slf4j
public class CollaborationStatisticsServiceImpl implements CollaborationStatisticsService {

    @Resource
    private CollaborationPostMapper collaborationPostMapper;
    @Resource
    private CollaborationTaskMapper collaborationTaskMapper;
    @Resource
    private CreateGroupMapper createGroupMapper;
    @Resource
    private CollaborationAttendanceSwitchMapper attendanceSwitchMapper;
    @Resource
    private CollaborationAttendanceMapper attendanceMapper;
    @Resource
    private CollaborationPostGroupMapper collaborationPostGroupMapper;
    @Resource
    private CollaborationPostService collaborationPostService;
    @Resource
    private ICollaborationTaskService collaborationTaskService;
    @Resource
    private ITasksExpiredService tasksExpiredService;
    @Resource
    private OrganizationDiversionService organizationDiversionService;
    @DubboReference
    private IIMUserRPCService imUserRPCService;
    @Resource
    private ReactAiAgentClient reactAiAgentClient;
    @Resource
    @Qualifier("coopImHttpClient")
    private ImHttpClient imHttpClient;

    @Override
    public CollaborationStatisticsVO count(String departmentCode, String startTime, String endTime) {
        QueryWrapper<CollaborationPost> queryWrapper = new QueryWrapper();

        List<ImDepartment> imDepartments = organizationDiversionService.queryDepartmentForList(departmentCode);
        List<String> collect = imDepartments.stream().map(ImDepartment::getCode).collect(Collectors.toList());

        if (StringUtils.isNotBlank(departmentCode)) {
            queryWrapper.in("org_code", collect);
        }
        if (StringUtils.isNotBlank(startTime)) {
            queryWrapper.ge("update_time", startTime);
        }
        if (StringUtils.isNotBlank(endTime)) {
            queryWrapper.le("update_time", endTime + " 23:59:59");
        }

        List<CollaborationPost> collaborationPosts = collaborationPostMapper.selectList(queryWrapper);
        if (collaborationPosts != null && !collaborationPosts.isEmpty()) {
            CollaborationStatisticsVO collaborationStatisticsVO = new CollaborationStatisticsVO();
            // 根据协同岗id统计该协同岗在线人数
            collaborationStatisticsVO.setTotal(collaborationPosts.size());
            // 根据协同岗绑定人员统计协同岗总人数
            var uids =
                collaborationPosts.stream().map(CollaborationPost::getUids).flatMap(Collection::stream).distinct()
                    .collect(Collectors.toList());
            collaborationStatisticsVO.setUserTotal(uids.size());
            // 根据协同岗绑定人员统计在线人数
            Set<Long> users = new HashSet<>();
            // 用户->协同岗映射表，用于后续统计0在岗协同岗数量，如后续增加协同岗及绑定人员关系表，则此处建议直接优化为sql联查直接输出统计结果
            Map<Long, Long> peoplePostMap = new HashMap<>();
            collaborationPosts.stream().filter(t -> StringUtils.isNotBlank(t.getRelatedUserIds())).forEach(t -> {
                String[] split = t.getRelatedUserIds().split(",");
                Arrays.stream(split).forEach(str -> {
                    // 插入当前选定协同岗包含用户id集合
                    users.add(Long.valueOf(str));
                    // 填充用户->协同岗映射表
                    peoplePostMap.put(Long.valueOf(str), t.getId());
                });
            });
            collaborationStatisticsVO.setUserOnlineTotal(attendanceSwitchMapper.countOnDutyByPersonIds(users));
            // 统计关联人员在线比例
            collaborationStatisticsVO.setUserOnlineRatio(collaborationStatisticsVO.getUserTotal().equals(0) ? 0
                : new BigDecimal(collaborationStatisticsVO.getUserOnlineTotal()).multiply(new BigDecimal(100))
                    .divide(new BigDecimal(collaborationStatisticsVO.getUserTotal()), RoundingMode.HALF_EVEN)
                    .setScale(2, RoundingMode.HALF_EVEN).doubleValue());
            // 统计0人员在线协同岗个数
            List<Long> onDutyPeople = attendanceSwitchMapper.listOnDutyPeople(users);
            // 存在在岗人员的协同岗id
            Set<Long> onDutyPosts = new HashSet<>();
            if (CollectionUtils.isNotEmpty(onDutyPeople)) {
                onDutyPeople.forEach(t -> {
                    Long postId = peoplePostMap.get(t);
                    if (postId != null) {
                        onDutyPosts.add(postId);
                    }
                });
            }
            collaborationStatisticsVO.setZeroUserOnlineTotal(collaborationStatisticsVO.getTotal() - onDutyPosts.size());
            return collaborationStatisticsVO;
        }
        return new CollaborationStatisticsVO();
    }

    @Override
    public CollaborationTaskStatisticsVO calTaskCount(String departmentCode, String startTime, String endTime) {
        CollaborationTaskStatisticsVO vo = new CollaborationTaskStatisticsVO();
        List<Long> postIdList = new ArrayList<>();
        List<Long> departmentIdList = new ArrayList<>();
        if (StringUtils.isNotBlank(departmentCode)) {
            // 查找当前部门编号
            List<ImDepartment> imDepartments = organizationDiversionService.queryDepartmentForList(departmentCode);
            departmentIdList = imDepartments.stream().map(ImDepartment::getId).collect(Collectors.toList());
            if (ListUtils.isNotBlankList(departmentIdList)) {
                List<CollaborationPost> postList = collaborationPostService.findList(departmentIdList);
                postIdList = postList.stream().map(CollaborationPost::getId).collect(Collectors.toList());
            }
        }
        if (ListUtils.isBlankList(postIdList)) {
            log.info("statistics postIdList is null");
            return vo;
        }
        log.info("statistics postIdList:{}", postIdList);

        List<CollaborationTask> taskList =
            collaborationTaskService.findList(List.of(1, 2, 3, 4, 7, 8), postIdList, startTime, endTime);
        Map<Integer, Long> taskMap =
            taskList.stream().collect(Collectors.groupingBy(CollaborationTask::getStatus, Collectors.counting()));

        // 查找未处理问题统计
        vo.setUnprocessedTaskCount(taskMap.getOrDefault(1, 0L));

        // 查找未及时回复问题统计
        vo.setUntimelyRepliedTaskCount(taskMap.getOrDefault(7, 0L));

        // 查找逾期问题回复统计
        //        Long overdueTaskCount = taskMap.get(8);
        //        if (Objects.nonNull(overdueTaskCount)) {
        //            vo.setOverdueTaskCount(overdueTaskCount);
        //        }

        // 逾期回复统计，这里他们给了方案，搞了个新表存储任务逾期回复告警的表，数据从这个表取
        // 不知道当初为什么给个这样的方案，先按这个处理吧
        Long overdueTaskCount = tasksExpiredService.countByCondition(departmentIdList, startTime, endTime);
        if (Objects.nonNull(overdueTaskCount)) {
            vo.setOverdueTaskCount(overdueTaskCount);
        }

        // 查找跟踪问题统计
        vo.setTrackingTaskCount(taskMap.getOrDefault(2, 0L));

        // task表逾期的数量
        Long overdueCount = taskMap.getOrDefault(8, 0L);
        // 查找待办问题统计
        Long pendingTaskCount =
            //            vo.getUnprocessedTaskCount() + vo.getUntimelyRepliedTaskCount() + vo.getOverdueTaskCount() + vo.getTrackingTaskCount();
            // 2026-01-30 测试说待办包含3个状态：待处理、未及时回复、已逾期
            vo.getUnprocessedTaskCount() + vo.getUntimelyRepliedTaskCount() + overdueCount;
        vo.setPendingTaskCount(pendingTaskCount);

        // 查找已办结问题统计
        vo.setCompletedTaskCount(taskMap.getOrDefault(3, 0L));

        // 无需回复问题统计
        vo.setIgnoredTaskCount(taskMap.getOrDefault(4, 0L));
        vo.setProcessedTaskCount(vo.getCompletedTaskCount() + vo.getIgnoredTaskCount());
        return vo;
    }

    @Override
    public List<CollaborationAttendance> listZeroOnDutyPosts(String departmentCode, String startTime, String endTime) {
        QueryWrapper<CollaborationPost> queryWrapper = new QueryWrapper();
        if (StringUtils.isNotBlank(departmentCode)) {
            List<ImDepartment> imDepartments = organizationDiversionService.queryDepartmentForList(departmentCode);
            List<String> collect = imDepartments.stream().map(ImDepartment::getCode).collect(Collectors.toList());
            queryWrapper.in("org_code", collect);
        }
        if (StringUtils.isNotBlank(startTime)) {
            queryWrapper.ge("update_time", startTime);
        }
        if (StringUtils.isNotBlank(endTime)) {
            queryWrapper.le("update_time", endTime + " 23:59:59");
        }
        List<CollaborationPost> collaborationPosts = collaborationPostMapper.selectList(queryWrapper);
        List<CollaborationAttendance> resultList = new ArrayList<>();
        if (CollectionUtils.isEmpty(collaborationPosts)) {
            return resultList;
        }

        // 根据协同岗绑定人员统计在线人数
        Set<Long> users = new HashSet<>();
        // 用户->协同岗映射表，用于后续统计0在岗协同岗数量，如后续增加协同岗及绑定人员关系表，则此处建议直接优化为sql联查直接输出统计结果
        Map<Long, Long> peoplePostMap = new HashMap<>();
        collaborationPosts.stream().filter(t -> StringUtils.isNotBlank(t.getRelatedUserIds())).forEach(t -> {
            String[] split = t.getRelatedUserIds().split(",");
            Arrays.stream(split).forEach(str -> {
                // 插入当前选定协同岗包含用户id集合
                users.add(Long.valueOf(str));
                // 填充用户->协同岗映射表
                peoplePostMap.put(Long.valueOf(str), t.getId());
            });
        });
        Set<Long> postIds = collaborationPosts.stream().map(CollaborationPost::getId).collect(Collectors.toSet());
        // 统计0人员在线协同岗个数
        List<Long> onDutyPeople = attendanceSwitchMapper.listOnDutyPeople(users);
        // 存在在岗人员的协同岗id
        Set<Long> onDutyPosts = new HashSet<>();
        if (CollectionUtils.isNotEmpty(onDutyPeople)) {
            onDutyPeople.forEach(t -> {
                Long postId = peoplePostMap.get(t);
                if (postId != null) {
                    onDutyPosts.add(postId);
                }
            });
        }

        Set<Long> zeroPostIds = new HashSet<>();
        // 未在onDutyPosts中的则为0在岗协同岗
        if (CollectionUtils.isEmpty(onDutyPosts)) {
            zeroPostIds = postIds;
        } else {
            for (Long postId : postIds) {
                boolean hasPerson = false;
                for (Long onDutyPostId : onDutyPosts) {
                    if (postId.equals(onDutyPostId)) {
                        hasPerson = true;
                        break;
                    }
                }
                if (!hasPerson) {
                    zeroPostIds.add(postId);
                }
            }
        }
        if (CollectionUtils.isEmpty(zeroPostIds)) {
            return resultList;
        }
        // 获取改协同岗最后一次下岗时间
        List<CollaborationAttendance> lastOffDutyTimes = attendanceMapper.getLastOffDutyTimeByPostIds(zeroPostIds);
        Map<Long, CollaborationAttendance> lastTimeMap = CollectionUtils.isEmpty(lastOffDutyTimes) ? new HashMap<>()
            : lastOffDutyTimes.stream()
                .collect(Collectors.toMap(CollaborationAttendance::getPostId, Function.identity()));
        Map<Long, CollaborationPost> postMap = collaborationPosts.stream()
            .collect(Collectors.toMap(CollaborationPost::getId, Function.identity(), (o1, o2) -> o1));
        // 封装数据
        for (Long postId : zeroPostIds) {
            CollaborationPost post = postMap.get(postId);
            CollaborationAttendance lastTime = lastTimeMap.get(postId);
            resultList.add(CollaborationAttendance.builder().postName(post.getPostName()).orgName(post.getOrgName())
                .lastPeopleNum(0).createTime(lastTime == null ? null : lastTime.getCreateTime()).build());
        }
        return resultList;
    }

    @Override
    public List<CollaborationReplyTotalVO> replyCount(String departmentCode, String startTime, String endTime) {
        List<ImDepartment> imDepartments = organizationDiversionService.queryDepartmentForList(departmentCode);
        List<Long> departmentIdList = imDepartments.stream().map(ImDepartment::getId).collect(Collectors.toList());
        log.info("replyCount ImDepartment ids:{}", departmentIdList);
        if (ListUtils.isBlankList(departmentIdList)) {
            log.info("replyCount departmentIdList is empty");
            return Collections.emptyList();
        }
        List<CollaborationPost> postList = collaborationPostService.findList(departmentIdList);
        List<Long> postIdList = postList.stream().map(CollaborationPost::getId).collect(Collectors.toList());
        if (ListUtils.isBlankList(postIdList)) {
            log.info("replyCount postIdList is empty");
            // 没有绑定协同岗位则直接返回
            return Collections.emptyList();
        }
        // 防止因为协同岗更改过组织机构，导致数据没有按协同岗的纬度统计数据
        return collaborationTaskMapper.replyCount(departmentIdList, postIdList, startTime, endTime);
    }

    @Override
    public List<CollaborationReplyTotalVO> replyCountAll(String startTime, String endTime) {
        log.info("replyCountAll: getting all reply counts");
        return collaborationTaskMapper.replyCount(null, null, startTime, endTime);
    }

    @Override
    public List<CollaborationReplyDurationVO> replyDuration(String departmentCode, String startTime, String endTime) {
        List<ImDepartment> imDepartments = organizationDiversionService.queryDepartmentForList(departmentCode);
        List<Long> departmentIdList = imDepartments.stream().map(ImDepartment::getId).collect(Collectors.toList());
        log.info("replyDuration ImDepartment ids:{}", departmentIdList);
        if (ListUtils.isBlankList(departmentIdList)) {
            log.info("replyDuration departmentIdList is empty");
            return Collections.emptyList();
        }
        List<CollaborationPost> postList = collaborationPostService.findList(departmentIdList);
        List<Long> postIdList = postList.stream().map(CollaborationPost::getId).collect(Collectors.toList());
        if (ListUtils.isBlankList(postIdList)) {
            log.info("replyDuration postIdList is empty");
            // 没有绑定协同岗位则直接返回
            return Collections.emptyList();
        }
        // 防止因为协同岗更改过组织机构，导致数据没有按协同岗的纬度统计数据
        return collaborationTaskMapper.replyDuration(departmentIdList, postIdList, startTime, endTime);

    }

    @Override
    public List<CollaborationDispositionVO> dispositionCount(String departmentCode, String startTime, String endTime) {
        List<ImDepartment> imDepartments = organizationDiversionService.queryDepartmentForList(departmentCode);
        List<String> collect = imDepartments.stream().map(ImDepartment::getCode).collect(Collectors.toList());
        log.info("dispositionCount ImDepartment codes:{}", collect);
        return collaborationTaskMapper.dispositionCount(collect, startTime, endTime);
    }

    @Override
    public List<CollaborationDispositionVO> dispositionReplyDuration(String departmentCode, String startTime,
        String endTime) {
        List<ImDepartment> imDepartments = organizationDiversionService.queryDepartmentForList(departmentCode);
        List<String> collect = imDepartments.stream().map(ImDepartment::getCode).collect(Collectors.toList());
        log.info("dispositionReplyDuration ImDepartment codes:{}", collect);
        return collaborationTaskMapper.dispositionReplyDuration(collect, startTime, endTime);
    }

    @Override
    public List<GroupCreationCountVO> groupCreateCountByCode(String departmentCode, String startTime, String endTime,
        Integer source) {
        List<ImDepartment> imDepartments = organizationDiversionService.queryDepartmentForList(departmentCode);
        List<String> collect = imDepartments.stream().map(ImDepartment::getCode).collect(Collectors.toList());
        if (collect.isEmpty()) {
            return Collections.emptyList();
        }
        log.info("groupCreateCountByCode ImDepartment codes:{}", collect);
        return createGroupMapper.groupCreateCountByCode(collect, startTime, endTime, source);
    }

    @Override
    public List<GroupCreationCountVO> groupCreateCountAll(String startTime, String endTime, Integer source) {
        log.info("groupCreateCountAll: getting all group creation counts");
        return createGroupMapper.groupCreateCountByCode(null, startTime, endTime, source);
    }

    @Override
    public List<UserCreationCountVO> groupCreateCountByUser(String departmentCode, String startTime, String endTime) {
        List<ImDepartment> imDepartments = organizationDiversionService.queryDepartmentForList(departmentCode);
        List<String> collect = imDepartments.stream().map(ImDepartment::getCode).collect(Collectors.toList());
        log.info("groupCreateCountByUser ImDepartment codes:{}", collect);
        return createGroupMapper.groupCreateCountByUser(collect, startTime, endTime);
    }

    @Override
    public List<CollaborationPostOnlineVO> onlineStatistics(String departmentCode, String startTime, String endTime) {
        LambdaQueryWrapper<CollaborationPost> queryWrapper;
        if(StringUtils.isNotBlank(departmentCode)){
            List<ImDepartment> imDepartments = organizationDiversionService.queryDepartmentForList(departmentCode);
            if(CollectionUtils.isEmpty(imDepartments)){
                 return Collections.emptyList();
            }
            List<String> orgCodeList = imDepartments.stream().map(ImDepartment::getCode).collect(Collectors.toList());
            queryWrapper = Wrappers.lambdaQuery(CollaborationPost.class)
                    .in(CollaborationPost::getOrgCode, orgCodeList)
                    .orderByAsc(CollaborationPost::getOperateTime);
        }else {
           queryWrapper = Wrappers.lambdaQuery(CollaborationPost.class).orderByAsc(CollaborationPost::getOperateTime);
        }
        List<CollaborationPost> postList = collaborationPostMapper.selectList(queryWrapper);

        if (CollectionUtils.isEmpty(postList)) {
            return Collections.emptyList();
        }
        List<Long> postIdList = postList.stream().map(CollaborationPost::getId).collect(Collectors.toList());
        // 查询上岗人员信息
        //        List<CollaborationAttendance> attendanceList = attendanceMapper.lastPeopleListByPostIdList(postIdList);
        //        Map<Long, List<CollaborationAttendance>> onlineMap =
        //            attendanceList.stream().collect(Collectors.groupingBy(CollaborationAttendance::getPostId));

        List<Long> userIdList = postList.stream()
            .map(post -> Arrays.stream(post.getRelatedUserIds().split(",")).collect(Collectors.toList()))
            .flatMap(List::stream).distinct().map(Long::parseLong).collect(Collectors.toList());
        Map<Long, Integer> userSwitchMap = attendanceSwitchMapper.getByPersonIds(userIdList).stream().collect(
            Collectors.toMap(CollaborationAttendanceSwitch::getPersonId,
                CollaborationAttendanceSwitch::getSwitchStatus));

        // 查询群组
        List<CollaborationPostGroup> groupList = collaborationPostGroupMapper.listByPostIds(postIdList);
        Map<Long, List<CollaborationPostGroup>> groupMap =
            groupList.stream().collect(Collectors.groupingBy(CollaborationPostGroup::getPostId));

        return postList.stream().map(post -> {
            CollaborationPostOnlineVO vo = new CollaborationPostOnlineVO();
            vo.setPostName(post.getPostName());
            vo.setIconUrl(post.getIconUrl());
            String relatedUserIds = post.getRelatedUserIds();
            if (StringUtils.isNotBlank(relatedUserIds)) {
                vo.setTotalCount(relatedUserIds.split(",").length);
            }
            List<Long> onLineUserIdList = onLineUserIdList(userSwitchMap, post);
            if (CollectionUtils.isNotEmpty(onLineUserIdList)) {
                vo.setOnlineCount(onLineUserIdList.size());
                Map<Long, String> uidNameMap = post.toUidNameMap();
                vo.setOnlineUsers(
                    onLineUserIdList.stream().map(id -> uidNameMap.get(id)).collect(Collectors.joining("、")));
            }
            List<CollaborationPostGroup> matchedGroupList = groupMap.get(post.getId());
            if (CollectionUtils.isNotEmpty(matchedGroupList)) {
                vo.setGroupCount(matchedGroupList.size());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    private List<Long> onLineUserIdList(Map<Long, Integer> userSwitchMap, CollaborationPost post) {
        return post.getUids().stream().filter(userId -> {
            Integer switchValue = userSwitchMap.get(userId);
            return Objects.nonNull(switchValue) && 0 == switchValue;
        }).collect(Collectors.toList());

    }

    @Override
    public CcmdPage<CollaborationPostOnlineDurationVO> onlineDuration(CcmdPageParam pageParam, Date startTime, Date endTime, String departmentCode) {
        var collaborationPosts = collaborationPostMapper.selectPageX(pageParam,
                Wrappers.lambdaQuery(CollaborationPost.class)
                        .eq(departmentCode != null, CollaborationPost::getOrgCode, departmentCode));
        if (collaborationPosts.isEmpty()) {
            return CcmdPage.empty(pageParam);
        }

        var uids = collaborationPosts.getRecords().stream()
                .map(CollaborationPost::getUids)
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toList());

        LambdaQueryWrapper<CollaborationAttendance> wrapper = Wrappers.lambdaQuery(CollaborationAttendance.class)
                .in(CollaborationAttendance::getPersonId, uids)
                .ge(startTime != null, CollaborationAttendance::getCreateTime, startTime)
                .le(endTime != null, CollaborationAttendance::getCreateTime, endTime)
                .orderByAsc(CollaborationAttendance::getCreateTime); // 关键修复

        var collaborationAttendances = attendanceMapper.selectList(wrapper);
        if (collaborationAttendances == null || collaborationAttendances.isEmpty()) {
            return CcmdPage.empty(pageParam);
        }

        Map<Long, Long> userOnlineSecMap = new HashMap<>();
        Map<Long, List<CollaborationAttendance>> groupAttMap = collaborationAttendances.stream()
                .collect(Collectors.groupingBy(CollaborationAttendance::getPersonId));

        for (Map.Entry<Long, List<CollaborationAttendance>> entry : groupAttMap.entrySet()) {
            List<CollaborationAttendance> userAtts = entry.getValue();
            long totalSecond = 0L;
            Date lastOnlineTime = null;
            boolean isOnline = false;

            long endTs = (endTime != null) ? endTime.getTime() : System.currentTimeMillis();
            for (CollaborationAttendance att : userAtts) {
                long currTs = att.getCreateTime().getTime();
                String type = att.getType();

                if ("上岗".equals(type)) {
                    if (!isOnline) {
                        lastOnlineTime = att.getCreateTime();
                        isOnline = true;
                    }
                } else if ("下岗".equals(type)) {
                    if (isOnline && lastOnlineTime != null) {
                        long sec = (currTs - lastOnlineTime.getTime()) / 1000;
                        totalSecond += Math.max(sec, 0);
                        isOnline = false;
                        lastOnlineTime = null;
                    }
                }
            }

            if (isOnline && lastOnlineTime != null) {
                long sec = (endTs - lastOnlineTime.getTime()) / 1000;
                totalSecond += Math.max(sec, 0);
            }

            userOnlineSecMap.put(entry.getKey(), totalSecond);
        }

        List<CollaborationPostOnlineDurationVO> voList = collaborationPosts.getRecords().stream()
                .map(post -> {
                    CollaborationPostOnlineDurationVO vo = BeanCopyUtils.copyBean(post, CollaborationPostOnlineDurationVO::new);
                    double avgSec = post.getUids().stream()
                            .mapToLong(id -> userOnlineSecMap.getOrDefault(id, 0L))
                            .average()
                            .orElse(0D);
                    vo.setOnlineDuration((long) avgSec);
                    return vo;
                }).collect(Collectors.toList());

        return collaborationPosts.mult(voList);
    }

    @Override
    public Map<Long, List<Long>> getGroupIds(List<Long> collaborationIds, String startTime, String endTime) {
        log.info("getGroupIds collaborationIds: {} startTime: {} endTime: {}", collaborationIds, startTime, endTime);
        // 查询群组
        List<CollaborationPostGroup> groupList =
            collaborationPostGroupMapper.findList(collaborationIds, startTime, endTime);
        Map<Long, List<Long>> groupMap = groupList.stream()
            // 以postId为键
            .collect(Collectors.groupingBy(CollaborationPostGroup::getPostId,
                // 收集每个postId对应的所有groupId
                Collectors.mapping(CollaborationPostGroup::getGroupId, Collectors.toList())));
        return groupMap;
    }

    @Override
    public int groupCreateAllCount(String departmentCode, String startTime, String endTime, Integer source) {
        List<ImDepartment> imDepartments = organizationDiversionService.queryDepartmentForList(departmentCode);
        List<String> collect = imDepartments.stream().map(ImDepartment::getCode).collect(Collectors.toList());
        return createGroupMapper.groupCreateAllCount(collect, startTime, endTime, source);
    }

    @Override
    public List<RecordCountResp> countAgentRecords(List<String> departmentCode, String startTime, String endTime,
        String personName, String agentName, String category, List<RecordCountReq.GroupEnum> group) {
        RecordCountReq req = new RecordCountReq();
        req.setGroup(group);
        req.setDepartmentCode(departmentCode);
        req.setAgentName(agentName);
        req.setPersonName(personName);
        req.setCategory(category);
        if (StringUtils.isNotBlank(startTime)) {
            req.setStartTime(startTime);
        }
        if (StringUtils.isNotBlank(endTime)) {
            req.setEndTime(endTime);
        }

        var resps = reactAiAgentClient.countAIRecord(req);
        if (resps == null || resps.isEmpty()) {
            log.warn("countAgentRecords resp is empty,:{} {} {} {} {} {}", departmentCode, startTime, endTime,
                personName, category, group);
            return Collections.emptyList();
        }
        return resps;
    }

    @Override
    public List<PersonnelVerificationVO> personnelVerification(String departmentCode, String startTime,
        String endTime) {
        var imDepartments = organizationDiversionService.queryDepartmentForList(departmentCode);
        var collect = imDepartments.stream().map(ImDepartment::getCode).collect(Collectors.toList());
        var aiRecord = countAgentRecords(collect, startTime, endTime, null, "人员核查", null,
            List.of(RecordCountReq.GroupEnum.date));
        if (CollectionUtils.isEmpty(aiRecord)) {
            log.info("personnelVerification aiRecord is empty");
            return Collections.emptyList();
        }
        return aiRecord.stream().sorted(Comparator.comparingLong(a -> a.getDate().getTime())).map(a -> {
            PersonnelVerificationVO vo = new PersonnelVerificationVO();
            vo.setDate(DateFormatUtil.format(a.getDate(), DateFormatUtil.YYYY_MM_DD));
            vo.setCount(a.getCount());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<CollaborationTaskCountResponse> getCollabCount(List<Long> collaborationIds, String startTime,
        String endTime) {
        List<CollaborationTaskCountResponse> collaborationTaskCountResponses =
            collaborationTaskMapper.selectCountByUserIds(collaborationIds, startTime, endTime);
        return collaborationTaskCountResponses;
    }

}
