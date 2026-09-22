package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import cloudcmd.service.rpc.PerWarningRalationRpcService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.tdtech.cloudcmd.bean.CagentMqFrame;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.im.jingxin.client.ImHttpClient;
import com.tdtech.cloudcmd.im.jingxin.client.entity.*;
import com.tdtech.cloudcmd.im.jingxin.server.entity.*;
import com.tdtech.cloudcmd.im.jingxin.server.service.CollaborationPostGroupService;
import com.tdtech.cloudcmd.im.jingxin.server.service.DepartmentLocationService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.*;
import com.tdtech.cloudcmd.im.jingxin.server.util.FileUtil;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.IdWorker;
import com.tdtech.cloudcmd.util.ListUtils;
import com.tdtech.cloudcmd.util.StringUtils;
import com.tdtech.cloudcmd.util.json.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class CollaborationPostGroupServiceImpl implements CollaborationPostGroupService {

    private static final String ASYNC_BIND_SUPPORT_CHANNEL = "cloudcmd-jinxing-asyncpostpick";

    private final IdWorker idWorker;
    private final ImHttpClient imHttpClient;
    private final StreamBridge streamBridge;
    private final CollaborationPostGroupMapper collaborationPostGroupMapper;
    private final CollaborationPostMapper collaborationPostMapper;
    private final CollaborationAttendanceSwitchMapper collaborationAttendanceSwitchMapper;
    private final CreateGroupMapper createGroupMapper;
    private final GroupExtendsMapper groupExtendsMapper;
    @Resource
    private DepartmentLocationMapper departmentLocationMapper;
    @Resource
    private UserGroupCareMapper userGroupCareMapper;
    @Resource
    private GroupTagMapper groupTagMapper;
    @Resource
    private FileUtil fileUtil;
    @Resource(name = "warningImHttpClient")
    private ImHttpClient warningImHttpClient;
    @DubboReference
    private PerWarningRalationRpcService perWarningRalationRpcService;

    @Resource
    private DepartmentLocationService departmentLocationService;

    @Resource
    private OrganizationDiversionService organizationDiversionService;

    @Override
    public void deleteByPostId(List<Long> postIds) {
        var collaborationPostGroups = collaborationPostGroupMapper.listByPostIds(postIds);
        delete(collaborationPostGroups);
    }

    private void delete(List<CollaborationPostGroup> collaborationPostGroups) {
        if (collaborationPostGroups != null && !collaborationPostGroups.isEmpty()) {
            var now = System.currentTimeMillis();
            var collect = collaborationPostGroups.stream().filter(a -> a.getSupportUserId() != null).map(
                a -> new CooperationUserGroupSupportUserReq(a.getGroupId(), a.getPostId(), a.getSupportUserId(), 1, now,
                    null, null)).collect(Collectors.toList());
            if (!collect.isEmpty()) {
                imHttpClient.groupSupport(collect);
            }
            collaborationPostGroupMapper.deleteBatchIds(
                collaborationPostGroups.stream().map(CollaborationPostGroup::getId).collect(Collectors.toList()));
        }
    }

    @Override
    public void changeUserStatus(Integer status, Long userId) {
        if (status == 0) {
            addSupport(userId);
        } else {
            removeSupport(userId);
        }
    }

    @Override
    public void changeUserStatus(Integer status, Long userId, List<CollaborationPost> collaborationPosts) {
        if (status == 0) {
            addSupport(userId, collaborationPosts);
        } else {
            removeSupport(userId);
        }
    }

    @Override
    public void addSupport(List<Long> userId, CollaborationPost post) {
        if (userId.isEmpty()) {
            log.warn("empty user");
            return;
        }
        var onlines = collaborationAttendanceSwitchMapper.selectList(
            Wrappers.lambdaQuery(CollaborationAttendanceSwitch.class)
                .in(CollaborationAttendanceSwitch::getPersonId, userId)
                .eq(CollaborationAttendanceSwitch::getSwitchStatus, 0));
        if (onlines == null || onlines.isEmpty()) {
            log.warn("no online user");
            return;
        }
        if (post.getType() == 0) {
            var freeUser = pickFreeUser(onlines, null, post);
            var groups = collaborationPostGroupMapper.listNoSupportedByPosts(List.of(post));
            if (groups == null || groups.isEmpty()) {
                log.info("user post:{} has no group to support", freeUser);
                return;
            }
            log.info("add support:{} {}", freeUser, groups);
            var now = System.currentTimeMillis();
            var req = transform(groups, freeUser, now, 0);
            // 调接口
            log.info("group support:{}", req);
            imHttpClient.groupSupport(req);
            collaborationPostGroupMapper.setUser(groups, freeUser);
        } else {
            //1:14e太慢了转异步处理
            var groups = collaborationPostGroupMapper.listNoSupportedByPosts(List.of(post));
            for (var group : groups) {
                streamBridge.send(ASYNC_BIND_SUPPORT_CHANNEL, group);
            }
        }
    }

    @Override
    public void addSupport(Long userId) {
        log.info("add support:{}", userId);
        // 上岗
        var collaborationPosts = collaborationPostMapper.listByUserId(userId + "");
        doAddSupport(userId, collaborationPosts);
    }

    @Override
    public void addSupport(Long userId, List<CollaborationPost> collaborationPosts) {
        log.info("add support:{}", userId);
        // 上岗
        doAddSupport(userId, collaborationPosts);
    }

    private void doAddSupport(Long userId, List<CollaborationPost> collaborationPosts) {
        if (collaborationPosts == null || collaborationPosts.isEmpty()) {
            log.warn("user {} has no post", userId);
            return;
        }
        var groups = collaborationPostGroupMapper.listNoSupportedByPosts(collaborationPosts);
        if (groups == null || groups.isEmpty()) {
            log.info("user post:{} has no group to support", userId);
            return;
        }
        var now = System.currentTimeMillis();
        var req = transform(groups, userId, now, 0);
        // 调接口
        log.info("group support:{}", req);
        try {
            imHttpClient.groupSupport(req);
        } catch (Exception e) {
             // 支撑失败，要把状态改为未上岗，否则上岗就没有上岗记录
            UpdateWrapper<CollaborationAttendanceSwitch> wrapper = new UpdateWrapper<>();
            wrapper.eq("person_id", userId);
            CollaborationAttendanceSwitch attendanceSwitch = new CollaborationAttendanceSwitch();
            attendanceSwitch.setSwitchStatus(1);
            collaborationAttendanceSwitchMapper.update(attendanceSwitch, wrapper);
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.contains("同一群组的协同岗不能由同一人支撑")) {
                throw new BusinessException("上岗失败：您绑定的多个协同岗支撑了同一群组，无法同时上岗。请联系管理员处理。");
            }
            throw new RuntimeException(e);
        }
        collaborationPostGroupMapper.setUser(groups, userId);
    }

    @Override
    public void removeSupport(Long userId) {
        log.info("del support:{}", userId);
        var supportGroups = collaborationPostGroupMapper.listByUserId(userId);
        if (supportGroups == null || supportGroups.isEmpty()) {
            log.info("user {} has no support group", userId);
            return;
        }
        var now = System.currentTimeMillis();
        var downs = transform(supportGroups, userId, now, 1);
        log.info("group support:{}", downs);
        imHttpClient.groupSupport(downs);
        // 下岗
        collaborationPostGroupMapper.update(null,
            Wrappers.lambdaUpdate(CollaborationPostGroup.class).eq(CollaborationPostGroup::getSupportUserId, userId)
                .set(CollaborationPostGroup::getSupportUserId, null));
        for (var supportGroup : supportGroups) {
            // 删除当前绑定的以后要异步整个人绑上去
            streamBridge.send(ASYNC_BIND_SUPPORT_CHANNEL, supportGroup);
        }
    }

    private List<CooperationUserGroupSupportUserReq> transform(List<CollaborationPostGroup> supportGroups, Long userId,
        Long now, Integer status) {
        return supportGroups.stream().map(
                a -> new CooperationUserGroupSupportUserReq(a.getGroupId(), a.getPostId(), userId, status, now, null, null))
            .collect(Collectors.toList());
    }

    @Bean("asyncPostPick")
    public Consumer<CollaborationPostGroup> asyncPostPick() {
        return postGroup -> {
            try {
                log.info("on message:{}", postGroup);
                Long supportUserId = postGroup.getSupportUserId();
                var pg = collaborationPostGroupMapper.selectById(postGroup.getId());
                if (pg.getSupportUserId() != null) {
                    log.warn("group already supported:{}", pg);
                    return;
                }
                var postById = collaborationPostMapper.selectById(postGroup.getPostId());

                List<String> userIds = Optional.ofNullable(postById).map(CollaborationPost::getRelatedUserIds).map(a -> a.split(","))
                        .stream().flatMap(Arrays::stream).filter(a -> !a.isBlank()).collect(Collectors.toList());
                if (userIds.isEmpty()) {
                    log.warn("Collaboration not found");
                    return;
                }
                // 过滤掉自己本人
                userIds = userIds.stream()
                        .filter(userid->!userid.equals(String.valueOf(supportUserId)))
                        .collect(Collectors.toList());

                if (userIds.isEmpty()) {
                    log.warn("Collaboration not found");
                    return;
                }

                List<CollaborationAttendanceSwitch> onlines = collaborationAttendanceSwitchMapper.selectList(
                        Wrappers.lambdaQuery(CollaborationAttendanceSwitch.class)
                                .in(CollaborationAttendanceSwitch::getPersonId, userIds)
                                .eq(CollaborationAttendanceSwitch::getSwitchStatus, 0));
                if (onlines == null || onlines.isEmpty()) {
                    log.info("post:{} has no online user", postGroup);
                    return;
                }
                var pick = pickFreeUser(onlines, postGroup.getGroupId(), postById);
                var req = new CooperationUserGroupSupportUserReq(postGroup.getGroupId(), postGroup.getPostId(), pick, 0,
                    System.currentTimeMillis(), null, null);
                imHttpClient.groupSupport(List.of(req));
                collaborationPostGroupMapper.update(null, Wrappers.lambdaUpdate(CollaborationPostGroup.class)
                    .eq(CollaborationPostGroup::getGroupId, postGroup.getGroupId())
                    .eq(CollaborationPostGroup::getPostId, postGroup.getPostId())
                    .set(CollaborationPostGroup::getSupportUserId, pick));
            } catch (Exception e) {
                log.error("consume error", e);
            }
        };
    }

    private Long pickFor1o1p4b(List<CollaborationAttendanceSwitch> onlines, Long groupId, CollaborationPost post) {
        //1：14e特殊处理
        var groupVo = imHttpClient.queryGroupDetail(groupId);
        var imUsers = imHttpClient.userPage(null,
            onlines.stream().map(CollaborationAttendanceSwitch::getPersonId).map(Object::toString)
                .collect(Collectors.joining(",")));
        var userDepartment = Optional.ofNullable(groupVo.getGroupMembers()).stream().flatMap(Collection::stream)
            .filter(a -> a.getRole() == 2).findAny().map(a -> imHttpClient.userPage(a.getIdCard(), null))
            .map(UserGetVo::getResults).map(a -> a.get(0)).map(ImUser::getUserDepartments).map(a -> a.get(0));
        if (userDepartment.isEmpty()) {
            return null;
        }
        var departmentCode = userDepartment.get().getDepartmentCode();

        //        var imDepartments = imHttpClient.queryDepartment(departmentCode);
        // 兼容老方案
        var imDepartment = organizationDiversionService.findOne(departmentCode);
        var depIds = Arrays.stream(imDepartment.getFullPath().split(",")).filter(a -> !a.isBlank()).map(Long::parseLong)
            .collect(Collectors.toList());
        for (int i = depIds.size() - 1; i >= 0; i--) {
            var departmentId = depIds.get(i);
            for (var imUser : imUsers.getResults()) {
                if (imUser.getUserDepartments() != null && imUser.getUserDepartments().stream()
                    .map(ImUser.UserDepartment::getId).anyMatch(a -> Objects.equals(a, departmentId))) {
                    log.info("pick for 1o1p4b:{} {} {}", onlines, post, groupVo);
                    return imUser.getId();
                }
            }
        }
        return null;
    }

    private Long pickFreeUser(List<CollaborationAttendanceSwitch> onlines, Long groupId, CollaborationPost post) {
        Long id;
        if (post.getType() == 1 && (id = pickFor1o1p4b(onlines, groupId, post)) != null) {
            return id;
        }
        var userCnts = collaborationPostGroupMapper.cntUserByPost(post.getId());
        // try none
        if (userCnts == null || userCnts.isEmpty()) {
            log.info("pick for none:{} {}", onlines, post);
            return onlines.get(0).getPersonId();
        }
        var busyIds =
            userCnts.stream().map(CollaborationPostGroupMapper.UserCnt::getSupportUserId).collect(Collectors.toList());
        for (var online : onlines) {
            if (!busyIds.contains(online.getPersonId())) {
                log.info("pick for free:{} in {} {}", online.getPersonId(), onlines, post);
                return online.getPersonId();
            }
        }
        // try less
        userCnts.sort(Comparator.comparingInt(CollaborationPostGroupMapper.UserCnt::getCnt));
        var onlineIds = onlines.stream().map(CollaborationAttendanceSwitch::getPersonId).collect(Collectors.toList());
        for (var userCnt : userCnts) {
            if (onlineIds.contains(userCnt.getSupportUserId())) {
                log.info("pick for less:{} in {} {}", userCnt.getSupportUserId(), onlines, post);
                return userCnt.getSupportUserId();
            }
        }
        log.info("pick for default:{}  {}", onlines, post);
        return onlines.get(0).getPersonId();
    }

    @Bean("collaborationMessage")
    public Consumer<WsResponse> processWsMessage() {
        return wsResponse -> {
            try {
                if (!Objects.equals(wsResponse.getModule(), "group") || !Objects.equals(wsResponse.getNotifyType(),
                    "cooperation_user_support")) {
                    return;
                }
                log.info("on message:{}", wsResponse);
                var datas = JsonUtil.convert(wsResponse.getData(), new TypeReference<List<WsCollaborationMessage>>() {
                });
                var distinct = new HashSet<String>();
                for (var data : datas) {
                    if (!distinct.add(data.getPostId() + "-" + data.getGroupId())) {
                        log.info("conflict:{}", data);
                        continue;
                    }
                    if (data.getOperType() == 1) {
                        // 新增协同岗群组关系
                        var exists =
                            collaborationPostGroupMapper.getOneByPostIdAndGroupId(data.getPostId(), data.getGroupId());
                        if (exists.isEmpty()) {
                            var collaborationPostGroup =
                                new CollaborationPostGroup(idWorker.nextId(), data.getPostId(), data.getGroupId(),
                                    null);
                            collaborationPostGroupMapper.insert(collaborationPostGroup);
                            streamBridge.send(ASYNC_BIND_SUPPORT_CHANNEL, collaborationPostGroup);
                        } else if (exists.get().getSupportUserId() == null) {
                            streamBridge.send(ASYNC_BIND_SUPPORT_CHANNEL, exists.get());
                        } else {
                            // NOPE
                        }
                    } else {
                        collaborationPostGroupMapper.deleteByPostIdAndGroupId(data.getPostId(), data.getGroupId());
                    }
                }
            } catch (Exception e) {
                log.error("consume error", e);
            }
        };
    }

    @Bean("collaborationGroupChangeMessage")
    public Consumer<WsResponse> processWsChangeGroup() {
        return wsResponse -> {
            try {
                if (!Objects.equals(wsResponse.getModule(), "group") || !Objects.equals(wsResponse.getNotifyType(),
                    "groupNotice")) {
                    return;
                }
                log.info("processWsChangeGroup on message:{}", wsResponse);
                var data = JsonUtil.convert(wsResponse.getData(), new TypeReference<WsCollaborationChangeMessage>() {
                });
                var distinct = new HashSet<String>();
                if (!distinct.add(data.getOperateId() + "-" + data.getGroupId())) {
                    log.info("processWsChangeGroup conflict:{}", data);
                    return;
                }
                cleanupWarningGroupRelation(data);
                if (data.getOperationType() == 7) {
                    log.info("警信解散群组，标记为已解散，groupId:{}", data.getGroupId());
                    CreateGroup createGroup = new CreateGroup();
                    createGroup.setExisted(0);
                    createGroup.setUpdateTime(new Date());
                    UpdateWrapper<CreateGroup> updateWrapper = new UpdateWrapper<>();
                    updateWrapper.eq("group_id", data.getGroupId());
                    createGroupMapper.update(createGroup, updateWrapper);
//                    QueryWrapper<CreateGroup> queryWrapper = new QueryWrapper<>();
//                    createGroupMapper.delete(queryWrapper.eq("group_id", data.getGroupId()));
//                    //删除关注的群组
//                    QueryWrapper<UserGroupCare> userGroupCareQueryWrapper = new QueryWrapper<>();
//                    userGroupCareMapper.delete(userGroupCareQueryWrapper.eq("group_id", data.getGroupId()));
//                    //删除打过标签的群组
//                    QueryWrapper<GroupTag> groupTagQueryWrapper = new QueryWrapper<>();
//                    groupTagMapper.delete(groupTagQueryWrapper.eq("group_id", data.getGroupId()));
                } else if (data.getOperationType() == 6) {
                    QueryWrapper<CreateGroup> queryWrapper = new QueryWrapper<>();
                    var initGroupNum =
                            createGroupMapper.selectCount(queryWrapper.eq("group_id", data.getGroupId()));
                    if(initGroupNum>0){
                        log.info("警信群组已存在，不做处理");
                        return;
                    }
                    try {
                        GroupVo groupVo = imHttpClient.queryGroupDetail(data.getGroupId());
                        // 新增记录创建群组记录
                        CreateGroup createGroup = new CreateGroup();
                        createGroup.setGroupId(groupVo.getId());
                        createGroup.setCreateTime(new Date(groupVo.getGmtCreated()));
                        createGroup.setUpdateTime(new Date(groupVo.getGmtModified()));
                        List<GroupMembers> groupMembers = groupVo.getGroupMembers();
                        log.info("groupMembers:{}", groupMembers);
                        if (ListUtils.isNotBlankList(groupMembers)) {
                            for (GroupMembers groupMember : groupMembers) {
                                if (groupMember.getRole().equals(2)) {
                                    log.info("groupMember.getUserId():{}", groupMember.getUserId());
                                    createGroup.setOwnerId(groupMember.getUserId() + "");
                                    log.info("groupMember.getName():{}", groupMember.getName());
                                    createGroup.setOwnerName(groupMember.getName());
                                    UserGetVo userGetVo = imHttpClient.userPage(groupMember.getIdCard(), null);
                                    if (userGetVo.getResults() != null && userGetVo.getResults().size() > 0) {
                                        ImUser imUser = userGetVo.getResults().get(0);
                                        List<ImUser.UserDepartment> userDepartments = imUser.getUserDepartments();
                                        for (ImUser.UserDepartment department : userDepartments) {
                                            log.info("department.getDepartmentCode():{}",
                                                department.getDepartmentCode());
                                            if (department.getIsPrimary()) {
                                                String departmentCode = department.getDepartmentCode();
                                                if (StringUtils.isNotBlank(departmentCode)) {
                                                    DepartmentLocation departmentLocation =
                                                        departmentLocationService.findByDeptCode(departmentCode);
                                                    createGroup.setLocation(Objects.nonNull(departmentLocation)
                                                        ? departmentLocation.getLocation() : null);
                                                }
                                                createGroup.setDepartmentId(department.getDepartmentCode());
                                                createGroup.setDepartmentName(department.getDepartmentName());
                                            }
                                        }
                                    }
                                }
                            }
                            createGroup.setGroupName(StringUtils.isNotBlank(groupVo.getName()) ? groupVo.getName()
                                : groupVo.getUndefinedName());
                            createGroup.setAvatar(groupVo.getAvatar());
                            createGroup.setAvatarImg(fileUtil.downloadSaveIcon(imHttpClient, groupVo.getAvatar()));

                            List<Long> userIdList =
                                groupMembers.stream().map(GroupMembers::getUserId).collect(Collectors.toList());
                            List<String> nameList =
                                groupMembers.stream().map(GroupMembers::getName).collect(Collectors.toList());

                            createGroup.setUserIds(CollectionUtils.join(userIdList, ","));
                            createGroup.setUserNames(CollectionUtils.join(nameList, ","));
                            // pc自定义建群这俩没有值
                            //                            createGroup.setUserIds(groupVo.getUndefinedNameIds());
                            //                            createGroup.setUserNames(groupVo.getUndefinedName());
                            createGroup.setId(idWorker.nextId());
                            // 区分普通建群(0)和PC自定义建群(3)：PC自定义建群 undefinedName 和 undefinedNameIds 均为空
                            boolean isCustomGroup = StringUtils.isBlank(groupVo.getUndefinedName())
                                && StringUtils.isBlank(groupVo.getUndefinedNameIds());
                            int source = isCustomGroup ? 3 : 0;
                            createGroup.setSource(source);
                            var groupNum =
                                createGroupMapper.selectCount(queryWrapper.eq("group_id", data.getGroupId()));
                            log.info("groupNum:{}, source:{}", groupNum, source);
                            if (groupNum == 0L) {
                                // INSERT 失败(重复键)时不更新 source，避免覆盖其他路径(LabelServiceImpl/ImCommonServiceImpl)已写入的正确值
                                createGroupMapper.upsert(createGroup, null);
                            }
                            GroupExtends existing = groupExtendsMapper.selectByGroupId(groupVo.getId());
                            if (existing == null) {
                                GroupExtends groupExtends = new GroupExtends();
                                groupExtends.setGmtCreated(LocalDateTime.now());
                                groupExtends.setGroupId(groupVo.getId());
                                groupExtends.setArchived(0);
                                Integer type = groupVo.getType();
                                groupExtends.setGroupType(type == 3 ? 2 : 1);
                                groupExtendsMapper.insertIgnoreDuplicated(groupExtends);
                            }
                        }

                    } catch (Exception e) {
                        log.error(e.getMessage());
                    }
                } else if (data.getOperationType() == 5) {
                    LambdaQueryWrapper<CreateGroup> lambdaQuery = new LambdaQueryWrapper<>();
                    List<CreateGroup> createGroups =
                        createGroupMapper.selectList(lambdaQuery.eq(CreateGroup::getGroupId, data.getGroupId()));
                    log.info(" processWsChangeGroup createGroups:{}", createGroups);
                    if (CollectionUtils.isEmpty(createGroups)) {
                        log.info("processWsChangeGroup createGroups:{}", data.getGroupId());
                        return;
                    }
                    try {
                        CreateGroup createGroup = new CreateGroup();
                        List<WsCollaborationChangeMember> memberList = data.getMemberList();
                        log.info("processWsChangeGroup memberList:{}", memberList);
                        if (ListUtils.isNotBlankList(memberList)) {
                            for (WsCollaborationChangeMember member : memberList) {
                                if (member.getRole().equals(2)) {
                                    log.info("processWsChangeGroup member UserId:{}", member.getUserId());
                                    createGroup.setOwnerId(member.getUserId() + "");
                                    log.info("processWsChangeGroup member Name():{}", member.getName());
                                    createGroup.setOwnerName(member.getName());
                                    createGroup.setUpdateTime(new Date(member.getGmtModified()));
                                    
                                    // 群主转让时更新位置信息
                                    updateGroupLocationByOwner(createGroup, member.getUserId());
                                }
                            }
                            UpdateWrapper<CreateGroup> updateWrapper = new UpdateWrapper<>();
                            updateWrapper.eq("id", createGroups.get(0).getId());
                            createGroupMapper.update(createGroup, updateWrapper);
                        }
                    } catch (Exception e) {
                        log.error(e.getMessage());
                    }
                } else if (data.getOperationType() == 1 || data.getOperationType() == 2 || data.getOperationType() == 3 || data.getOperationType() == 4) {
                    LambdaQueryWrapper<CreateGroup> lambdaQuery = new LambdaQueryWrapper<>();
                    List<CreateGroup> createGroups =
                        createGroupMapper.selectList(lambdaQuery.eq(CreateGroup::getGroupId, data.getGroupId()));
                    log.info(" processWsChangeGroup createGroups:{}", createGroups);
                    if (CollectionUtils.isEmpty(createGroups)) {
                        log.info("processWsChangeGroup createGroups:{}", data.getGroupId());
                        return;
                    }
                    try {
                        List<WsCollaborationChangeMember> noticeMemberList = data.getMemberList();
                        // 仅更新需要修改的字段（例如：只更新 groupName 和 updateTime）
                        CreateGroup createGroup = new CreateGroup();

                        if (data.getNewGroupProfile() != null
                            && StringUtils.isNotBlank(data.getNewGroupProfile().getName())) {
                            createGroup.setGroupName(data.getNewGroupProfile().getName());
                        }
                        UpdateWrapper<CreateGroup> updateWrapper = new UpdateWrapper<>();
                        updateWrapper.eq("id", createGroups.get(0).getId());
                        GroupVo groupVo = imHttpClient.queryGroupDetail(data.getGroupId());
                        if (groupVo != null && ListUtils.isNotBlankList(groupVo.getGroupMembers())) {
                            List<Long> userIdList = groupVo.getGroupMembers().stream().map(GroupMembers::getUserId)
                                .collect(Collectors.toList());
                            List<String> nameList = groupVo.getGroupMembers().stream().map(GroupMembers::getName)
                                .collect(Collectors.toList());

                            createGroup.setUserIds(CollectionUtils.join(userIdList, ","));
                            createGroup.setUserNames(CollectionUtils.join(nameList, ","));
                            var owner = CollectionUtils.find(groupVo.getGroupMembers(), GroupMembers::getRole, 2);
                            createGroup.setOwnerId(owner.getUserId() + "");
                            createGroup.setOwnerName(owner.getName());
                            List<WsCollaborationChangeMember> memberList = new ArrayList<>();
                            Set<Long> userIdSet = new HashSet<>();
                            for (Long userId : userIdList) {
                                userIdSet.add(userId);
                                WsCollaborationChangeMember member = new WsCollaborationChangeMember();
                                member.setUserId(userId);
                                memberList.add(member);
                            }
                            List<String> postMapMembers = collaborationPostMapper.selectMemberByPostIds(new ArrayList<>(userIdSet));
                            postMapMembers.forEach(m -> {
                                String[] ids = m.split(",");
                                for (String id : ids) {
                                    WsCollaborationChangeMember member = new WsCollaborationChangeMember();
                                    member.setUserId(Long.parseLong(id.trim()));
                                    memberList.add(member);
                                }
                            });
                            data.setMemberList(memberList);
                        }
                        if(groupVo != null){
                            String name = groupVo.getName();
                            String undefinedName = groupVo.getUndefinedName();
                            if(StringUtils.isBlank(name) && StringUtils.isNotBlank(undefinedName)){
                                 createGroup.setGroupName(undefinedName);
                            }
                        }
                        if (ObjectUtils.isNotEmpty(createGroup)) {
                            createGroup.setUpdateTime(new Date());
                            createGroupMapper.update(createGroup, updateWrapper);
                            // 发消息给前端，通知群消息变更了只会
                            sendToCAgent(data, noticeMemberList);
                        }
                    } catch (Exception e) {
                        log.error(e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.error("processWsChangeGroup consume error", e);
            }
        };
    }

    private void updateGroupLocationByOwner(CreateGroup createGroup, Long userId) {
        try {
            var userGetVo = imHttpClient.userPage(null, userId.toString());
            if (userGetVo == null || ListUtils.isBlankList(userGetVo.getResults())) {
                return;
            }
            var imUser = userGetVo.getResults().get(0);
            var primaryDept = imUser.getPrimaryDepartment();
            if (primaryDept == null || StringUtils.isBlank(primaryDept.getDepartmentCode())) {
                return;
            }
            var deptLocation = departmentLocationMapper.selectOne(
                Wrappers.lambdaQuery(DepartmentLocation.class)
                    .eq(DepartmentLocation::getDepartmentCode, primaryDept.getDepartmentCode()));
            if (deptLocation == null || StringUtils.isBlank(deptLocation.getLocation())) {
                return;
            }
            createGroup.setLocation(deptLocation.getLocation());
            log.info("Group owner transferred, updated location for userId: {}, departmentCode: {}, location: {}",
                userId, primaryDept.getDepartmentCode(), deptLocation.getLocation());
        } catch (Exception e) {
            log.warn("Failed to update location when group owner transferred, userId: {}", userId, e);
        }
    }

    /**
     * 移除预警信息
     * 1、群解散时移除
     * 2、预警助手被移除出群时移除
     * @param data
     */
    private void cleanupWarningGroupRelation(WsCollaborationChangeMessage data) {
        if (data == null || data.getGroupId() == null) {
            return;
        }
        // 群解散要关联删除
        if (Objects.equals(data.getOperationType(), 7)) {
            deleteGroupWarningRelation(data.getGroupId(), "group_dismissed");
            return;
        }
        if (!warningProxyUserRemoved(data)) {
            return;
        }
        deleteGroupWarningRelation(data.getGroupId(), "warning_user_removed");
    }

    private boolean warningProxyUserRemoved(WsCollaborationChangeMessage data) {
        if (CollectionUtils.isEmpty(data.getMemberList())) {
            return false;
        }
        Long warningProxyUserId = warningImHttpClient.getProxyUserId();
        return data.getMemberList().stream()
            .anyMatch(member -> Boolean.TRUE.equals(member.getIsDel())
                && Objects.equals(member.getUserId(), warningProxyUserId));
    }

    private void deleteGroupWarningRelation(Long groupId, String reason) {
        try {
            int deleted = perWarningRalationRpcService.deleteGroupWarningRalation(groupId);
            log.info("cleanup warning relation by group change, groupId:{}, reason:{}, deleted:{}",
                groupId, reason, deleted);
        } catch (Exception e) {
            log.error("cleanup warning relation by group change failed, groupId:{}, reason:{}", groupId, reason, e);
        }
    }

    private void sendToCAgent(WsCollaborationChangeMessage data,List<WsCollaborationChangeMember> memberList) {
        if (CollectionUtils.isEmpty(memberList)) {
            return;
        }
        List<@NotNull String> userIds = memberList.stream().filter(e -> {
            Boolean isDel = e.getIsDel();
            return !isDel;
        }).map(e-> String.valueOf(e.getUserId())
        ).collect(Collectors.toList());
        if(CollectionUtils.isNotEmpty(userIds)){
            var cagentMqFrame = new CagentMqFrame().toBuilder().typeSubSystemMessage("CLOUDCMD_IM_JINGXIN")
                    .unicast().userIds(userIds).build()
                    .body("CLOUDCMD_IM_JINGXIN", "group_update", data).build();
            streamBridge.send("cloudcmd-cagent", cagentMqFrame);
        }
    }

    public void syncPostGroup(List<UserListVo> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        var exists = Optional.ofNullable(collaborationPostGroupMapper.selectList(Wrappers.lambdaQuery()));

        var postGroups = users.stream().flatMap(
            ui -> Optional.ofNullable(ui.getGroups()).stream().flatMap(Collection::stream).map(
                group -> new CollaborationPostGroup(idWorker.nextId(), Long.parseLong(ui.getId()), group.getGroupId(),
                    group.getSupportUserId() == 0L ? null : group.getSupportUserId()))).collect(Collectors.toList());
        collaborationPostGroupMapper.delete(Wrappers.lambdaQuery());
        for (var postGroup : postGroups) {
            collaborationPostGroupMapper.insert(postGroup);
            GroupVo groupVo = imHttpClient.queryGroupDetail(postGroup.getGroupId());

            // 问题单号：9901，已冻结的群不同步过来了
            Integer status = groupVo.getStatus();
            boolean noNeedSync = Objects.nonNull(status) && 2 == status;
            if (noNeedSync) {
                continue;
            }

            // 同步增加创群记录
            QueryWrapper<CreateGroup> queryWrapper = new QueryWrapper<>();
            var groupNum = createGroupMapper.selectCount(queryWrapper.eq("group_id", postGroup.getGroupId()));
            log.info("groupNum:{}", groupNum);
            if (groupNum == 0L) {
                // 新增记录创建群组记录
                try {
                    CreateGroup createGroup = new CreateGroup();
                    createGroup.setGroupId(groupVo.getId());
                    createGroup.setAvatar(groupVo.getAvatar());
                    createGroup.setAvatarImg(fileUtil.downloadSaveIcon(imHttpClient, groupVo.getAvatar()));
                    createGroup.setCreateTime(new Date(groupVo.getGmtCreated()));
                    createGroup.setUpdateTime(new Date(groupVo.getGmtModified()));
                    List<GroupMembers> groupMembers = groupVo.getGroupMembers();
                    log.info("groupMembers:{}", groupMembers);
                    if (ListUtils.isNotBlankList(groupMembers)) {
                        for (GroupMembers groupMember : groupMembers) {
                            if (groupMember.getRole().equals(2)) {
                                log.info("groupMember.getUserId():{}", groupMember.getUserId());
                                createGroup.setOwnerId(groupMember.getUserId() + "");
                                log.info("groupMember.getName():{}", groupMember.getName());
                                createGroup.setOwnerName(groupMember.getName());
                                UserGetVo userGetVo = imHttpClient.userPage(groupMember.getIdCard(), null);
                                if (userGetVo.getResults() != null && userGetVo.getResults().size() > 0) {
                                    ImUser imUser = userGetVo.getResults().get(0);
                                    List<ImUser.UserDepartment> userDepartments = imUser.getUserDepartments();
                                    for (ImUser.UserDepartment department : userDepartments) {
                                        log.info("department.getDepartmentCode():{}", department.getDepartmentCode());
                                        if (department.getIsPrimary()) {
                                            createGroup.setDepartmentId(department.getDepartmentCode());
                                            createGroup.setDepartmentName(department.getDepartmentName());
                                        }
                                    }
                                }
                            }
                        }
                        List<Long> userIdList = groupVo.getGroupMembers().stream().map(GroupMembers::getUserId)
                            .collect(Collectors.toList());
                        List<String> nameList =
                            groupVo.getGroupMembers().stream().map(GroupMembers::getName).collect(Collectors.toList());

                        createGroup.setUserIds(CollectionUtils.join(userIdList, ","));
                        createGroup.setUserNames(CollectionUtils.join(nameList, ","));
                        createGroup.setId(idWorker.nextId());
                        createGroup.setGroupName(
                            StringUtils.isNotBlank(groupVo.getName()) ? groupVo.getName() : groupVo.getUndefinedName());
                        createGroupMapper.upsert(createGroup, null);
                        GroupExtends existing = groupExtendsMapper.selectByGroupId(groupVo.getId());
                        if (existing == null) {
                            GroupExtends groupExtends = new GroupExtends();
                            groupExtends.setGmtCreated(LocalDateTime.now());
                            groupExtends.setGroupId(groupVo.getId());
                            groupExtends.setArchived(0);
                            Integer type = groupVo.getType();
                            groupExtends.setGroupType(type == 3 ? 2 : 1);
                            groupExtendsMapper.insertIgnoreDuplicated(groupExtends);
                        }
                    }
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            } else {
                QueryWrapper<CreateGroup> oldQueryWrapper = new QueryWrapper<>();
                oldQueryWrapper.eq("group_id", postGroup.getGroupId());
                CreateGroup createGroup = createGroupMapper.selectOne(oldQueryWrapper);
                createGroup.setGroupName(
                    StringUtils.isNotBlank(groupVo.getName()) ? groupVo.getName() : groupVo.getUndefinedName());
                createGroup.setAvatar(groupVo.getAvatar());
                createGroup.setAvatarImg(fileUtil.downloadSaveIcon(imHttpClient, groupVo.getAvatar()));
                createGroup.setUpdateTime(new Date());
                createGroupMapper.updateById(createGroup);
            }
        }
        log.info("sync post group done,exists:{},sync size:{}", exists.map(List::size).orElse(0), postGroups.size());
    }

    @Override
    public List<Long> postsByGroupId(Long groupId) {
        var list = collaborationPostGroupMapper.selectList(
            Wrappers.lambdaQuery(CollaborationPostGroup.class).eq(CollaborationPostGroup::getGroupId, groupId));
        return Optional.ofNullable(list).stream().flatMap(Collection::stream).map(CollaborationPostGroup::getPostId)
            .distinct().collect(Collectors.toList());
    }

    @Override
    public void removeSupport(Long userId, Long postId) {
        log.info("del support:{}", userId);
        var supportGroups = collaborationPostGroupMapper.listByUserId(userId);
        if (supportGroups == null || supportGroups.isEmpty()) {
            log.info("user {} has no support group", userId);
            return;
        }
        // 过滤出当前协同岗所在的群组进行下岗通知
        supportGroups = supportGroups.stream().filter(group -> group.getPostId().equals(postId)).collect(Collectors.toList());
        if (supportGroups.isEmpty()) {
            log.info("user {} has no support group", userId);
            return;
        }
        var now = System.currentTimeMillis();
        var downs = transform(supportGroups, userId, now, 1);
        log.info("group support:{}", downs);
        imHttpClient.groupSupport(downs);
        // 只下岗该人员协同岗所在的群组
        collaborationPostGroupMapper.update(null,
                Wrappers.lambdaUpdate(CollaborationPostGroup.class)
                        .eq(CollaborationPostGroup::getSupportUserId, userId)
                        .eq(CollaborationPostGroup::getPostId, postId)
                        .set(CollaborationPostGroup::getSupportUserId, null));
        for (var supportGroup : supportGroups) {
            // 删除当前绑定的以后要异步整个人绑上去但是不能是自己
            streamBridge.send(ASYNC_BIND_SUPPORT_CHANNEL, supportGroup);
        }
    }
}