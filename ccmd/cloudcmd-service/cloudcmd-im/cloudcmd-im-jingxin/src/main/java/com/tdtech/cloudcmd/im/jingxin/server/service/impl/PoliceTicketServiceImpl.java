package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.toolkit.MPJWrappers;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.tdtech.cloudcmd.bean.CagentMqFrame;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.*;
import com.tdtech.cloudcmd.im.jingxin.api.entity.polTicket.PoliceTicket;
import com.tdtech.cloudcmd.im.jingxin.api.entity.polTicket.PoliceTicketQO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.polTicket.PoliceTicketVO;
import com.tdtech.cloudcmd.im.jingxin.client.ImHttpClient;
import com.tdtech.cloudcmd.im.jingxin.client.entity.*;
import com.tdtech.cloudcmd.im.jingxin.server.aiclient.group.GroupAiClient;
import com.tdtech.cloudcmd.im.jingxin.server.entity.*;
import com.tdtech.cloudcmd.im.jingxin.server.enums.ImGroupTypeEnum;
import com.tdtech.cloudcmd.im.jingxin.server.service.CollaborationPostGroupService;
import com.tdtech.cloudcmd.im.jingxin.server.service.LabelService;
import com.tdtech.cloudcmd.im.jingxin.server.service.PoliceTicketService;
import com.tdtech.cloudcmd.im.jingxin.server.service.PoliceTicketTypeService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.*;
import com.tdtech.cloudcmd.im.jingxin.server.util.FileUtil;
import com.tdtech.cloudcmd.msip.enums.LicenseEnum;
import com.tdtech.cloudcmd.msip.util.LicenseUtil;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.IdWorker;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class PoliceTicketServiceImpl implements PoliceTicketService {
    private static final String LOCATION_REGEX =
            "^(-?((180(\\.\\d{1,18})?)|((1[0-7]\\d)|([1-9]?\\d))(\\.\\d{1,18})?))," + "(-?((90(\\.\\d{1,18})?)|([1-8]?\\d)(\\.\\d{1,18})?))$";
    private static final Pattern LOCATION_PATTERN = Pattern.compile(LOCATION_REGEX);
    private static final String ID_CARD_PATTERN = "^\\d{17}[\\dXx]$";
    @Resource
    private PoliceTicketMapper policeTicketMapper;
    @Resource
    private TrPoliceTicketGroupMapper policeTicketGroupMapper;
    @Resource
    private PoliceTicketTypeService policeTicketTypeService;
    @Resource
    protected CollaborationPostGroupService postGroupService;
    @Resource
    private IdWorker idWorker;
    /******** 一键建群专用 SRT *******/
    @Resource
    @Qualifier("coopImHttpClient")
    private ImHttpClient coopImHttpClient;
    @Resource
    private ImService imService;
    @Resource
    private CollaborationPostMapper collaborationPostMapper;
    @Resource
    private CollaborationAttendanceSwitchMapper attendanceSwitchMapper;
    @Resource
    private GroupAiClient groupAiClient;
    @Resource
    private DepartmentLocationMapper departmentLocationMapper;
    @Resource
    private CreateGroupMapper createGroupMapper;
    @Resource
    private GroupExtendsMapper groupExtendsMapper;
    @Resource
    private FileUtil fileUtil;
    @Resource
    private LicenseUtil licenseUtil;
    @Resource
    private PoliceTicketTypeMapper policeTicketTypeMapper;

    @Resource
    private OrganizationDiversionService organizationDiversionService;

    @Resource
    private LabelService labelService;

    @Resource
    private ImHttpClient  imHttpClient;

    @Resource(name = "creatGroupExecutorService")
    private TaskExecutor taskExecutor;

    @Resource
    private StreamBridge streamBridge;

    private static final String GROUP_CREATE = "GROUP_CREATE";
    private static final String MSG_TOPIC = "cloudcmd-cagent";

    /******** 一键建群专用 END *******/

    @Override
    public int create(PoliceTicket policeTicket) {
        policeTicket.setId(idWorker.nextId());
        PoliceTicketType policeTicketType = policeTicketTypeMapper.selectByTag(policeTicket.getTag());
        if (policeTicketType == null) {
            PoliceTicketType pt = new PoliceTicketType();
            pt.setTag(policeTicket.getTag());
            pt.setId(idWorker.nextId());
            pt.setCreator("admin");
            pt.setCreatorId(1L);
            pt.setGmtCreated(new Date());
            policeTicketTypeMapper.insert(pt);
        }
        return policeTicketMapper.insert(policeTicket);
    }

    @Override
    public PoliceTicket findById(Long id) {
        return policeTicketMapper.selectById(id);
    }

    @Override
    public int update(PoliceTicket policeTicket) {
        return policeTicketMapper.updateById(policeTicket);
    }

    @Override
    public int deleteById(Long id) {
        return policeTicketMapper.deleteById(id);
    }

    @Override
    public Page<PoliceTicketVO> findPage(Page<PoliceTicketVO> page, @NotNull PoliceTicketQO qo) {
        var wrapper = new MPJLambdaWrapper<>(PoliceTicket.class)//
                .selectAll(PoliceTicket.class).distinct();
        if (qo.getGroupId() != null) {

            wrapper = wrapper.selectAs("IF(tptg.id is null, 0, 1)", PoliceTicketVO::getBindFlag)//
                    .leftJoin(TrPoliceTicketGroup.class, "tptg", //
                            on -> on//
                                    .eq(TrPoliceTicketGroup::getTicketId, PoliceTicket::getId)//
                                    .eq(TrPoliceTicketGroup::getGroupId, qo.getGroupId()))//
                    .orderByDesc("bindFlag")//
                    .orderByDesc(PoliceTicket::getCreateTime);

            wrapper = wrapper//
                    .and(qo.getBindFlag() != null && qo.getBindFlag() == 0, and -> {
                        var collect = findTagsOrDefault(qo.getGroupId(), List.of("__NOPE__"), qo.getGroupType());
                        and.isNull(TrPoliceTicketGroup::getId)//
                                .in(PoliceTicket::getTag, collect);
                    })//
                    .and(qo.getBindFlag() == null, and -> {
                        and.isNotNull(TrPoliceTicketGroup::getId)//
                                .or(or -> {
                                    var collect = findTagsOrDefault(qo.getGroupId(), List.of("__NOPE__"), qo.getGroupType());
                                    or.isNull(TrPoliceTicketGroup::getId).in(PoliceTicket::getTag, collect);
                                });
                    })//
                    .and(qo.getBindFlag() != null && qo.getBindFlag() == 1, and -> {
                        and.isNotNull(TrPoliceTicketGroup::getId);
                    });
        } else {
            wrapper = wrapper.selectAs("0", PoliceTicketVO::getBindFlag)//
                    .orderByDesc(PoliceTicket::getCreateTime);
        }
        if (qo.getPostId() != null) {
            var all = policeTicketTypeService.findAll(new PoliceTicketTypeQO().setPostId(List.of(qo.getPostId())));
            if (all == null || all.isEmpty()) {
                page.setTotal(0L);
                return page;
            }
            wrapper = wrapper.in(PoliceTicket::getTag,
                    all.stream().map(PoliceTicketType::getTag).collect(Collectors.toList()));
        }
        wrapper = wrapper.gt(qo.getStartTime() != null, PoliceTicket::getCreateTime, qo.getStartTime())//
                .lt(qo.getEndTime() != null, PoliceTicket::getCreateTime, qo.getEndTime())//
                .like(qo.getName() != null && !qo.getName().isBlank(), PoliceTicket::getName, qo.getName())//
                .like(qo.getCode() != null && !qo.getCode().isBlank(), PoliceTicket::getCode, qo.getCode())//
                .like(qo.getSource() != null && !qo.getSource().isBlank(), PoliceTicket::getSource, qo.getSource())//
                .like(qo.getContent() != null && !qo.getContent().isBlank(), PoliceTicket::getContent, qo.getContent());

        return policeTicketMapper.selectJoinPage(page, PoliceTicketVO.class, wrapper);
    }

    private List<String> findTagsOrDefault(Long groupId, List<String> defaultCol, Integer groupType) {
        List<Long> postIds;
        if (groupType != null && groupType == 1) {
            // 普通群组需要查询成员中是否有绑定了协同岗
            CreateGroup createGroup = createGroupMapper.selectByGroupId(groupId);
            String userIds = createGroup.getUserIds();
            if (StringUtils.isNotBlank(userIds)) {
                List<String> userIdList = Stream.of(userIds.split(",")).collect(Collectors.toList());
                postIds = queryPostByUserIds(userIdList);
            } else {
                postIds = new ArrayList<>();
            }
        } else {
            postIds = postGroupService.postsByGroupId(groupId);
        }
        if (postIds == null || postIds.isEmpty()) {
            return defaultCol;
        }
        // 普通群组查询当前群组成员是否有成员绑定协同岗，有则查询出绑定的协同岗关联警单

        var ticket = policeTicketTypeService.findAll(new PoliceTicketTypeQO().setPostId(postIds));
        if (ticket == null || ticket.isEmpty()) {
            return defaultCol;
        }
        return ticket.stream().map(PoliceTicketType::getTag).collect(Collectors.toList());
    }


    public List<Long> queryPostByUserIds(List<String> userIdList) {
        // 2. 构建查询条件
        LambdaQueryWrapper<CollaborationPost> wrapper = Wrappers.lambdaQuery();
        wrapper.select(CollaborationPost::getId);
        // 方式1：包含任意一个元素
        for (String userIds : userIdList) {
            wrapper.or(w -> w.apply("FIND_IN_SET({0}, related_user_ids) > 0", userIds));
        }
        List<CollaborationPost> collaborationPosts = collaborationPostMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(collaborationPosts)) {
            return new ArrayList<>();
        }
        return collaborationPosts.stream().map(CollaborationPost::getId).collect(Collectors.toList());
    }

    @Override
    public void bindGroup(@NotNull Long groupId, @NotNull List<Long> ticketIds) {
        deleteBinding(groupId, null);
        if (ticketIds != null && !ticketIds.isEmpty()) {
            var collect = ticketIds.stream().map(tid -> new TrPoliceTicketGroup(idWorker.nextId(), tid, groupId))
                    .collect(Collectors.toList());
            policeTicketGroupMapper.insertBatch(collect);
        }
    }

    @Override
    public void deleteBinding(@NotNull Long groupId, @Null Long ticketId) {
        policeTicketGroupMapper.delete(
                Wrappers.lambdaQuery(TrPoliceTicketGroup.class).eq(TrPoliceTicketGroup::getGroupId, groupId)
                        .eq(ticketId != null, TrPoliceTicketGroup::getTicketId, ticketId));
    }

    @Override
    public List<Long> groupIds(@NotNull Long ticketId) {
        var list = policeTicketGroupMapper.selectList(
                Wrappers.lambdaQuery(TrPoliceTicketGroup.class).eq(TrPoliceTicketGroup::getTicketId, ticketId));
        return Optional.ofNullable(list).stream().flatMap(Collection::stream).map(TrPoliceTicketGroup::getGroupId)
                .collect(Collectors.toList());
    }

    @Override
    public Map<Long, Integer> countBinding(@NotNull List<Long> groupId) {
        var cnts = policeTicketGroupMapper.selectJoinList(PoliceTicketCntVO.class,
                MPJWrappers.lambdaJoin(TrPoliceTicketGroup.class).select(TrPoliceTicketGroup::getGroupId)
                        .selectAs("count(1)", PoliceTicketCntVO::getCnt).in(TrPoliceTicketGroup::getGroupId, groupId)
                        .groupBy(TrPoliceTicketGroup::getGroupId));
        if (cnts == null || cnts.isEmpty()) {
            return Collections.emptyMap();
        }
        return cnts.stream()
                .collect(Collectors.toMap(PoliceTicketCntVO::getGroupId, PoliceTicketCntVO::getCnt, (a, b) -> a));
    }

    @Override
    public void processPoliceTicket(@Validated @NotNull PoliceTicket policeTicket) {
        var cnt = policeTicketMapper.selectCount(
                Wrappers.lambdaQuery(PoliceTicket.class).eq(PoliceTicket::getId, policeTicket.getId()));
        if (cnt != 0) {
            log.warn("already exists");
            return;
        }
        policeTicket.setCreateTime(new Date());
        policeTicketMapper.insert(policeTicket);
    }

    private List<CollaborationPost> findPostForPolTicket(PoliceTicket policeTicket, List<String> depPathCodes) {
        var posts = policeTicketTypeService.getPosts(policeTicket.getTag());
        if (posts == null || posts.isEmpty()) {
            throw new BusinessException("没有找到协同岗");
        }
        List<CollaborationPost> collaborationPosts = collaborationPostMapper.selectBatchIds(posts);
        // 找最接近当前组织的协同岗
        List<CollaborationPost> result = null;
        for (int i = depPathCodes.size() - 1; i >= 0; i--) {
            var depCode = depPathCodes.get(i);
            result = collaborationPosts.stream().filter(a -> Objects.equals(depCode, a.getOrgCode()))
                    .collect(Collectors.toList());
            if (!result.isEmpty()) {
                break;
            }
        }
        if (result == null || result.isEmpty()) {
            throw new BusinessException("警单类型：" + policeTicket.getTag() + "，没有关联本级及上级协同岗，不允许建群！");
        }
        // 过滤没得在线人的协同岗
        var userIds = result.stream()//
                .map(CollaborationPost::getRelatedUserIds)//
                .map(a -> a.split(","))//
                .flatMap(Arrays::stream)//
                .filter(a -> !a.isBlank())//
                .distinct()//
                .map(Long::parseLong).collect(Collectors.toSet());
        var onDuties = attendanceSwitchMapper.listOnDutyPeople(userIds);
        result = result.stream().filter(cp -> {
            var relatedUserIds = cp.getRelatedUserIds();
            var uids = Arrays.stream(relatedUserIds.split(",")).filter(a -> !a.isBlank()).map(Long::parseLong)
                    .collect(Collectors.toSet());
            for (var uid : uids) {
                if (onDuties.contains(uid)) {
                    return true;
                }
            }
            return false;
        }).collect(Collectors.toList());
        if (result.isEmpty()) {
            throw new BusinessException("警单类型：" + policeTicket.getTag() + "，关联协同岗没有成员在线，不允许建群！");
        }
        return result;
    }

    @Override
    public Long createGroup(OpenApiCreateGroupCO createGroupVO, String ownerId) {
        if (StringUtils.isBlank(ownerId)) {
            throw new BusinessException("userId不能为空");
        }
        // groupName长度校验
        validateGroupName(createGroupVO.getGroupName());

        // 根据标签id查找对应的协同岗id
        // 兼容老版本
        PoliceTicket policeTicket;
        if (createGroupVO.getTicketId() != null) {
            policeTicket = policeTicketMapper.selectById(createGroupVO.getTicketId());
        } else if (StringUtils.isNotBlank(createGroupVO.getTicketNo())) {
            policeTicket = policeTicketMapper.selectOne(Wrappers.lambdaQuery(PoliceTicket.class)
                    .eq(PoliceTicket::getCode, createGroupVO.getTicketNo()));
        } else {
            throw new BusinessException("警单ID和警单单号不能同时为空");
        }
        if (policeTicket == null) {
            throw new BusinessException("警单不存在");
        }

        var list = policeTicketGroupMapper.selectList(Wrappers.lambdaQuery(TrPoliceTicketGroup.class)
                .eq(TrPoliceTicketGroup::getTicketId, policeTicket.getId()));
        if (list != null && !list.isEmpty()) {
            log.warn("group already exists:{}", list);
            return list.get(0).getGroupId();
        }
        var userGetVo = coopImHttpClient.userPage(null, ownerId);
        if (userGetVo == null || userGetVo.getResults() == null || userGetVo.getResults().isEmpty()) {
            throw new BusinessException("人员ID找不到");
        }
        var userInfo = userGetVo.getResults().get(0);

        //        var imDepartments = coopImHttpClient.queryDepartment(createGroupVO.getDepartmentCode());
        // 兼容老方案
        List<ImUser.UserDepartment> userDepartments = userInfo.getUserDepartments();
        if (CollectionUtils.isEmpty(userDepartments)) {
            throw new BusinessException("人员部门找不到");
        }
        var imDept = organizationDiversionService.findOne(userDepartments.get(0).getDepartmentCode());
        if (imDept == null) {
            throw new BusinessException("组织找不到");
        }
        var depPathCodes = Arrays.asList(imDept.getFullPathCode().split(","));
        // 获取协同用户
        List<CollaborationPost> postList = findPostForPolTicket(policeTicket, depPathCodes);
        var collaborationPostIds = postList.stream().map(CollaborationPost::getId).collect(Collectors.toList());
        var collaborationPostNames = postList.stream().map(CollaborationPost::getPostName).collect(Collectors.toList());

        // 获取到领导id
        Long directLeaderId = userInfo.getDirectLeaderId();
        // 将领导id拉入群中
        if (directLeaderId != null && directLeaderId != 0L) {
            collaborationPostIds.add(directLeaderId);
        }
        // 获取到领导姓名
        String directLeaderName = userInfo.getDirectLeaderName();
        // 将领导存入
        if (StringUtils.isNotBlank(directLeaderName)) {
            collaborationPostNames.add(directLeaderName);
        }
        // 判断license ai功能是否可用
        boolean aiIsAvailable = licenseUtil.availableByCode(LicenseEnum.AI_COLLABORATION.getCode());
        log.info("policeTicket createGroup aiIsAvailable: {}", aiIsAvailable);
        // ai功能可用才拉群AI助手入群
        if (aiIsAvailable) {
            List<Long> groupAIUserIds = groupAiClient.getDefaultProxyUserIds();
            for (Long groupAIUserId : groupAIUserIds) {
                collaborationPostIds.add(groupAIUserId);
                collaborationPostNames.add("群AI助手");
            }
        }
        // 一键建群使用的是协同岗的id不是人员id，
        List<AddMember> addMembers = collaborationPostIds.stream().map(collaborationPostId -> {
            AddMember addMember = new AddMember();
            addMember.setUserIdentity(collaborationPostId + "");
            addMember.setIdType(0);
            return addMember;
        }).collect(Collectors.toList());

        String location = "";
        var departmentCode = imDept.getCode();
        // 没有就获取部门location
        LambdaQueryWrapper<DepartmentLocation> lambdaWrapper = new LambdaQueryWrapper<>();
        lambdaWrapper.eq(DepartmentLocation::getDepartmentCode, departmentCode);
        DepartmentLocation departmentLocation = departmentLocationMapper.selectOne(lambdaWrapper);
        if (departmentLocation != null) {
            location = departmentLocation.getLocation();
            log.info("createGroup departmentId: {} DepartmentLocation location: {}", departmentCode, location);
        }

        // 构建参数
        CreateGroup createGroup = new CreateGroup();
        createGroup.setId(idWorker.nextId());
        createGroup.setOwnerId(ownerId);
        createGroup.setDepartmentName(imDept.getName());
        createGroup.setOwnerName(userInfo.getName());
        createGroup.setDepartmentId(imDept.getCode());
        createGroup.setLocation(location);
        Owner owner = new Owner();
        owner.setUserIdentity(ownerId);
        owner.setIdType(0);

        GroupCreateReq groupCreateReq = new GroupCreateReq();
        groupCreateReq.setAddMembers(addMembers);
        groupCreateReq.setName(createGroupVO.getGroupName() == null || createGroupVO.getGroupName().isBlank() ? null
                : createGroupVO.getGroupName());
        groupCreateReq.setOwner(owner);

        CreateGroupReq createGroupReq = new CreateGroupReq();
        createGroupReq.setCreateGroupReq(groupCreateReq);
        // 开始调用im接口
        Long group = coopImHttpClient.createGroup(createGroupReq);
        createGroup.setGroupId(group);
        GroupVo groupVo = coopImHttpClient.queryGroupDetail(group);
        createGroup.setGroupName(
                StringUtils.isNotBlank(groupVo.getName()) ? groupVo.getName() : groupVo.getUndefinedName());
        createGroup.setAvatar(groupVo.getAvatar());
        createGroup.setAvatarImg(fileUtil.downloadSaveIcon(coopImHttpClient, groupVo.getAvatar()));
        createGroup.setUserIds(CollectionUtils.join(collaborationPostIds, ",") + "," + ownerId);
        createGroup.setUserNames(CollectionUtils.join(collaborationPostNames, ",") + "," + userInfo.getName());
        createGroup.setCreateTime(new Date());
        createGroup.setUpdateTime(new Date());
        createGroup.setSource(2);
        createGroupMapper.upsert(createGroup,
                Wrappers.lambdaUpdate(CreateGroup.class).eq(CreateGroup::getGroupId, group).set(CreateGroup::getSource, 2));
        GroupExtends existing = groupExtendsMapper.selectByGroupId(group);
        if (existing == null) {
            GroupExtends groupExtends = new GroupExtends();
            groupExtends.setGmtCreated(LocalDateTime.now());
            groupExtends.setGroupId(group);
            groupExtends.setArchived(0);
            groupExtends.setGroupType(2);
            groupExtendsMapper.insertIgnoreDuplicated(groupExtends);
        }
        bindGroup(group, List.of(policeTicket.getId()));

        return group;
    }

    @Override
    public List<PoliceTicketStatisticsVO> statistics(List<Long> orgIds) {
        List<PoliceTicketStatisticsVO> result = new ArrayList<>();
        if (CollectionUtils.isEmpty(orgIds)) {
            return result;
        }
        // 由于历史原因，tb_create_group表中字段department_id存入的是部门code，又没有设计存入部门id字段，这里就按入参转成code去查询，有情况再优化吧
        List<ImDepartment> list = organizationDiversionService.findList(orgIds);
        List<String> orgCodeList = list.stream().map(ImDepartment::getCode).distinct().collect(Collectors.toList());
        if (CollectionUtils.isEmpty(orgCodeList)) {
            return result;
        }
        // 建群数量
        Map<String, Long> createGroupMap = createGroupMapper.findListByOrgCodeList(orgCodeList).stream()
                .collect(Collectors.toMap(CreateGroupCountVO::getOrgCode, CreateGroupCountVO::getCount));
        // 警单关联群组数据
        List<TrPoliceTicketGroupVO> dataList = policeTicketMapper.findListByOrgCodeList(orgCodeList);
        // 警单关联群组数据map
        Map<String, List<TrPoliceTicketGroupVO>> dataMap =
                dataList.stream().collect(Collectors.groupingBy(TrPoliceTicketGroupVO::getOrgCode));
        // 部门id转code
        Map<Long, String> orgMap = list.stream().collect(Collectors.toMap(ImDepartment::getId, ImDepartment::getCode));
        return orgIds.stream().map(orgId -> {
            PoliceTicketStatisticsVO vo = new PoliceTicketStatisticsVO();
            vo.setDeptId(orgId);

            // 获取id对应的code
            String orgCode = orgMap.get(orgId);
            // 总群数
            Long groupCount = createGroupMap.getOrDefault(orgCode, 0L);
            vo.setUnBindTicketGroupCount(groupCount);

            List<TrPoliceTicketGroupVO> matchedList = dataMap.get(orgCode);
            if (CollectionUtils.isNotEmpty(matchedList)) {
                // 关联了协同群组的警单数量
                Long bindGroupTicketCount = matchedList.stream().map(TrPoliceTicketGroupVO::getTicketId).distinct()
                        .collect(Collectors.counting());
                if (Objects.nonNull(bindGroupTicketCount)) {
                    vo.setBindGroupTicketCount(bindGroupTicketCount);
                }

                // 关联了警单的协同群组数量
                Long bindTicketGroupCount = matchedList.stream().map(TrPoliceTicketGroupVO::getGroupId).distinct()
                        .collect(Collectors.counting());
                if (Objects.nonNull(bindTicketGroupCount)) {
                    vo.setBindTicketGroupCount(bindTicketGroupCount);

                    // 未关联警单的协同群组数
                    vo.setUnBindTicketGroupCount(groupCount - bindTicketGroupCount);
                }
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public Long createGroupV2(OpenApiCreateGroupCOV2 openApiCreateGroupCOV2) {
        // 身份证号校验和转换
        String idCard = validateAndNormalizeIdCard(openApiCreateGroupCOV2.getIdCard());
        openApiCreateGroupCOV2.setIdCard(idCard);

        // groupName长度校验
        validateGroupName(openApiCreateGroupCOV2.getGroupName());

        UserGetVo userGetVo = coopImHttpClient.userPage(openApiCreateGroupCOV2.getIdCard(), null);
        if (userGetVo != null && CollectionUtils.isNotEmpty(userGetVo.getResults())) {
            OpenApiCreateGroupCO createGroupVO = new OpenApiCreateGroupCO();
            createGroupVO.setGroupName(openApiCreateGroupCOV2.getGroupName());
            createGroupVO.setTicketNo(openApiCreateGroupCOV2.getTicketNo());
            createGroupVO.setTicketId(openApiCreateGroupCOV2.getTicketId());
            return createGroup(createGroupVO, String.valueOf(userGetVo.getResults().get(0).getId()));
        } else {
            throw new BusinessException("用户信息不存在！");
        }
    }

    @Override
    public R<Long> createGroupByIdCardsAndUserIds(OpenApiCreateGroupCOV3 openApiCreateGroupCOV3, String xUserId, String xIdCard) {
        if (StringUtils.isBlank(xUserId) && StringUtils.isBlank(xIdCard)) {
            throw new BusinessException("userId或idCard不能同时为空");
        }

        if ((StringUtils.isNotBlank(xUserId) && StringUtils.isNotBlank(xIdCard))) {
            throw new BusinessException("userId和idCard不能同时存在");
        }

        GroupCreateTypeEnum groupCreateType = GroupCreateTypeEnum.of(openApiCreateGroupCOV3.getType());
        if (groupCreateType == null) {
            throw new BusinessException("type仅支持1(普通群组)或2(协同群组)");
        }

        GroupCreateSourceEnum groupCreateSource = GroupCreateSourceEnum.of(openApiCreateGroupCOV3.getSubType());
        if (GroupCreateTypeEnum.COLLABORATION.equals(groupCreateType) && groupCreateSource == null) {
            throw new BusinessException("type为2(协同群组)时subType只能是1(一键建群)、3(自定义建群)或5(一键调度)");
        }
        if (groupCreateSource != null) {
            openApiCreateGroupCOV3.setSubType(groupCreateSource.getValue());
        }
        if (GroupCreateTypeEnum.COLLABORATION.equals(groupCreateType)
                && GroupCreateSourceEnum.ONE_KEY.equals(groupCreateSource)
                && CollectionUtils.isEmpty(openApiCreateGroupCOV3.getTagIds())) {
            throw new BusinessException("一键建群标签id不能为空");
        }

        // 身份证号校验和转换
        List<String> idCards = openApiCreateGroupCOV3.getIdCards();
        List<Long> userIds = openApiCreateGroupCOV3.getUserIds();
        // 协同群+一键建群：会自动拉协同岗和AI助手凑足3人，允许 idCards/userIds 同时为空
        boolean isOneKeyCollaboration = GroupCreateTypeEnum.COLLABORATION.equals(groupCreateType)
                && GroupCreateSourceEnum.ONE_KEY.equals(groupCreateSource);
        if (!isOneKeyCollaboration && CollectionUtils.isEmpty(idCards) && CollectionUtils.isEmpty(userIds)) {
            throw new BusinessException("身份证号或用户ID不能同时为空");
        }

        String idCardsParam = CollectionUtils.isEmpty(idCards) ? null : String.join(",", idCards);
        String userIdsParam = CollectionUtils.isEmpty(userIds) ? null : userIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        // 警信接口支持 idCard/userId 混传，但混传时优先用 idCard 查询；为保证查询结果准确性，此处分开调用
        List<ImUser> imUsers = new ArrayList<>();
        if (StringUtils.isNotBlank(idCardsParam)) {
            UserGetVo idCardInfo = coopImHttpClient.userPage(idCardsParam, null);
            if (idCardInfo != null && CollectionUtils.isNotEmpty(idCardInfo.getResults())) {
                imUsers.addAll(idCardInfo.getResults());
            }
        }
        //拓展userId可以传入协同岗id，警信虚拟用户id
        List<CollaborationPost> collaborationPosts = new ArrayList<>();
        // 过滤null后查询，空集合直接跳过，避免 IN () 空集合SQL报错
        List<Long> postQueryIds = CollectionUtils.isEmpty(userIds) ? new ArrayList<>()
                : userIds.stream().filter(Objects::nonNull).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(postQueryIds)) {
            collaborationPosts = collaborationPostMapper.selectByIds(postQueryIds);
        }
        if (StringUtils.isNotBlank(userIdsParam)) {
            UserGetVo userIdInfo = coopImHttpClient.userPage(null, userIdsParam);
            if (userIdInfo != null && CollectionUtils.isNotEmpty(userIdInfo.getResults())) {
                imUsers.addAll(userIdInfo.getResults());
            }
        }

        // 校验查出的用户数量是否与传入数量一致，找出未查到的 idCards 和 userIds
        String missingMsg = buildMemberMissingMsg(imUsers, idCards, userIds, collaborationPosts);

        List<String> coIdCards = new ArrayList<>();
        List<String> errIdCards = new ArrayList<>();
        boolean hasError = StringUtils.isNotBlank(missingMsg);
        String msg = hasError ? missingMsg : "Operation succeeded.";
        if (CollectionUtils.isNotEmpty(idCards)) {
            for (String idCard : idCards) {
                if (!idCard.matches(ID_CARD_PATTERN)) {
                    errIdCards.add(idCard);
                    continue;
                }
                idCard = normalizeIdCard(idCard);
                coIdCards.add(idCard);
            }
            openApiCreateGroupCOV3.setIdCards(coIdCards);
            if (CollectionUtils.isNotEmpty(errIdCards)) {
                String formatErr = "身份证号格式不正确：" + StringUtils.join(errIdCards, ",");
                msg = hasError ? msg + "；" + formatErr : formatErr;
            }
        }

        // groupName长度校验
        validateGroupName(openApiCreateGroupCOV3.getGroupName());

        // 警信接口支持 idCard/userId 混传，但混传时优先用 idCard 查询；为保证查询结果准确性，此处分开调用
        UserGetVo userGetVo;
        if (StringUtils.isNotBlank(xIdCard)) {
            userGetVo = coopImHttpClient.userPage(xIdCard, null);
        } else if (StringUtils.isNotBlank(xUserId)) {
            userGetVo = coopImHttpClient.userPage(null, xUserId);
        } else {
            throw new BusinessException("userId或idCard不能同时为空");
        }
        if (userGetVo != null && CollectionUtils.isNotEmpty(userGetVo.getResults())) {
            if (GroupCreateTypeEnum.NORMAL.equals(groupCreateType)) {
                CreateGroupCOV2 createGroupVOV2 = getCreateGroupCOV2(openApiCreateGroupCOV3, userGetVo, imUsers);
                return R.success(0, msg, createGroup(createGroupVOV2));
            } else if (GroupCreateTypeEnum.COLLABORATION.equals(groupCreateType)) {
                CreateGroupCO createGroupVO = getCreateGroupCO(openApiCreateGroupCOV3, userGetVo, imUsers, collaborationPosts);
                return R.success(0, msg, labelService.createGroup(createGroupVO));
            }
        } else {
            throw new BusinessException("用户信息不存在！");
        }
        return R.failure();
    }

    private Long createGroup(CreateGroupCOV2 createGroupVOV2) {
        // if (( 创建人 + 群成员数量 ) < 3 ) return 群成员小于3不允许建群
        int idCardCount = CollectionUtils.isEmpty(createGroupVOV2.getIdCards()) ? 0 : createGroupVOV2.getIdCards().size();
        int userIdCount = CollectionUtils.isEmpty(createGroupVOV2.getUserIds()) ? 0 : createGroupVOV2.getUserIds().size();
        if (1 + idCardCount + userIdCount < 3) {
            throw new BusinessException("群成员不足3个，不允许建群");
        }

        List<AddMember> addMembers = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(createGroupVOV2.getUserIds())) {
            for (Long userId : createGroupVOV2.getUserIds()) {
                AddMember addMember = new AddMember();
                addMember.setUserIdentity(String.valueOf(userId));
                addMember.setIdType(0);
                addMembers.add(addMember);
            }
        }
        if (CollectionUtils.isNotEmpty(createGroupVOV2.getIdCards())) {
            for (String idCard : createGroupVOV2.getIdCards()) {
                AddMember addMember = new AddMember();
                addMember.setUserIdentity(idCard);
                addMember.setIdType(1);
                addMembers.add(addMember);
            }
        }

        String location = "";
        LambdaQueryWrapper<DepartmentLocation> lambdaWrapper = new LambdaQueryWrapper<>();
        lambdaWrapper.eq(DepartmentLocation::getDepartmentCode, createGroupVOV2.getDepartmentCode());
        DepartmentLocation departmentLocation = departmentLocationMapper.selectOne(lambdaWrapper);
        if (departmentLocation != null) {
            location = departmentLocation.getLocation();
            log.info("createGroup departmentCode: {} DepartmentLocation location: {}",
                    createGroupVOV2.getDepartmentCode(), location);
        }
        log.info("createGroup end location:{}", location);
        // 构建参数
        CreateGroup createGroup = new CreateGroup();
        createGroup.setId(idWorker.nextId());
        createGroup.setOwnerId(createGroupVOV2.getOwnerId());
        createGroup.setDepartmentName(createGroupVOV2.getDepartmentName());
        createGroup.setOwnerName(createGroupVOV2.getOwnerName());
        createGroup.setDepartmentId(createGroupVOV2.getDepartmentCode());
        createGroup.setLocation(location);

        CreateGroupReq createGroupReq = new CreateGroupReq();
        GroupCreateReq groupCreateReq = new GroupCreateReq();
        groupCreateReq.setAddMembers(addMembers);
        if(StringUtils.isNotBlank(createGroupVOV2.getGroupName())){
            groupCreateReq.setName(createGroupVOV2.getGroupName());
        }

        Owner owner = new Owner();
        owner.setUserIdentity(createGroupVOV2.getOwnerId());
        owner.setIdType(0);
        groupCreateReq.setOwner(owner);
        groupCreateReq.setType(ImGroupTypeEnum.CHAT_GROUP.getValue());
        createGroupReq.setCreateGroupReq(groupCreateReq);
        Long groupId = imHttpClient.createGroup(createGroupReq);
        GroupVo groupVo = imHttpClient.queryGroupDetail(groupId);
        if (groupVo == null || CollectionUtils.isEmpty(groupVo.getGroupMembers())) {
            log.error("createGroup queryGroupDetail empty, groupId: {}", groupId);
            throw new BusinessException("群组详情查询失败");
        }

        List<GroupMembers> groupMembers = groupVo.getGroupMembers();
        List<Long> userIdList = new ArrayList<>(groupMembers.size());
        List<String> nameList = new ArrayList<>(groupMembers.size());
        for (GroupMembers m : groupMembers) {
            if (m == null) {
                continue;
            }
            userIdList.add(m.getUserId());
            nameList.add(m.getName());
        }
        Set<Long> members = new HashSet<>(userIdList);

        taskExecutor.execute(() -> {
            try {
                // 异步只做落库
                createGroup.setGroupId(groupId);
                createGroup.setGroupName(
                        StringUtils.isNotBlank(groupVo.getName()) ? groupVo.getName() : groupVo.getUndefinedName());
                createGroup.setUserIds(CollectionUtils.join(userIdList, ","));
                createGroup.setUserNames(CollectionUtils.join(nameList, ","));
                createGroup.setAvatar(groupVo.getAvatar());
                createGroup.setAvatarImg(fileUtil.downloadSaveIcon(imHttpClient, groupVo.getAvatar()));
                createGroup.setCreateTime(new Date());
                createGroup.setUpdateTime(new Date());

                createGroupMapper.upsert(createGroup,
                        Wrappers.lambdaUpdate(CreateGroup.class).eq(CreateGroup::getGroupId, groupId)
                                .set(CreateGroup::getSource, createGroupVOV2.getSource()));
                GroupExtends existing = groupExtendsMapper.selectByGroupId(groupId);
                if (existing == null) {
                    GroupExtends groupExtends = new GroupExtends();
                    groupExtends.setGmtCreated(LocalDateTime.now());
                    groupExtends.setGroupId(groupId);
                    groupExtends.setArchived(0);
                    groupExtends.setGroupType(1);
                    groupExtendsMapper.insertIgnoreDuplicated(groupExtends);
                }
            } catch (Exception e) {
                log.error("async upsert createGroup failed, groupId: {}", groupId, e);
            }
        });

        // 同步推送 MQ（此时 members 一定有值）
        Map<String, Object> map = new HashMap<>(4);
        map.put("groupId", groupId);
        map.put("members", members);
        // 建群完成后，推送所有在线前端消息，刷新页面
        List<String> membersList = members.stream().map(String::valueOf).collect(Collectors.toList());
        var cagentMqFrame = new CagentMqFrame().toBuilder().typeSubSystemMessage(GROUP_CREATE)
                .unicast().userIds(membersList).build()
                .body(GROUP_CREATE, GROUP_CREATE, map).build();
        streamBridge.send(MSG_TOPIC, cagentMqFrame);
        return groupId;
    }

    private CreateGroupCOV2 getCreateGroupCOV2(OpenApiCreateGroupCOV3 cov3, UserGetVo userGetVo, List<ImUser> imUsers) {
        var imUserDetail = userGetVo.getResults().get(0);
        CreateGroupCOV2 createGroupVO = new CreateGroupCOV2();
        createGroupVO.setOwnerId(String.valueOf(imUserDetail.getId()));
        createGroupVO.setOwnerName(imUserDetail.getName());
        createGroupVO.setIdCard(imUserDetail.getIdCard());
        if (imUserDetail.getPrimaryDepartment() != null) {
            createGroupVO.setDepartmentId(imUserDetail.getPrimaryDepartment().getId());
            createGroupVO.setDepartmentCode(imUserDetail.getPrimaryDepartment().getDepartmentCode());
            createGroupVO.setDepartmentName(imUserDetail.getPrimaryDepartment().getDepartmentName());
            createGroupVO.setDepartmentFullPath(imUserDetail.getPrimaryDepartment().getFullPath());
        }
        createGroupVO.setGroupName(cov3.getGroupName());
        Date date = new Date();
        createGroupVO.setCreateTime(date);
        createGroupVO.setUpdateTime(date);
        if (CollectionUtils.isNotEmpty(imUsers)) {
            List<Long> userIds = imUsers.stream().map(ImUser::getId).collect(Collectors.toList());
            createGroupVO.setUserIds(userIds);
        }
        return createGroupVO;
    }

    private CreateGroupCO getCreateGroupCO(OpenApiCreateGroupCOV3 cov3, UserGetVo userGetVo, List<ImUser> imUsers, List<CollaborationPost> collaborationPosts) {
        var imUserDetail = userGetVo.getResults().get(0);
        CreateGroupCO createGroupVO = new CreateGroupCO();
        if (CollectionUtils.isNotEmpty(cov3.getTagIds())) {
            createGroupVO.setIds(cov3.getTagIds().stream().map(String::valueOf).collect(Collectors.toList()));
        }
        createGroupVO.setOwnerId(String.valueOf(imUserDetail.getId()));
        createGroupVO.setOwnerName(imUserDetail.getName());
        createGroupVO.setIdCard(imUserDetail.getIdCard());
        if (imUserDetail.getPrimaryDepartment() != null) {
            createGroupVO.setDepartmentId(imUserDetail.getPrimaryDepartment().getId());
            createGroupVO.setDepartmentCode(imUserDetail.getPrimaryDepartment().getDepartmentCode());
            createGroupVO.setDepartmentName(imUserDetail.getPrimaryDepartment().getDepartmentName());
            createGroupVO.setDepartmentFullPath(imUserDetail.getPrimaryDepartment().getFullPath());
        }
        createGroupVO.setGroupName(cov3.getGroupName());
        Date date = new Date();
        createGroupVO.setCreateTime(date);
        createGroupVO.setUpdateTime(date);
        if (CollectionUtils.isNotEmpty(imUsers)) {
            // 协同岗id可能同时被警信作为虚拟用户返回，从userIds中剔除，避免同一id以userId和cPostId双重入群
            Set<Long> postIdSet = CollectionUtils.isEmpty(collaborationPosts) ? Collections.emptySet()
                    : collaborationPosts.stream().map(CollaborationPost::getId).filter(Objects::nonNull).collect(Collectors.toSet());
            List<Long> userIds = imUsers.stream().map(ImUser::getId)
                    .filter(Objects::nonNull)
                    .filter(id -> !postIdSet.contains(id))
                    .collect(Collectors.toList());
            createGroupVO.setUserIds(userIds);
        }

        if (CollectionUtils.isNotEmpty(collaborationPosts)) {
            List<Long> postIds = collaborationPosts.stream().map(CollaborationPost::getId).collect(Collectors.toList());
            createGroupVO.setCPostIds(postIds);
        }
        createGroupVO.setSource(cov3.getSubType());
        return createGroupVO;
    }

    /**
     * 校验查出的成员用户数量是否与传入数量一致，找出未查到的 idCards 和 userIds，返回缺失信息字符串。
     * 返回空字符串表示无缺失。
     */
    private String buildMemberMissingMsg(List<ImUser> imUsers, List<String> idCards, List<Long> userIds, List<CollaborationPost> collaborationPosts) {
        log.info("imUsers: {}, collaborationPosts: {}", imUsers, collaborationPosts);
        Set<String> foundIdCards = new HashSet<>();
        Set<String> foundUserIds = new HashSet<>();
        if (CollectionUtils.isNotEmpty(imUsers)) {
            for (ImUser u : imUsers) {
                if (u == null) {
                    continue;
                }
                if (StringUtils.isNotBlank(u.getIdCard())) {
                    foundIdCards.add(u.getIdCard().toUpperCase());
                }
                if (u.getId() != null) {
                    foundUserIds.add(String.valueOf(u.getId()));
                }
            }
        }
        // 协同岗ID集合：userId允许传入协同岗id，需与本地库查出的协同岗逐一校验（逻辑删除的协同岗不会查出）
        Set<String> foundPostIds = new HashSet<>();
        if (CollectionUtils.isNotEmpty(collaborationPosts)) {
            for (CollaborationPost post : collaborationPosts) {
                if (post != null && post.getId() != null) {
                    foundPostIds.add(String.valueOf(post.getId()));
                }
            }
        }
        int expectCount = (CollectionUtils.isEmpty(idCards) ? 0 : idCards.size())
                + (CollectionUtils.isEmpty(userIds) ? 0 : userIds.size());

        // 未查到的身份证（按归一化大写比较，兼容末位 x/X 差异）
        List<String> missingIdCards = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(idCards)) {
            for (String idCard : idCards) {
                String normalized = normalizeIdCard(idCard);
                if (normalized == null || !foundIdCards.contains(normalized)) {
                    missingIdCards.add(idCard);
                }
            }
        }
        // 未查到的用户ID/协同岗ID：userId可能是协同岗id也可能是用户id，混传时分别按协同岗、用户校验
        List<String> missingIds = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(userIds)) {
            for (Long id : userIds) {
                String idStr = String.valueOf(id);
                if (id == null || (!foundUserIds.contains(idStr) && !foundPostIds.contains(idStr))) {
                    missingIds.add(idStr);
                }
            }
        }

        if (missingIdCards.isEmpty() && missingIds.isEmpty()) {
            return "";
        }
        int foundCount = expectCount - missingIdCards.size() - missingIds.size();
        List<String> details = new ArrayList<>();
        details.add("期望查到" + expectCount + "个用户，实际查到" + foundCount + "个");
        if (!missingIdCards.isEmpty()) {
            details.add("未找到身份证：" + String.join(",", missingIdCards));
        }
        if (!missingIds.isEmpty()) {
            details.add("未找到用户ID（或协同岗ID）：" + String.join(",", missingIds));
        }
        return String.join("；", details);
    }

    private void validateGroupName(String groupName) {
        if (StringUtils.isNotBlank(groupName) && groupName.length() > 50) {
            throw new BusinessException("群组名称长度不能超过50个字符");
        }
    }

    private String normalizeIdCard(String idCard) {
        if (idCard == null) {
            return null;
        }
        return idCard.toUpperCase();
    }

    private String validateAndNormalizeIdCard(String idCard) {
        if (StringUtils.isBlank(idCard)) {
            throw new BusinessException("身份证号不能为空");
        }
        if (!idCard.matches(ID_CARD_PATTERN)) {
            throw new BusinessException("身份证号格式不正确");
        }
        return normalizeIdCard(idCard);
    }

}