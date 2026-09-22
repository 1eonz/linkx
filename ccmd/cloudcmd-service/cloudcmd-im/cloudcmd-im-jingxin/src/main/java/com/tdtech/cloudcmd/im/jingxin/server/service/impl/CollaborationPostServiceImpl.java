package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.tdtech.cloudcmd.bean.CagentMqFrame;
import com.tdtech.cloudcmd.bean.MsgBody;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.CollaborationAttendance;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.CollaborationTasksCountVO;
import com.tdtech.cloudcmd.im.jingxin.client.ImHttpClient;
import com.tdtech.cloudcmd.im.jingxin.client.entity.*;
import com.tdtech.cloudcmd.im.jingxin.server.entity.*;
import com.tdtech.cloudcmd.im.jingxin.server.enums.CollaborationTaskStatusEnum;
import com.tdtech.cloudcmd.im.jingxin.server.enums.LogTypeEnum;
import com.tdtech.cloudcmd.im.jingxin.server.enums.SwitchTypeEnum;
import com.tdtech.cloudcmd.im.jingxin.server.service.*;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.*;
import com.tdtech.cloudcmd.im.jingxin.server.util.FileUtil;
import com.tdtech.cloudcmd.msip.aop.LogReport;
import com.tdtech.cloudcmd.msip.aop.LogReportParam;
import com.tdtech.cloudcmd.msip.enums.OperationTypeEnum;
import com.tdtech.cloudcmd.mysql.entity.CcmdPage;
import com.tdtech.cloudcmd.mysql.entity.CcmdPageParam;
import com.tdtech.cloudcmd.redis.RedisUtil;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.ListUtils;
import com.tdtech.cloudcmd.util.json.JsonUtil;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * @author lsc
 * @date 2025/7/14
 **/
@Slf4j
@Service
public class CollaborationPostServiceImpl extends ServiceImpl<CollaborationPostMapper, CollaborationPost>
    implements CollaborationPostService {

    private static final String SYNC_HIS_POST_FLAG_KEY = "sync_his_post_flag";
    private static final String HAS_SYNC = "0";
    private static final String ALL_USER_V2_KEY = "cloudcmd:im:user:all";
    private static final String SELECTION_TYPE_COOP_LEVEL = "coopLevel";
    private static final String SELECTION_TYPE_FUNCTIONAL_DEPARTMENT = "functionalDepartment";


    @Resource
    private CollaborationPostMapper collaborationPostMapper;
    @Resource
    private CollaborationPostGroupServiceImpl collaborationPostGroupService;
    @Resource
    private CollaborationPostGroupMapper collaborationPostGroupMapper;
    @Resource
    private CollaborationAttendanceSwitchMapper collaborationAttendanceSwitchMapper;
    @Resource
    private CommonFlagMapper commonFlagMapper;
    @Resource
    private CoopLevelMemberMapper coopLevelMemberMapper;
    @Resource
    private FunctionalDepartmentCoopMapper functionalDepartmentCoopMapper;
    @Resource
    private CollaborationAttendanceMapper collaborationAttendanceMapper;
    @Resource
    private CollaborationAttendanceService collaborationAttendanceService;
    @Resource
    private ImService imService;
    @Resource
    private FileUtil fileUtil;
    @Resource
    private ImHttpClient imHttpClient;
    @Resource
    private StreamBridge streamBridge;
    @Resource
    private LabelService labelService;
    @Resource
    private ICollaborationTaskResponseService collaborationTaskResponseService;
    @Resource
    private CollaborationPostLogService collaborationPostLogService;
    @Resource
    private PoliceTicketTypeService policeTicketTypeService;

    @Resource
    private ICooperationUnattendedService cooperationUnattendedService;

    @Resource
    private OrganizationDiversionService organizationDiversionService;

    @Resource
    private CollaborationAttendanceSwitchMapper attendanceSwitchMapper;

    @Resource
    private CollaborationTaskMapper collaborationTaskMapper;

    @Resource
    private ImCommonService imCommonService;

    @Autowired
    private RedisUtil redisUtil;

    private boolean SYNC_POST_FROM_IM = true;

    @Nullable
    private static List<String> splitOrgStr(String orgId) {
        List<String> orgArr;
        if (orgId != null && !orgId.isBlank()) {
            orgArr = Arrays.asList(orgId.split(","));
        } else {
            orgArr = null;
        }
        return orgArr;
    }

    private LinkedHashMap<Long, String> buildUidNameMap(List<CollaborationPost> collaborationPosts) {
        return collaborationPosts.stream().map(CollaborationPost::toUidNameMap)//
            .collect(Collector.<Map<Long, String>, LinkedHashMap<Long, String>>of(//
                LinkedHashMap::new, //
                LinkedHashMap::putAll, //
                (a, b) -> {
                    a.putAll(b);
                    return a;
                }));
    }

    private Map<Long, List<CollaborationPost>> buildUid2PostListMap(List<CollaborationPost> collaborationPosts) {
        // 一个人可能关联俩个协同岗（普通协同岗、人员核查协同岗）,这里需要根据关联人，去查处这些人还没有关联其他协同岗
        List<String> userIdList = collaborationPosts.stream()
            .map(post -> Arrays.stream(post.getRelatedUserIds().split(",")).collect(Collectors.toList()))
            .flatMap(List::stream).distinct().collect(Collectors.toList());
        return buildUserIdListMap(userIdList);
    }

    public Map<Long, List<CollaborationPost>> buildUserIdListMap(List<String> userIdList) {
        log.info("buildUidPostMap userIdList: {}", userIdList);
        List<CollaborationPost> postList = collaborationPostMapper.findPostList(userIdList);
        return buildMap(postList);
    }

    @NotNull
    private static Map<Long, List<CollaborationPost>> buildMap(List<CollaborationPost> postList) {
        var userId2PostsMap = postList.stream()//
            .flatMap(post -> post.getUids().stream()//
                .map(uid -> new AbstractMap.SimpleImmutableEntry<>(uid, post)))//
            .collect(//
                Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
        log.info("buildUidPostMap userId2PostsMap: {}", userId2PostsMap);
        return userId2PostsMap;
    }

    @Override
    public Page<CollaborationPost> getPage(int pageNum, int pageSize, String name, String orgName, Long orgId,
        String relatedUserNames, String startTime, String endTime, Integer type) {
        //        var orgArr = splitOrgStr(orgId);
        // 2026-01-23前端有调整，只传最顶层的id，需要看到本级及其下级的数据
        /* 权限控制，暂时删除
        List<Long> orgIdSet;
        Set<String> orgs = imCommonService.getUserDepartments();
        Set<Long> allowOrgIds = orgs.stream().map(Long::parseLong).collect(Collectors.toSet());
        if (orgId != null) {
            List<Long> orgIds =
                    organizationDiversionService.queryDepartmentForListById(orgId).stream().map(ImDepartment::getId)
                            .collect(Collectors.toList());
            orgIdSet = orgIds.stream().filter(allowOrgIds::contains).collect(Collectors.toList());
            if (orgIdSet.isEmpty()) {
                return new Page<>();
            }
        } else {
            orgIdSet = new ArrayList<>(allowOrgIds);
        }*/
        List<Long> orgIds =
                organizationDiversionService.queryDepartmentForListById(orgId).stream().map(ImDepartment::getId)
                        .collect(Collectors.toList());

        Page<CollaborationPost> result = collaborationPostMapper.selectPageWithCondition(new Page<>(pageNum, pageSize), name, orgName, orgIds,
            relatedUserNames, startTime, endTime, type);
        return result;
    }

    @Override
    public List<CollaborationPost> listByUserId(String userId) {
        return collaborationPostMapper.listByUserId(userId);
    }

    @Override
    public List<CollaborationPost> queryByName(String name) {
        return collaborationPostMapper.selectList(new QueryWrapper<CollaborationPost>().eq("post_name", name).eq("deleted", 0));
    }

    @Override
    public List<CooperationUserVO> queryByUserId(String userId) {
        if (StringUtils.isBlank(userId)) {
            UserInfo user = SecurityUtils.getUser();
            if (Objects.isNull(user)) {
                throw new SecurityUtils.UnAuthException("access token invalid");
            }
            userId = String.valueOf(user.getUserId());
        }
        List<CollaborationPost> posts = collaborationPostMapper.selectList(new LambdaQueryWrapper<CollaborationPost>()
                .eq(CollaborationPost::getDeleted, 0)
                .apply("FIND_IN_SET({0}, related_user_ids) > 0", userId)
        );
        if (CollectionUtils.isEmpty(posts)) {
            return Collections.emptyList();
        }
        List<CooperationUserVO> cooperationUserVOS = new ArrayList<>();
        List<Long> postIds = posts.stream().map(CollaborationPost::getId).collect(Collectors.toList());
        List<CollaborationPostGroup> postGroups = collaborationPostGroupMapper.selectList(Wrappers.lambdaQuery(CollaborationPostGroup.class)
                .in(CollaborationPostGroup::getPostId, postIds));
        Map<Long, List<Long>> postIdGroupsMap = postGroups.stream()
                .collect(Collectors.groupingBy(CollaborationPostGroup::getPostId,
                        Collectors.mapping(CollaborationPostGroup::getGroupId, Collectors.toList())));
        for (CollaborationPost post : posts) {
            CooperationUserVO cooperationUserVO = new CooperationUserVO();
            cooperationUserVO.setUserId(post.getId());
            cooperationUserVO.setName(post.getPostName());
            cooperationUserVO.setGroupIds(postIdGroupsMap.getOrDefault(post.getId(), new ArrayList<>()));
            //isdn先不查，后续有需要再查
            cooperationUserVOS.add(cooperationUserVO);
        }
        return cooperationUserVOS;

    }

    @Override
    public Set<Long> listBoundUserIds(Integer type) {
        LambdaQueryWrapper<CollaborationPost> queryWrapper = new LambdaQueryWrapper<CollaborationPost>()
                .eq(Objects.nonNull(type), CollaborationPost::getType, type)
                .eq(CollaborationPost::getDeleted, 0);
        List<CollaborationPost> collaborationPosts = collaborationPostMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(collaborationPosts)) {
            return Collections.emptySet();
        }
        return collaborationPosts.stream()
                .map(CollaborationPost::getRelatedUserIds)
                .filter(StringUtils::isNotBlank)
                .flatMap(ids -> Arrays.stream(ids.split(",")))
                .filter(s -> !s.isBlank())
                .map(Long::parseLong)
                .collect(Collectors.toSet());
    }

    @Override
    public CcmdPage<CollaborationPost> listByDepartmentCode(CcmdPageParam pageParam, String orgCode, Long groupId) {
        return collaborationPostMapper.selectPageX(pageParam, Wrappers.lambdaQuery(CollaborationPost.class)
            .eq(orgCode != null && !orgCode.isBlank(), CollaborationPost::getOrgCode, orgCode)
            .inSql(groupId != null, CollaborationPost::getId,
                "select post_id from tb_collaboration_post_group where group_id=" + groupId));
    }

    @Override
    public Set<Long> selectedPostIds(List<Long> postIds, String selectionType, String selectionId) {
        if (CollectionUtils.isEmpty(postIds) || StringUtils.isBlank(selectionType)) {
            return Collections.emptySet();
        }
        if (SELECTION_TYPE_COOP_LEVEL.equals(selectionType)) {
            return coopLevelMemberMapper.selectList(Wrappers.lambdaQuery(CoopLevelMemberDO.class)
                    .select(CoopLevelMemberDO::getCoopUserId)
                    .in(CoopLevelMemberDO::getCoopUserId, postIds)
                    .eq(CoopLevelMemberDO::getIsDeleted, 0))
                .stream().map(CoopLevelMemberDO::getCoopUserId).collect(Collectors.toSet());
        }
        if (SELECTION_TYPE_FUNCTIONAL_DEPARTMENT.equals(selectionType)) {
            if (StringUtils.isBlank(selectionId)) {
                return Collections.emptySet();
            }
            Long deptId = parseLongSelectionId(selectionType, selectionId);
            return functionalDepartmentCoopMapper.selectList(Wrappers.lambdaQuery(FunctionalDepartmentCoop.class)
                    .select(FunctionalDepartmentCoop::getUserId)
                    .eq(FunctionalDepartmentCoop::getDeptId, deptId)
                    .in(FunctionalDepartmentCoop::getUserId, postIds)
                    .eq(FunctionalDepartmentCoop::getIsDeleted, 0))
                .stream().map(FunctionalDepartmentCoop::getUserId).collect(Collectors.toSet());
        }
        log.warn("unsupported collaboration post selection type: {}", selectionType);
        return Collections.emptySet();
    }

    private Long parseLongSelectionId(String selectionType, String selectionId) {
        try {
            return Long.valueOf(selectionId);
        } catch (NumberFormatException e) {
            throw new BusinessException("selectionId参数错误，selectionType=" + selectionType);
        }
    }

    /**
     * 发送协同岗开关给到客户端
     *
     * @param dataMap
     */
    private void sendCollaborationPostStatusToCagent(Map<String, String> dataMap) {
        CagentMqFrame frame = new CagentMqFrame();
        frame.setSubsystem("CLOUDCMD_IM_JINGXIN");
        frame.setType(Short.parseShort("2"));
        String tokenKey = "*-*";
        frame.setTokenkey(tokenKey);
        MsgBody<Map<String, String>> body = new MsgBody<>();
        body.setModule("CLOUDCMD_IM_JINGXIN");
        body.setNotifyType("collaboration_update");
        body.setData(dataMap);
        frame.setBody(body);
        try {
            var msg = JsonUtil.toJsonStr(frame);
            log.info("sendToCAgent:" + msg);
            streamBridge.send("cloudcmd-cagent", MessageBuilder.withPayload(msg).build());
        } catch (Exception e) {
            log.error("send message:{} failed", frame, e);
        }
    }

    @Override
    public boolean save(CollaborationPost collaborationPost) {
        if (collaborationPost.getUids().size() != collaborationPost.getUnames().size()) {
            throw new BusinessException("参数错误");
        }
        // 设置默认值（如果需要）
        if (collaborationPost.getOperateTime() == null) {
            collaborationPost.setOperateTime(new Date());
        }
        if (collaborationPost.getUpdateTime() == null) {
            collaborationPost.setUpdateTime(new Date());
        }

        // 插入数据库
        collaborationPostMapper.insert(collaborationPost);
        // 新增操作日志
        saveInsertPostLog(collaborationPost);
        // TODO 推送消息给前端
        Map<String, String> sendMap = new HashMap<>();
        sendMap.put("personId", String.valueOf(collaborationPost.getRelatedUserIds()));
        sendMap.put("type", "create");
        sendCollaborationPostStatusToCagent(sendMap);
        // imService.queryUser()
        return true;
    }

    @Override
    public boolean savePost(UserCreateRequestBody userCreateRequestBody, CollaborationPost collaborationPost) {
        try {
            Long id = imHttpClient.insertCollborationUser(userCreateRequestBody);
            // 新增协同岗获取id
            // 新增成功后，将id绑定到协同岗实体中
            collaborationPost.setId(id);
            save(collaborationPost);
            judgeWarn(collaborationPost);
        } catch (Exception e) {
            throw new BusinessException(e.getMessage());
        }
        return true;
    }

    private void judgeWarn(CollaborationPost post) {
        List<Long> userIdList = post.getUids();
        Map<Long, Integer> userSwitchMap = attendanceSwitchMapper.getByPersonIds(userIdList)
                .stream()
                .collect(Collectors.toMap(CollaborationAttendanceSwitch::getPersonId, CollaborationAttendanceSwitch::getSwitchStatus));
        Long onLineUserCount = onLineUserCount(userSwitchMap, userIdList);
        boolean warn = Objects.isNull(onLineUserCount) || onLineUserCount.longValue() == 0;
        if (warn) {
            cooperationUnattendedService.saveUnattendedRecord(post.getId());
        }
    }

    private Long onLineUserCount(Map<Long, Integer> userSwitchMap, List<Long> userIdList) {
        return userIdList.stream().filter(userId -> {
            Integer switchValue = userSwitchMap.get(userId);
            return Objects.nonNull(switchValue) && 0 == switchValue;
        }).count();

    }

    @Override
    public void checkPostParam(CollaborationPost collaborationPost) {
        String[] userIdArray = collaborationPost.getRelatedUserIds().split(",");
        String[] userNameArray = collaborationPost.getRelatedUserNames().split(",");

        List<String> errorUserNameList = new ArrayList<>();
        for (int i = 0; i < userIdArray.length; i++) {
            List<CollaborationPost> postList = collaborationPostMapper.listByUserId(userIdArray[i]);
            log.debug("checkPostParam userId: {}, postList: {}", userIdArray[i], postList);
            if (CollectionUtils.isEmpty(postList)) {
                continue;
            }
            Optional<CollaborationPost> any = postList.stream().filter(post -> {
                boolean typeEq = post.getType().equals(collaborationPost.getType());
                if (Objects.nonNull(collaborationPost.getId())) {
                    boolean idNotEq = !post.getId().equals(collaborationPost.getId());
                    return typeEq && idNotEq;
                }
                return typeEq;
            }).findAny();
            if (any.isPresent()) {
                errorUserNameList.add(userNameArray[i]);
            }
        }
        if (CollectionUtils.isNotEmpty(errorUserNameList)) {
            String errorUserNames = StringUtils.join(errorUserNameList, ",");
            String errorMsg = String.format("%s已绑定协同岗", errorUserNames);
            throw new BusinessException(errorMsg);
        }
    }

    private void saveInsertPostLog(CollaborationPost collaborationPost) {
        UserInfo user = SecurityUtils.getUser();
        if (Objects.isNull(user)) {
            throw new SecurityUtils.UnAuthException("access token invalid");
        }
        collaborationPostLogService.saveLog(collaborationPost, LogTypeEnum.INSERT.getCode(),
            user.getUserName() + "新建了协同岗");
    }

    private void saveUpdatePostLog(CollaborationPost oldPost, CollaborationPost newPost, List<Long> newTypeIds) {
        UserInfo user = SecurityUtils.getUser();
        if (Objects.isNull(user)) {
            throw new SecurityUtils.UnAuthException("access token invalid");
        }
        // 简单处理，可以使用自定义注解比对哪些字段发生了变化
        List<String> messageList = new ArrayList<>();
        String content = "";
        String oldPostName = oldPost.getPostName();
        String newPostName = newPost.getPostName();
        if (!StringUtils.equals(oldPostName, newPostName)) {
            messageList.add(String.format("%s修改了协同岗名称为: %s", user.getUserName(), newPostName));
        }
        String oldOrgName = oldPost.getOrgName();
        String newOrgName = newPost.getOrgName();
        if (!StringUtils.equals(oldOrgName, newOrgName)) {
            messageList.add(String.format("%s修改了协同岗组织为: %s", user.getUserName(), newOrgName));
        }
        String oldRelatedUserNames = oldPost.getRelatedUserNames();
        String newRelatedUserNames = newPost.getRelatedUserNames();
        if (!StringUtils.equals(oldRelatedUserNames, newRelatedUserNames)) {
            messageList.add(String.format("%s修改了协同岗关联人员为: %s", user.getUserName(), newRelatedUserNames));
        }
        Integer oldType = oldPost.getType();
        if (!oldType.equals(newPost.getType())) {
            String msg = "普通";
            if (newPost.getType().equals(1)) {
                msg = "人员核查";
            }
            messageList.add(String.format("%s修改了协同岗类型为: %s", user.getUserName(), msg));
        }
        String oldFileId = oldPost.getFileId();
        String newFileId = newPost.getFileId();
        if (!StringUtils.equals(oldFileId, newFileId)) {
            messageList.add(String.format("%s修改了协同岗图标", user.getUserName()));
        }
        List<Long> oldTypeIds = policeTicketTypeService.getTypeIds(oldPost.getId());
        boolean isEqual = oldTypeIds.containsAll(newTypeIds) && newTypeIds.containsAll(oldTypeIds);
        if (!isEqual) {
            String newTagNames = "空";
            if (CollectionUtils.isNotEmpty(newTypeIds)) {
                List<PoliceTicketType> typeList = policeTicketTypeService.findByIdList(newTypeIds);
                newTagNames = typeList.stream().map(PoliceTicketType::getTag).collect(Collectors.joining(","));
            }
            messageList.add(String.format("%s修改了警单类型为: %s", user.getUserName(), newTagNames));
        }
        if (CollectionUtils.isNotEmpty(messageList)) {
            content = messageList.stream().collect(Collectors.joining(","));
            collaborationPostLogService.saveLog(newPost, LogTypeEnum.UPDATE.getCode(), content);
        }
    }

    private void saveDeletePostLog(CollaborationPost collaborationPost) {
        UserInfo user = SecurityUtils.getUser();
        if (Objects.isNull(user)) {
            throw new SecurityUtils.UnAuthException("access token invalid");
        }
        collaborationPostLogService.saveLog(collaborationPost, LogTypeEnum.DELETE.getCode(),
            user.getUserName() + "删除了协同岗");
    }

    /**
     * 修正历史回复记录的部门信息
     */
    private void updateTaskResponseDepartment(CollaborationPost collaborationPost) {
        CollaborationTaskResponse response = new CollaborationTaskResponse();
        response.setDepartmentId(collaborationPost.getOrgId());
        response.setDepartmentName(collaborationPost.getOrgName());

        LambdaQueryWrapper<CollaborationTaskResponse> queryWrapper =
            new LambdaQueryWrapper<CollaborationTaskResponse>().eq(CollaborationTaskResponse::getPostId,
                collaborationPost.getId());
        collaborationTaskResponseService.update(response, queryWrapper);
    }

    @Override
    public boolean update(CollaborationPost collaborationPost, List<Long> typeIds) {
        if (collaborationPost.getUids().size() != collaborationPost.getUnames().size()) {
            throw new BusinessException("参数错误");
        }
        var old = collaborationPostMapper.selectById(collaborationPost.getId());
        // 设置更新时间
        collaborationPost.setUpdateTime(new Date());
        // 修改协同岗信息时，对历史的任务回复记录中的组织信息同步修改
        updateTaskResponseDepartment(collaborationPost);
        // 修改协同岗信息时，对历史的协同岗记录中的协同岗名称同步修改
        QueryWrapper<CollaborationAttendance> queryWrapper = new QueryWrapper<>();
        List<CollaborationAttendance> attendances = collaborationAttendanceMapper.selectList(
            queryWrapper.eq("post_id", collaborationPost.getId()).orderByDesc("create_time"));
        if (ListUtils.isNotBlankList(attendances)) {
            for (CollaborationAttendance collaborationAttendance : attendances) {
                collaborationAttendance.setPostName(collaborationPost.getPostName());
            }
            collaborationAttendanceService.updateBatchById(attendances);
        }

        // 用户不在了的要搞一手群组绑定
        var newUsers = Optional.ofNullable(collaborationPost.getRelatedUserIds()).stream().map(a -> a.split(","))
            .flatMap(Arrays::stream).filter(a -> !a.isBlank()).map(Long::parseLong).collect(Collectors.toList());
        var oldUsers =
            Optional.ofNullable(old.getRelatedUserIds()).stream().map(a -> a.split(",")).flatMap(Arrays::stream)
                .filter(a -> !a.isBlank()).map(Long::parseLong).collect(Collectors.toList());
        var delUsers = oldUsers.stream().filter(a -> !newUsers.contains(a)).collect(Collectors.toList());
        var idAndNameMap = old.toUidNameMap();
        idAndNameMap.putAll(collaborationPost.toUidNameMap());

        if (CollectionUtils.isNotEmpty(delUsers)) {

            log.info("delUsers start: {}", System.currentTimeMillis());

            List<String> userIdList = delUsers.stream().map(String::valueOf).collect(Collectors.toList());
            var uidPostMap = buildUserIdListMap(userIdList);
            for (var delUser : delUsers) {
                collaborationPostGroupService.removeSupport(delUser);
                //支撑两个群及以上的人 不改状态
                if (uidPostMap.containsKey(delUser) && uidPostMap.get(delUser).size() > 1) {
                    continue;
                }

                // 判断用户是否在岗，若在岗，则下岗
                CollaborationAttendanceSwitch attendanceSwitch =
                    collaborationAttendanceSwitchMapper.getByPersonId(delUser);
                if (attendanceSwitch != null && attendanceSwitch.getSwitchStatus() == 0) {
                    collaborationAttendanceSwitchMapper.update(null,
                        Wrappers.lambdaUpdate(CollaborationAttendanceSwitch.class)
                            .eq(CollaborationAttendanceSwitch::getPersonId, delUser)
                            .set(CollaborationAttendanceSwitch::getSwitchStatus, 1)
                            .set(CollaborationAttendanceSwitch::getSwitchType, SwitchTypeEnum.OTHER.getCode()));
                    Map<String, String> sendMap = new HashMap<>();
                    sendMap.put("personId", String.valueOf(delUser));
                    sendMap.put("type", collaborationPost.getType() + "");
                    collaborationAttendanceService.sendSwitchStatusToCagent(sendMap);

                    List<CollaborationPost> postList = uidPostMap.get(delUser);
                    if (CollectionUtils.isNotEmpty(postList)) {
                        postList.stream().forEach(post -> {
                            CollaborationAttendance attendance =
                                    CollaborationAttendance.builder().orgId(post.getOrgId())
                                            .orgName(post.getOrgName()).postId(post.getId())
                                            .postName(post.getPostName()).personId(delUser)
                                            .personName(idAndNameMap.get(delUser)).switchType(SwitchTypeEnum.OTHER.getCode())
                                            .build();
                            collaborationAttendanceService.saveRecord(attendance, 1);
                        });
                    }
                }
            }
            log.info("delUsers end: {}", System.currentTimeMillis());
        }

        var addUsers = newUsers.stream().filter(a -> !oldUsers.contains(a)).collect(Collectors.toList());
        if (!addUsers.isEmpty()) {
            log.info("addUsers start: {}", System.currentTimeMillis());
            collaborationPostGroupService.addSupport(addUsers, collaborationPost);
            for (var addUser : addUsers) {
                // 判断用户是否在岗，若在岗，则添加上岗记录
                CollaborationAttendanceSwitch attendanceSwitch =
                    collaborationAttendanceSwitchMapper.getByPersonId(addUser);
                if (attendanceSwitch != null && attendanceSwitch.getSwitchStatus() == 0) {
                    CollaborationAttendance attendance =
                        CollaborationAttendance.builder().orgId(collaborationPost.getOrgId())
                            .orgName(collaborationPost.getOrgName()).postId(collaborationPost.getId())
                            .postName(collaborationPost.getPostName()).personId(addUser)
                            .personName(idAndNameMap.get(addUser)).switchType(SwitchTypeEnum.OTHER.getCode()).build();
                    collaborationAttendanceService.saveRecord(attendance, 0);
                }
            }
            log.info("addUsers end: {}", System.currentTimeMillis());
        }
        // 使用 MyBatis Plus 更新
        var cnt = collaborationPostMapper.updateById(collaborationPost);
        // 新增操作日志
        saveUpdatePostLog(old, collaborationPost, typeIds);
        // 发cagent消息
        sendCollaborationPostStatusToCagent(
            Map.of("personId", collaborationPost.getRelatedUserIds(), "type", "update"));
        return cnt > 0;
    }

    @Override
    public boolean delete(Long id) {
        // 使用 MyBatis Plus 删除
        // TODO 推送消息给前端
        CollaborationPost collaborationPost = collaborationPostMapper.selectById(id);
        return delete(collaborationPost);
    }

    @Override
    @LogReport(type = OperationTypeEnum.COLLABORATION_POST_DELETE)
    public boolean delete(@LogReportParam(field = "postName") CollaborationPost collaborationPost) {
        Long id = collaborationPost.getId();
        Map<String, String> sendMap = new HashMap<>();
        sendMap.put("personId", String.valueOf(collaborationPost.getRelatedUserIds()));
        sendMap.put("type", "delete");
        sendCollaborationPostStatusToCagent(sendMap);
        var uidNameMap = collaborationPost.toUidNameMap();
        var uidPostMap = buildUid2PostListMap(List.of(collaborationPost));

        Optional.ofNullable(collaborationAttendanceSwitchMapper.getByPersonIds(collaborationPost.getUids()))//
            .stream()//
            .flatMap(Collection::stream)//
            .forEach(attendanceSwitch -> {
                if (attendanceSwitch == null) {
                    return;
                }
                //支撑两个群及以上的人 不改状态
                if (uidPostMap.containsKey(attendanceSwitch.getPersonId()) && uidPostMap.get(
                    attendanceSwitch.getPersonId()).size() > 1) {
                    return;
                }
                if (attendanceSwitch.getSwitchStatus() == 0) {
                    collaborationAttendanceSwitchMapper.update(null, Wrappers//
                        .lambdaUpdate(CollaborationAttendanceSwitch.class)//
                        .eq(CollaborationAttendanceSwitch::getPersonId, attendanceSwitch.getPersonId())//
                        .set(CollaborationAttendanceSwitch::getSwitchStatus, 1)
                        .set(CollaborationAttendanceSwitch::getSwitchType, SwitchTypeEnum.OTHER.getCode()));
                    collaborationAttendanceService.sendSwitchStatusToCagent(
                        Map.of("personId", String.valueOf(attendanceSwitch.getPersonId()), "type", "下岗"));
                    var userPostList = uidPostMap.get(attendanceSwitch.getPersonId());
                    userPostList.forEach(post -> {
                        CollaborationAttendance attendance =
                            CollaborationAttendance.builder().orgId(post.getOrgId()).orgName(post.getOrgName())
                                .postId(post.getId()).postName(post.getPostName())
                                .personId(attendanceSwitch.getPersonId())
                                .personName(uidNameMap.get(attendanceSwitch.getPersonId()))
                                .switchType(SwitchTypeEnum.OTHER.getCode()).build();
                        collaborationAttendanceService.saveRecord(attendance, 1);
                    });
                }
            });
        var i = collaborationPostMapper.deleteById(id);
        // 新增操作日志
        saveDeletePostLog(collaborationPost);
        //        List<Label> labelsByCollaborationPostId = labelService.getLabelsByCollaborationPostId(id);
        // 同步删除标签绑定的协同岗
        labelService.removePostIdFromBindings(id);
        collaborationPostGroupService.deleteByPostId(List.of(id));
        return i > 0;
    }

    @Override
    public boolean deleteBatch(List<Long> ids) {
        // TODO 推送消息给前端
        List<CollaborationPost> collaborationPosts = collaborationPostMapper.selectBatchIds(ids);
        return deleteBatchPost(collaborationPosts);
    }

    @Override
    @LogReport(type = OperationTypeEnum.COLLABORATION_POST_DELETE)
    public boolean deleteBatchPost(@LogReportParam(field = "postName") List<CollaborationPost> dataList) {
        List<String> collect = dataList.stream().map(CollaborationPost::getRelatedUserIds).collect(Collectors.toList());
        List<Long> ids = dataList.stream().map(CollaborationPost::getId).collect(Collectors.toList());
        Map<String, String> sendMap = new HashMap<>();
        sendMap.put("personId", String.join(",", collect));
        sendMap.put("type", "delete");
        sendCollaborationPostStatusToCagent(sendMap);
        var uids = dataList.stream().map(CollaborationPost::getUids).flatMap(Collection::stream).distinct()
            .collect(Collectors.toList());
        var uidNameMap = buildUidNameMap(dataList);
        var uidPostMap = buildUid2PostListMap(dataList);
        var suidPostMap = buildMap(dataList);
        Optional.ofNullable(collaborationAttendanceSwitchMapper.getByPersonIds(uids))//
            .stream()//
            .flatMap(Collection::stream)//
            .forEach(attendanceSwitch -> {
                if (attendanceSwitch == null) {
                    return;
                }
                //支撑两个群及以上的人 不改状态
                if (uidPostMap.containsKey(attendanceSwitch.getPersonId()) && uidPostMap.get(
                    attendanceSwitch.getPersonId()).size() > suidPostMap.get(attendanceSwitch.getPersonId()).size()) {
                    return;
                }
                if (attendanceSwitch.getSwitchStatus() == 0) {
                    collaborationAttendanceSwitchMapper.update(null, Wrappers//
                        .lambdaUpdate(CollaborationAttendanceSwitch.class)//
                        .eq(CollaborationAttendanceSwitch::getPersonId, attendanceSwitch.getPersonId())//
                        .set(CollaborationAttendanceSwitch::getSwitchStatus, 1)
                        .set(CollaborationAttendanceSwitch::getSwitchType, SwitchTypeEnum.OTHER.getCode()));
                    collaborationAttendanceService.sendSwitchStatusToCagent(
                        Map.of("personId", String.valueOf(attendanceSwitch.getPersonId()), "type", "下岗"));
                    var userPostList = uidPostMap.get(attendanceSwitch.getPersonId());
                    userPostList.forEach(post -> {
                        CollaborationAttendance attendance =
                            CollaborationAttendance.builder().orgId(post.getOrgId()).orgName(post.getOrgName())
                                .postId(post.getId()).postName(post.getPostName())
                                .personId(attendanceSwitch.getPersonId())
                                .personName(uidNameMap.get(attendanceSwitch.getPersonId()))
                                .switchType(SwitchTypeEnum.OTHER.getCode()).build();
                        collaborationAttendanceService.saveRecord(attendance, 1);
                    });
                }
            });
        var i = collaborationPostMapper.deleteBatchIds(ids);
        for (var post : dataList) {
            // 新增操作日志
            saveDeletePostLog(post);
        }
        for (var id : ids) {
            labelService.removePostIdFromBindings(id);
        }
        collaborationPostGroupService.deleteByPostId(ids);
        return i > 0;
    }

    @Override
    public List<ImUser> queryUser(String code, Integer includeChildren, String keywords, String name) {
        List<ImUser> imUserList = imService.queryUser(code, includeChildren, keywords, null, name);
        List<CollaborationPost> collaborationPosts = collaborationPostMapper.selectList(new QueryWrapper<CollaborationPost>().eq("deleted", 0));
        List<String> relatedUserIds =
            collaborationPosts.stream().map(CollaborationPost::getRelatedUserIds).collect(Collectors.toList());
        List<String> userIds = new ArrayList<>();
        for (String relatedUserId : relatedUserIds) {
            if (StringUtils.isNotBlank(relatedUserId)) {
                String[] split = relatedUserId.split(",");
                Arrays.stream(split).forEach(a -> {
                    if (!userIds.contains(a)) {
                        userIds.add(a);
                    }
                });
            }
        }
        for (ImUser imUser : imUserList) {
            if (userIds.contains(String.valueOf(imUser.getId()))) {
                imUser.setIsBinding(1);
            }
        }
        return imUserList;
    }

    @Override
    public ImPage<ImUser> queryUserByPage(String code, Integer type, Integer pageNum, Integer pageSize,
                                          Integer includeChildren, String keywords, String name) {
        ImPage<ImUser> imUserImPage = imService.queryUserByPage(code, pageNum, pageSize, includeChildren, keywords, name);

        // 只查询 relatedUserIds 一列，避免整行大字段加载
        LambdaQueryWrapper<CollaborationPost> queryWrapper = new LambdaQueryWrapper<CollaborationPost>()
                .select(CollaborationPost::getRelatedUserIds)
                .eq(Objects.nonNull(type), CollaborationPost::getType, type).eq(CollaborationPost::getDeleted, 0);
        List<CollaborationPost> collaborationPosts = collaborationPostMapper.selectList(queryWrapper);
        Set<String> userIds = new HashSet<>();
        for (CollaborationPost collaborationPost : collaborationPosts) {
            String relatedUserIds = collaborationPost.getRelatedUserIds();
            if (StringUtils.isNotBlank(relatedUserIds)) {
                String[] split = relatedUserIds.split(",");
                for (String a : split) {
                    if (StringUtils.isNotBlank(a)) {
                        userIds.add(a);
                    }
                }
            }
        }
        for (ImUser imUser : imUserImPage.getRecords()) {
            if (userIds.contains(String.valueOf(imUser.getId()))) {
                imUser.setIsBinding(1);
            }
        }
        return imUserImPage;
    }

    @Override
    public Integer syncPostFromIm(UserInfo user) {
        try {

            SYNC_POST_FROM_IM = false;
            // 读取全局同步标记位
            if (getSyncStatus()) {
                log.info("已同步过im协同岗");
                SYNC_POST_FROM_IM = true;
                // 已同步过直接返回
                return 0;
            }
            // 从im查询所有协同岗
            List<UserListVo> userListVos = imService.queryCollaborationPost();
            if (CollectionUtils.isEmpty(userListVos)) {
                log.info("im协同岗用户为空");
                SYNC_POST_FROM_IM = true;
                return 0;
            }
            // 过滤相同主键的记录
            List<Long> idList = collaborationPostMapper.listAllIds();
            if (CollectionUtils.isNotEmpty(idList)) {
                userListVos = userListVos.stream()
                    .filter(t -> StringUtils.isNotBlank(t.getId()) && !idList.contains(Long.valueOf(t.getId())))
                    .collect(Collectors.toList());
            }
            if (CollectionUtils.isEmpty(userListVos)) {
                log.info("im协同岗用户都已存在库中");
                SYNC_POST_FROM_IM = true;
                return 0;
            }
            // 查询im部门列表
            List<ImDepartment> imDepartments = imService.queryDepartmentForList(null);
            if (CollectionUtils.isEmpty(imDepartments)) {
                log.warn("同步im协同岗时获取全量im部门数据结果为空");
                SYNC_POST_FROM_IM = true;
                return 0;
            }
            Map<Long, ImDepartment> deptMap = imDepartments.stream()
                .collect(Collectors.toMap(ImDepartment::getId, Function.identity(), (o1, o2) -> o1));

            List<CollaborationPost> posts = new ArrayList<>();
            for (UserListVo userVo : userListVos) {
                CollaborationPost post = CollaborationPost.from(userVo);
                ImDepartment dept = deptMap.get(post.getOrgId());
                post.setOrgCode(dept == null ? null : dept.getCode());
                post.setIconUrl(fileUtil.downloadSaveIcon(imHttpClient, post.getFileId()));
                collaborationPostLogService.saveLog(post, user, LogTypeEnum.INSERT.getCode(), "同步协同岗");
                posts.add(post);
            }
            // 批量插入记录
            int rows = collaborationPostMapper.insertBatch(posts);
            // 同步协同岗群组关系
            collaborationPostGroupService.syncPostGroup(userListVos);

            // 批量刷新勤务状态
            var existsSwitchs = collaborationAttendanceSwitchMapper.selectList(Wrappers.lambdaQuery());
            var existsUids = Optional.ofNullable(existsSwitchs).stream().flatMap(Collection::stream)
                .map(CollaborationAttendanceSwitch::getPersonId).collect(Collectors.toList());
            var newIds = new HashSet<Long>();
            var newSwitchs =
                userListVos.stream().map(CollaborationAttendanceSwitch::batchFrom).flatMap(Collection::stream)
                    .filter(a -> !existsUids.contains(a.getPersonId())).filter(a -> newIds.add(a.getPersonId()))
                    .collect(Collectors.toList());
            if (ListUtils.isNotBlankList(newSwitchs)) {
                // 避免重复
                collaborationAttendanceSwitchMapper.deleteBatchIds(newIds);
                collaborationAttendanceSwitchMapper.insertBatch(newSwitchs);
                // 同步数据的时候自动增加上岗记录
                for (CollaborationPost collaborationPost : posts) {
                    if (StringUtils.isNotBlank(collaborationPost.getRelatedUserIds())) {
                        String[] idArray = collaborationPost.getRelatedUserIds().split(",");
                        String[] nameArray = collaborationPost.getRelatedUserNames().split(",");
                        for (int i = 0; i < idArray.length; i++) {
                            CollaborationAttendance attendance =
                                CollaborationAttendance.builder().orgId(collaborationPost.getOrgId())
                                    .orgName(collaborationPost.getOrgName()).postId(collaborationPost.getId())
                                    .postName(collaborationPost.getPostName()).personId(Long.parseLong(idArray[i]))
                                    .personName(nameArray[i]).build();
                            collaborationAttendanceService.saveRecord(attendance, 0);

                        }

                    }
                }
            }

            // 设置全局同步标记位为已同步状态
            commonFlagMapper.insert(CommonFlag.builder().flagKey(SYNC_HIS_POST_FLAG_KEY).flagValue(HAS_SYNC)
                .operatorId(user == null ? null : user.getUserId())
                .operatorName(user == null ? null : user.getUserName()).build());
            SYNC_POST_FROM_IM = true;
            return rows;
        } catch (Exception e) {
            log.error("同步im协同岗失败", e);
            SYNC_POST_FROM_IM = true;
        }
        return 0;
    }

    @Override
    public boolean getProcess() {
        return SYNC_POST_FROM_IM;
    }

    @Override
    public Boolean getImSyncStatus() {
        return getSyncStatus();
    }

    /**
     * 获取从im同步协同岗状态
     *
     * @return true已同步，false位同步
     */
    private Boolean getSyncStatus() {
        // 读取全局同步标记位
        QueryWrapper<CommonFlag> wrapper = new QueryWrapper<>();
        wrapper.eq("flag_key", SYNC_HIS_POST_FLAG_KEY);
        CommonFlag commonFlag = commonFlagMapper.selectOne(wrapper);
        return commonFlag != null && StringUtils.isNotBlank(commonFlag.getFlagValue()) && commonFlag.getFlagValue()
            .equals(HAS_SYNC);
    }

    @Override
    public void exportToExcel(String fileName, String name, String orgName, String orgId, String relatedUserNames,
        String startTime, String endTime, HttpServletResponse response) throws IOException {
        var orgArr = splitOrgStr(orgId);
        List<CollaborationPost> list =
            collaborationPostMapper.selectListByCondition(name, orgName, orgArr, relatedUserNames, startTime, endTime);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-disposition",
            "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8") + ".xlsx");

        EasyExcel.write(response.getOutputStream(), CollaborationPost.class).sheet("协同岗列表").doWrite(list);
    }

    @Override
    public CollaborationPost getById(Long id) {
        return collaborationPostMapper.selectById(id);
    }

    @Override
    public List<CollaborationPost> getByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        return collaborationPostMapper.selectBatchIds(ids);
    }

    @Override
    public List<CollaborationPost> findList(List<Long> orgIdList) {
        if (CollectionUtils.isEmpty(orgIdList)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<CollaborationPost> queryWrapper =
            new LambdaQueryWrapper<CollaborationPost>().in(CollaborationPost::getOrgId, orgIdList);
        return list(queryWrapper);
    }

    @Override
    public List<CollaborationPost> findBatchContainsDeleted(List<Long> idList) {
        if (CollectionUtils.isEmpty(idList)) {
            return Collections.emptyList();
        }
        return collaborationPostMapper.findBatchContainsDeleted(idList);
    }

    @Override
    public List<CollaborationPost> findAll() {
        LambdaQueryWrapper<CollaborationPost> queryWrapper =
            new LambdaQueryWrapper<CollaborationPost>().in(CollaborationPost::getDeleted, 0);
        return list(queryWrapper);
    }

    @Override
    public List<ImUser> getUserList(List<String> userIdList) {
        List<ImUser> imUserList = redisUtil.get(ALL_USER_V2_KEY, new TypeReference<>() {
        });
        if (CollectionUtils.isEmpty(imUserList)) {
            return Collections.emptyList();
        }
        return imUserList.stream().filter(a -> userIdList.contains(String.valueOf(a.getId()))).collect(Collectors.toList());
    }

    @Override
    public void updateUserName(Long userId, String newName) {
        if (userId == null || StringUtils.isBlank(newName)) {
            return;
        }
        List<CollaborationPost> posts = collaborationPostMapper.listByUserId(String.valueOf(userId));
        for (CollaborationPost post : posts) {
            if (StringUtils.isBlank(post.getRelatedUserIds()) || StringUtils.isBlank(post.getRelatedUserNames())) {
                continue;
            }
            String[] ids = post.getRelatedUserIds().split(",");
            String[] names = post.getRelatedUserNames().split(",");
            StringBuilder newNames = new StringBuilder();
            boolean changed = false;
            for (int i = 0; i < ids.length; i++) {
                if (Long.parseLong(ids[i].trim()) == userId) {
                    newNames.append(newName);
                    changed = true;
                } else {
                    newNames.append(names[i]);
                }
                if (i < ids.length - 1) {
                    newNames.append(",");
                }
            }
            if (changed) {
                LambdaUpdateWrapper<CollaborationPost> wrapper = new LambdaUpdateWrapper<>();
                wrapper.eq(CollaborationPost::getId, post.getId())
                       .set(CollaborationPost::getRelatedUserNames, newNames.toString());
                collaborationPostMapper.update(null, wrapper);
            }
        }
    }

    @Override
    public CollaborationTasksCountVO getCollaborationTasksCount(Long collaborationId) {
        // 待办包含：1(待处理)、7(未及时回复)、8(已逾期)，统计时合并计入 pendingCount
        List<Integer> statuses = Arrays.stream(CollaborationTaskStatusEnum.values())
                .map(CollaborationTaskStatusEnum::getValue)
                .collect(Collectors.toList());
        List<CollaborationTask> taskList = collaborationTaskMapper.findListByCondition(statuses, null, Collections.singletonList(collaborationId), null, null, null);
        CollaborationTasksCountVO vo = new CollaborationTasksCountVO();
        if (CollectionUtils.isEmpty(taskList)) {
            return vo;
        }
        Map<Integer, Long> countMap = taskList.stream()
                .filter(a -> a != null && a.getStatus() != null)
                .collect(Collectors.groupingBy(CollaborationTask::getStatus, Collectors.counting()));
        vo.setPendingCount(countMap.getOrDefault(CollaborationTaskStatusEnum.PENDING.getValue(), 0L).intValue()
                + countMap.getOrDefault(CollaborationTaskStatusEnum.DELAY.getValue(), 0L).intValue()
                + countMap.getOrDefault(CollaborationTaskStatusEnum.OVERDUE.getValue(), 0L).intValue());
        vo.setTrackingCount(countMap.getOrDefault(CollaborationTaskStatusEnum.TRACKING.getValue(), 0L).intValue());
        vo.setFinishedCount(countMap.getOrDefault(CollaborationTaskStatusEnum.FINISHED.getValue(), 0L).intValue());
        vo.setNoNeedProcessCount(countMap.getOrDefault(CollaborationTaskStatusEnum.NO_NEED_REPLY.getValue(), 0L).intValue());
        return vo;
    }

}