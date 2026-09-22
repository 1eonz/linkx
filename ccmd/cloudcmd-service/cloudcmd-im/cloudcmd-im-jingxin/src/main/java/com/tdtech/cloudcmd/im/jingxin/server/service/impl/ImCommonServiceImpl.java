package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.tdtech.cloudcmd.auth.dto.OrgPermissionDto;
import com.tdtech.cloudcmd.auth.service.RoleRpcService;
import com.tdtech.cloudcmd.base.api.service.GlobalsRpcService;
import com.tdtech.cloudcmd.bean.CagentMqFrame;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.encryptor.service.EncryptionService;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.im.jingxin.api.entity.department.OrganizationVO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.CreateGroupCO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.SendApproveCardCO;
import com.tdtech.cloudcmd.im.jingxin.client.ImHttpClient;
import com.tdtech.cloudcmd.im.jingxin.client.entity.*;
import com.tdtech.cloudcmd.im.jingxin.server.aiclient.group.GroupAiClient;
import com.tdtech.cloudcmd.im.jingxin.server.entity.*;
import com.tdtech.cloudcmd.im.jingxin.server.enums.UserTypeEnum;
import com.tdtech.cloudcmd.im.jingxin.server.service.*;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.*;
import com.tdtech.cloudcmd.im.jingxin.server.util.FileUtil;
import com.tdtech.cloudcmd.msip.enums.LicenseEnum;
import com.tdtech.cloudcmd.msip.util.LicenseUtil;
import com.tdtech.cloudcmd.redis.RedisUtil;
import com.tdtech.cloudcmd.util.*;
import com.tdtech.cloudcmd.util.json.JsonObject;
import com.tdtech.cloudcmd.util.json.JsonUtil;
import com.tdtech.cloudcmd.web.utils.ApplicationContextHolder;
import com.tdtech.cloudcmd.web.utils.HttpClient;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import com.tdtech.linkx.node.api.P2PDataFlowRpcApi;
import com.tdtech.linkx.node.constants.Constants;
import com.tdtech.linkx.node.dto.DispatchResponseDTO;
import com.tdtech.linkx.node.enums.NodeStatusEnum;
import com.tdtech.linkx.node.vo.PeerNodeClientVO;
import com.tdtech.linkx.node.vo.PeerNodeServerVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * @author: S063874
 * @date: 2026-01-14 15:50
 */
@Service
@Slf4j
public class ImCommonServiceImpl implements ImCommonService {


    /**
     * AI助手用户类型
     */
    private static final String AGENT_USER_TYPE_AI = "1";

    /**
     * 1比14亿用户类型
     */

    private static final String AGENT_USER_TYPE_1_14 = "2";
    /**
     * 预警助手用户类型
     */
    private static final String AGENT_USER_TYPE_WARNING = "3";

    private static Map<String, String> AGENT_USER_TYPE_MAP = new HashMap<>();

    private static final String GROUP_CREATE = "GROUP_CREATE";
    private static final String MSG_TOPIC = "cloudcmd-cagent";

    private static final String LOCATION_REGEX =
            "^(-?((180(\\.\\d{1,18})?)|((1[0-7]\\d)|([1-9]?\\d))(\\.\\d{1,18})?))," + "(-?((90(\\.\\d{1,18})?)|([1-8]?\\d)(\\.\\d{1,18})?))$";
    private static final Pattern LOCATION_PATTERN = Pattern.compile(LOCATION_REGEX);

    private static final String ALL_USER_V2_KEY = "cloudcmd:im:user:all";

    /** 用户拼音索引：userId -> [fullPinyin(小写), headChar(小写)]，由 DutyScheduleServiceImpl 定时刷新 */
    private static final String ALL_USER_PINYIN_INDEX_KEY = "cloudcmd:im:user:pinyin:index";

    //调用警信卡片消息时 会出现一个问号图片 故改成纯白图片的base64格式
    private static final String THUMB = "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/2wCEAAkGBwgHBgkIBwgKCgkLDRYPDQwMDRsUFRAWIB0iIiAdHx8kKDQsJCYxJx8fLT0tMTU3Ojo6Iys/RD84QzQ5OjcBCgoKDQwNGg8PGjclHyU3Nzc3Nzc3Nzc3Nzc3Nzc3Nzc3Nzc3Nzc3Nzc3Nzc3Nzc3Nzc3Nzc3Nzc3Nzc3Nzc3N//AABEIAJIAmAMBIgACEQEDEQH/xAAVAAEBAAAAAAAAAAAAAAAAAAAAB//EABQQAQAAAAAAAAAAAAAAAAAAAAD/xAAUAQEAAAAAAAAAAAAAAAAAAAAA/8QAFBEBAAAAAAAAAAAAAAAAAAAAAP/aAAwDAQACEQMRAD8AuIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAP/Z";

    static {
        // 根据类型获取对应注入对象的beanName
        // 群AI助手不再通过全局参数构建，无法通过该方式进行获取连接器
        // AGENT_USER_TYPE_MAP.put(AGENT_USER_TYPE_AI, "groupAIImHttpClient");
        AGENT_USER_TYPE_MAP.put(AGENT_USER_TYPE_1_14, "oneO1p4BImHttpClient");
        AGENT_USER_TYPE_MAP.put(AGENT_USER_TYPE_WARNING, "warningImHttpClient");
    }


    @Autowired
    private ImHttpClient imHttpClient;

    @DubboReference
    RoleRpcService roleRpcService;

    @Autowired
    private ImService imService;

    @Resource
    private CreateGroupMapper createGroupMapper;

    @Resource
    private GroupExtendsMapper groupExtendsMapper;

    @Resource
    private StreamBridge streamBridge;

    @Resource
    private DepartmentLocationMapper departmentLocationMapper;

    @Resource
    private IdWorker idWorker;

    @Resource
    private FileUtil fileUtil;

    @Resource
    private OrganizationServiceImpl organizationService;

    @Resource
    private OrganizationDiversionService organizationDiversionService;

    @Resource
    private CollaborationPostMapper collaborationPostMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Resource
    private LabelService labelService;

    @Resource
    FunctionalDepartmentCoopDefaultService functionalDepartmentCoopDefaultService;

    @Resource
    private GroupAiClient groupAiClient;

    @Resource
    @Qualifier("oneO1p4BImHttpClient")
    private ImHttpClient oneO1p4BImHttpClient;
    @Resource
    private LicenseUtil licenseUtil;

    @Resource(name = "creatGroupExecutorService")
    private TaskExecutor taskExecutor;

    @Resource(name = "userNodeQueryExecutorService")
    private TaskExecutor userNodeQueryExecutor;

    @Resource
    private DepartmentLocationService departmentLocationService;

    @Resource
    private LabelBindingUserMapper labelBindingUserMapper;

    @DubboReference
    private P2PDataFlowRpcApi  p2PDataFlowRpcApi;

    @Autowired
    private EncryptionService encryptionService;

    @DubboReference
    private GlobalsRpcService globalsRpcService;


    /**
     * 根据部门查询组织机构下的用户
     *
     * @param code
     * @param includeChildren
     * @param keywords
     * @return
     */
    @Override
    public ImPage<ImUser> queryUser(String code, Integer includeChildren,
                                    String keywords, String deptId, String name,
                                    Integer pageNum, Integer pageSize) {
        return imHttpClient.userPageByDepartment(pageNum, pageSize, code, includeChildren, keywords, deptId, name, null);

    }

    /**
     * 查询用户所在的群组（如预警助手，普通用户等等）
     *
     * @param type
     * @param key
     * @return
     */
    @Override
    public ImPage<GroupInfoVo> queryGroupByUserType(String type, String key, String keywords, Integer pageNum, Integer pageSize) {
        // 如果是智能体用户，则需要获取智能体代理用户id作为user
        Long userId = null;
        if (UserTypeEnum.AGENT_USER.getCode().equals(type)) {
            // 根据类型获取智能体用户处理类
            String beanName = AGENT_USER_TYPE_MAP.get(key);
            if (StringUtils.isBlank(beanName)) {
                log.error("对应key:{}未找到对应的beanName", key);
                return new ImPage<>();
            }
            ImHttpClient userTypeClient = ApplicationContextHolder.getApplicationContext().getBean(beanName, ImHttpClient.class);
            userId = userTypeClient.getProxyUserId();
            if (null == userId) {
                log.error("获取代理用户id失败");
                return new ImPage<>();
            }
        } else {
            // 普通用户
            userId = Long.valueOf(key);
        }
        return queryLocalGroupByKeywords(userId, keywords, pageNum, pageSize);
    }

    private ImPage<GroupInfoVo> queryLocalGroupByKeywords(Long userId, String keywords, Integer pageNum, Integer pageSize) {
        Page<CreateGroup> page = createGroupMapper.selectUserGroupsByKeywords(new Page<>(pageNum, pageSize), userId, keywords);
        List<GroupInfoVo> records = page.getRecords().stream().map(this::toGroupInfoVo).collect(Collectors.toList());
        ImPage<GroupInfoVo> result = new ImPage<>();
        result.setCurrent((int) page.getCurrent());
        result.setSize((int) page.getSize());
        result.setPageNo(pageNum);
        result.setPageSize(pageSize);
        result.setTotal((int) page.getTotal());
        result.setTotalCount((int) page.getTotal());
        result.setRecords(records);
        return result;
    }

    private GroupInfoVo toGroupInfoVo(CreateGroup createGroup) {
        GroupInfoVo groupInfoVo = new GroupInfoVo();
        groupInfoVo.setGroupId(createGroup.getGroupId());
        groupInfoVo.setName(createGroup.getGroupName());
        groupInfoVo.setAvatar(createGroup.getAvatar());
        groupInfoVo.setGmtCreated(createGroup.getCreateTime() == null ? null : createGroup.getCreateTime().getTime());
        groupInfoVo.setGmtModified(createGroup.getUpdateTime() == null ? null : createGroup.getUpdateTime().getTime());
        return groupInfoVo;
    }

    @Override
    public ImPage<UserFollowVo> getFriendsOrFollows(String userId, Integer userType, Integer pageNo, Integer pageSize) {
        if (Objects.isNull(userType)) {
            userType = 0;
        }
        try {
            var result = imHttpClient.userFriend(userId, userType, pageNo, pageSize);
            if (CollectionUtils.isEmpty(result.getRecords())) {
                return result;
            }
            var records = result.getRecords();
            records.forEach(r -> {
                String path = fileUtil.downloadSaveIcon(imHttpClient, r.getAvatar());
                r.setAvatar(path);
            });
            return result;
        } catch (HttpClient.HttpStatusException e) {
            log.error("警信功能调用出错", e);
            if (HttpStatus.NOT_FOUND.equals(e.getStatus())) {
                throw new BusinessException("当前警信版本不支持该功能");
            } else {
                throw new BusinessException("警信功能调用出错");
            }
        }
    }

    @Override
    public Object getUserTree(String userId, String departmentId) {
        var userDepartments = getUserDepartmentTree(userId);
        JsonObject jobj = new JsonObject();
        jobj.put("departments", userDepartments);
        return jobj;
    }

    @Override
    public ImPage<ImDepartment> getDepartmentPage(String userId, String departmentId, Integer pageNo, Integer pageSize) {
        if (StringUtils.isBlank(departmentId)) {
            var token = imHttpClient.getToken();
            if (token == null) {
                log.error("获取组织部门失败: 无法获取token");
                throw new BusinessException("获取组织部门失败");
            } else if (token.getDepartment() != null && token.getDepartment()
                    .getDepartmentCode() != null && !token.getDepartment().getDepartmentCode().isBlank()) {
                List<ImDepartment> departments = imHttpClient.queryDepartmentByIds(token.getDepartment().getDepartmentId() + "");
                ImPage<ImDepartment> imPage = new ImPage<>();
                imPage.setCurrent(pageNo);
                imPage.setSize(pageSize);
                imPage.setTotal(departments.size());
                imPage.setRecords(departments);
                imPage.setPageSize(pageSize);
                imPage.setTotalCount(departments.size());
                imPage.setPageNo(pageNo);
                return imPage;
            } else {
                log.error("获取组织部门失败: token中部门信息不完整");
                throw new BusinessException("获取组织部门失败");
            }
        }
        return imHttpClient.departmentPage(pageNo, pageSize, null, departmentId, 0, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createGroup(CreateGroupCO createGroupVO) {
        long l = System.currentTimeMillis();
        Set<Long> members = new HashSet<>();

        List<AddMember> addMembers = createGroupVO.getIds().stream().map(id -> {
            members.add(Long.parseLong(id));
            AddMember addMember = new AddMember();
            addMember.setUserIdentity(id);
            addMember.setIdType(0);
            return addMember;
        }).collect(Collectors.toList());

        List<String> postMapMembers = collaborationPostMapper.selectMemberByPostIds(new ArrayList<>(members));
        postMapMembers.forEach(m -> {
            String[] ids = m.split(",");
            for (String id : ids) {
                members.add(Long.parseLong(id.trim()));
            }
        });

        members.add(Long.parseLong(createGroupVO.getOwnerId()));

        Owner owner = new Owner();
        owner.setUserIdentity(createGroupVO.getOwnerId());
        owner.setIdType(0);

        CreateGroupReq createGroupReq = new CreateGroupReq();
        GroupCreateReq groupCreateReq = new GroupCreateReq();
        groupCreateReq.setAddMembers(addMembers);

        groupCreateReq.setOwner(owner);

        String groupName = createGroupVO.getGroupName();
        if (StringUtils.isNotBlank(groupName)) {
            groupCreateReq.setName(groupName);
        }

        createGroupReq.setCreateGroupReq(groupCreateReq);

        String location = getLocation(createGroupVO);
        log.info("createGroup end location:{}", location);

        log.info("调用IM服务创建群组");
        Long group = imHttpClient.createGroup(createGroupReq);
        log.info("群组创建成功，开始异步执行群组ID: ***");
        taskExecutor.execute(()->{
            CreateGroup createGroup = new CreateGroup();
            createGroup.setId(idWorker.nextId());
            createGroup.setGroupId(group);
            createGroup.setOwnerId(createGroupVO.getOwnerId());
            createGroup.setDepartmentName(createGroupVO.getDepartmentName());
            createGroup.setOwnerName(createGroupVO.getOwnerName());
            createGroup.setDepartmentId(createGroupVO.getDepartmentCode());
            createGroup.setLocation(location);

            log.info("查询群组详细信息");
            GroupVo groupVo = imHttpClient.queryGroupDetail(group);
            createGroup.setGroupName(StringUtils.isNotBlank(groupVo.getName()) ? groupVo.getName() : groupVo.getUndefinedName());
            List<GroupMembers> groupMembers = groupVo.getGroupMembers();
            List<Long> userIdList =
                    groupMembers.stream().map(GroupMembers::getUserId).collect(Collectors.toList());
            List<String> nameList =
                    groupMembers.stream().map(GroupMembers::getName).collect(Collectors.toList());

            createGroup.setUserIds(CollectionUtils.join(userIdList, ","));
            createGroup.setUserNames(CollectionUtils.join(nameList, ","));
            createGroup.setAvatar(groupVo.getAvatar());
            // source 由前端传入：3=自定义建群，5=一键调度，不传默认3
            int source = createGroupVO.getSource() != null ? createGroupVO.getSource() : 3;
            createGroup.setSource(source);
            createGroup.setAvatarImg(fileUtil.downloadSaveIcon(imHttpClient, groupVo.getAvatar()));
            Date now = new Date();
            createGroup.setCreateTime(now);
            createGroup.setUpdateTime(now);

            log.info("保存群组信息到数据库, source:{}", source);
            createGroupMapper.upsert(createGroup,
                    Wrappers.lambdaUpdate(CreateGroup.class).eq(CreateGroup::getGroupId, group).set(CreateGroup::getSource, source));
            GroupExtends existing = groupExtendsMapper.selectByGroupId(group);
            if (existing == null) {
                GroupExtends groupExtends = new GroupExtends();
                groupExtends.setGmtCreated(LocalDateTime.now());
                groupExtends.setGroupId(group);
                groupExtends.setArchived(0);
                groupExtends.setGroupType(2);
                groupExtendsMapper.insertIgnoreDuplicated(groupExtends);
            }
        });
        Map<String, Object> map = new HashMap<>();
        map.put("members", members);
        map.put("groupId", group);

        //归档完成后，推送所有在线前端消息，刷新页面
        log.info("推送群成员群组创建消息到前端");
        List<String> membersList = members.stream().map(String::valueOf).collect(Collectors.toList());
        var cagentMqFrame = new CagentMqFrame().toBuilder().typeSubSystemMessage(GROUP_CREATE)
                .unicast().userIds(membersList).build()
                .body(GROUP_CREATE, GROUP_CREATE, map).build();
        streamBridge.send(MSG_TOPIC, cagentMqFrame);

        log.info("创建群组流程结束,耗时：{}，返回群组ID: ***", System.currentTimeMillis() - l);
        return group;
    }

    @Override
    public ImPage<ImUser> getUsersPage(String name, String keywords, String departmentId, Integer pageNo, Integer pageSize) {
        Set<String> departmentIds = getUserDepartments();
        List<String> deptIds = new ArrayList<>();
        if (StringUtils.isBlank(departmentId)) {
            deptIds.addAll(departmentIds);
        } else {
            for (String deptId : departmentId.split(",")) {
                if (departmentIds.stream().anyMatch(deptId::equals)) {
                    deptIds.add(deptId);
                    log.debug("部门ID {} 在权限范围内", deptId);
                } else {
                    log.warn("部门ID {} 不在权限范围内", deptId);
                    return new ImPage<>();
                }
            }
        }

        if (CollectionUtils.isEmpty(deptIds)) {
            return new ImPage<>();
        }

        ImPage<ImUser> newImUserPage = new ImPage<>();
        UserInfo user = SecurityUtils.getUser();
        // 规范化分页参数：pn=页码(默认1)，ps=页大小(0表示未指定)
        int pn = (pageNo == null || pageNo < 1) ? 1 : pageNo;
        int ps = (pageSize == null || pageSize < 1) ? 0 : pageSize;
        // 从缓存中获取用户数据
        List<ImUser> imUserList = redisUtil.get(ALL_USER_V2_KEY, new TypeReference<>() {
        });
        if(CollectionUtils.isEmpty(imUserList)){
            imUserList = new ArrayList<>();
            log.info("用户数据缓存为空，从IM服务获取数据");
            for (String deptId : deptIds) {
                log.debug("处理部门ID: {}", deptId);
                ImPage<ImUser> imUserPage = getImUserPage(name, keywords, deptId, pageNo, pageSize, departmentIds);
                List<ImUser> pageRecords = imUserPage.getRecords();
                if (pageRecords == null) {
                    pageRecords = new ArrayList<>();
                }
                // 剔除自己并同步修正该部门的total，保证total与各页records累加一致
                boolean selfRemoved = pageRecords.removeIf(r -> r != null && user.getUserId().equals(r.getId()));
                if (selfRemoved && imUserPage.getTotal() != null && imUserPage.getTotal() > 0) {
                    imUserPage.setTotal(imUserPage.getTotal() - 1);
                }
                imUserList.addAll(pageRecords);
                Integer deptTotal = imUserPage.getTotal();
                if (deptTotal != null && (newImUserPage.getTotal() == null || newImUserPage.getTotal() < deptTotal)) {
                    newImUserPage.setTotal(deptTotal);
                }
            }
        }else{
            //有缓存就走缓存
            log.info("从缓存获取数据并配置");
            // 仅在有关键字时读取拼音索引，避免无关键字查询时的冗余 redis 调用
            Map<String, String[]> pinyinIndex = StringUtils.isNotBlank(keywords)
                    ? redisUtil.get(ALL_USER_PINYIN_INDEX_KEY, new TypeReference<>() {}) : null;
            List<ImUser> filtered = imUserList.stream().filter(imUser -> {
                // 剔除自己，保证total与records口径一致
                if (user.getUserId().equals(imUser.getId())) {
                    return false;
                }
                boolean isMatchDept = false;
                ImUser.UserDepartment primaryDepartment = imUser.getPrimaryDepartment();
                if(primaryDepartment != null){
                    // 过滤出包含权限部门的用户
                    String id = String.valueOf(primaryDepartment.getId());
                    isMatchDept = deptIds.contains(id);
                }
                String[] pinyinArr = pinyinIndex != null ? pinyinIndex.get(String.valueOf(imUser.getId())) : null;
                return isMatchDept && matchUserKeywords(imUser, keywords, pinyinArr);
            }).collect(Collectors.toList());
            // 内存分页：只对当前页做头像处理，避免对全量命中用户逐个扫描头像目录
            newImUserPage.setTotal(filtered.size());
            int effectivePs = ps > 0 ? ps : filtered.size();
            int fromIndex = (pn - 1) * effectivePs;
            if (fromIndex >= filtered.size()) {
                imUserList = new ArrayList<>();
            } else {
                imUserList = new ArrayList<>(filtered.subList(fromIndex, Math.min(fromIndex + effectivePs, filtered.size())));
            }
        }
        long l = System.currentTimeMillis();
        imUserList.forEach(r -> r.setAvatar(fileUtil.getAvatarPathOrDownload(imHttpClient, r.getAvatar())));
        log.info("获取到头像: {},耗时：{}ms", imUserList.size(), System.currentTimeMillis() - l);
        newImUserPage.setRecords(imUserList);
        newImUserPage.setPageNo(pn);
        newImUserPage.setCurrent(pn);
        // pageSize返回实际生效的页大小(未指定时为当前记录数)，而非当前页记录数，避免前端误判有下一页
        int respPageSize = ps > 0 ? ps : imUserList.size();
        newImUserPage.setPageSize(respPageSize);
        newImUserPage.setSize(respPageSize);

        log.info("获取用户分页结束: 总记录数={}", newImUserPage.getTotal());
        return newImUserPage;
    }

    /**
     * 关键字匹配（身份证 / 姓名 / 手机号 / 拼音首字母 / 拼音全拼，大小写不敏感）。
     *
     * @param pinyinArr 预计算拼音 [fullPinyin(小写), headChar(小写)]，来自拼音索引缓存；为 null 时实时转换
     */
    private boolean matchUserKeywords(ImUser imUser, String keywords, String[] pinyinArr) {
        if (StringUtils.isBlank(keywords)) {
            return true;
        }
        String idCard = imUser.getIdCard();
        String imUserName = imUser.getName();
        String mobile = imUser.getMobile();
        boolean containsIdCard = StringUtils.isNotBlank(idCard) && idCard.contains(keywords);
        boolean containsUserName = StringUtils.isNotBlank(imUserName) && imUserName.contains(keywords);
        boolean containsMobile = StringUtils.isNotBlank(mobile) && mobile.contains(keywords);
        // 拼音首字母和全拼（大小写不敏感）
        boolean containsPinyin = false;
        String[] arr = pinyinArr;
        if (arr == null && StringUtils.isNotBlank(imUserName)) {
            arr = buildPinyin(imUserName);
        }
        if (arr != null) {
            String lowerKeywords = keywords.toLowerCase();
            containsPinyin = (StringUtils.isNotBlank(arr[0]) && arr[0].contains(lowerKeywords))
                    || (StringUtils.isNotBlank(arr[1]) && arr[1].contains(lowerKeywords));
        }
        return containsIdCard || containsUserName || containsMobile || containsPinyin;
    }

    private String[] buildPinyin(String userName) {
        try {
            String[] pinyinArr = PinyinUtils.getPinyinAndHead(userName);
            String fullPinyin = PinyinUtils.normalize(pinyinArr[0]).toLowerCase();
            String headChar = PinyinUtils.normalize(pinyinArr[1]).toLowerCase();
            return new String[]{fullPinyin, headChar};
        } catch (Exception e) {
            log.warn("用户名转拼音失败: {}, error: {}", userName, e.getMessage());
            return null;
        }
    }

    private ImPage<ImUser> getImUserPage(String name, String keywords, String departmentId, Integer pageNo, Integer pageSize, Set<String> userDepartments) {
        if (StringUtils.isNotBlank(departmentId) && !userDepartments.contains(departmentId)) {
            return new ImPage<>();
        }
        if (StringUtils.isBlank(departmentId)) {
            departmentId = SecurityUtils.getUser().getOrganizationId() + "";
        }
        var result = imHttpClient.userPageByDepartment(pageNo, pageSize, null, 0, keywords, departmentId, name, null);
        var records = result.getRecords();
        if (records != null && !records.isEmpty()) {
            records.forEach(r -> {
                r.setAvatar(fileUtil.getAvatarPathOrDownload(imHttpClient, r.getAvatar()));
            });
            result.setRecords(records);
        }
        return result;
    }

    private String getLocation(CreateGroupCO createGroupVO) {
        String location = createGroupVO.getLocation();
        if (StringUtils.isBlank(location)) {
            // 没有就获取部门location
            LambdaQueryWrapper<DepartmentLocation> lambdaWrapper = new LambdaQueryWrapper<>();
            lambdaWrapper.eq(DepartmentLocation::getDepartmentCode, createGroupVO.getDepartmentCode());
            DepartmentLocation departmentLocation = departmentLocationMapper.selectOne(lambdaWrapper);
            if (departmentLocation != null) {
                location = departmentLocation.getLocation();
            }
        } else {
            // 验证格式
            Matcher matcher = LOCATION_PATTERN.matcher(location);
            if (!matcher.matches()) {
                throw new BusinessException(
                        "经纬度格式错误，正确格式：经度,纬度，经度(-180~180)，纬度(-90~90)，小数点后最多18位");
            }
        }

        return location;
    }

    public Set<String> getUserDepartments() {
        UserInfo user = SecurityUtils.getUser();
        if (Objects.isNull(user)) {
            throw new BusinessException("获取用户信息失败");
        }
        String userId = user.getUserId() + "";

        var role = roleRpcService.getRoleByUserId(userId);
        if (Objects.isNull(role)) {
            return new HashSet<>();
        }
        Set<String> orgIds = new HashSet<>();
        if (user.isAdmin()) {
            var roles = organizationService.tree("");
            handleRole(roles, orgIds);
        } else {
            orgIds = role.getOrgPermissionIds();
        }

        if (user.getOrganizationId() != null) {
            orgIds.add(user.getOrganizationId() + "");
        } else {
            if (user.getOrganizationCode() != null) {
                var dep = imHttpClient.queryDepartment(user.getOrganizationCode());
                if (CollectionUtils.isNotEmpty(dep)) {
                    String[] arr = dep.get(0).getFullPath().split(",");
                    orgIds.add(arr[arr.length - 1].trim());
                }
            }
        }

        log.info("getUserDepartments user:{}, orgIds:{}", user, orgIds);

        return orgIds;
    }

    private void handleRole(OrganizationVO org, Set<String> orgIds) {
        if (org != null) {
            orgIds.add(org.getId() + "");

            if (org.getChildren() != null && !org.getChildren().isEmpty()) {
                org.getChildren().forEach(o -> handleRole(o, orgIds));
            }
        }
    }

    public List<OrganizationVO> getUserDepartmentTree(String userId) {
        UserInfo user = SecurityUtils.getUser();
        if (Objects.isNull(user)) {
            throw new BusinessException("获取用户信息失败");
        }

        if (StringUtils.isBlank(userId)) {
            userId = user.getUserId() + "";
        }

        var role = roleRpcService.getRoleByUserId(userId);
        if (Objects.isNull(role)) {
            return new ArrayList<>();
        }

        if (user.isAdmin()) {
            List<OrganizationVO> roles = new ArrayList<>();
            roles.add(organizationService.tree(""));
            return roles;
        }

        List<OrganizationVO> roleDtos = new ArrayList<>();
        copyRoleToOrg(role.getImOrgPriv(), roleDtos);

        Set<String> orgIds = role.getOrgPermissionIds();

        if (user.getOrganizationId() != null && !orgIds.contains(user.getOrganizationId() + "")) {
            var dep = imHttpClient.queryDepartmentByIds(user.getOrganizationId() + "");
            if (CollectionUtils.isEmpty(dep)) {
                throw new BusinessException("获取用户部门信息失败");
            }
            List<OrganizationVO> department = new ArrayList<>();
            copyImOrgToOrg(dep, department);
            for (OrganizationVO organizationVO : department) {
                addNode(organizationVO, roleDtos);
            }
        }

        if (user.getOrganizationId() == null && user.getOrganizationCode() != null) {
            var dep = imHttpClient.queryDepartment(user.getOrganizationCode());
            if (CollectionUtils.isNotEmpty(dep)) {
                List<OrganizationVO> department = new ArrayList<>();
                List<ImDepartment> src = dep.stream().filter(d -> {
                    String[] arr = d.getFullPath().split(",");
                    return !orgIds.contains(arr[arr.length - 1].trim());
                }).collect(Collectors.toList());
                copyImOrgToOrg(src, department);
                for (OrganizationVO organizationVO : department) {
                    addNode(organizationVO, roleDtos);
                }
            }
        }
        return roleDtos;
    }

    @Override
    public Long createGroupByFunctionalDepartment(CreateGroupCO createGroupVO) {
        //职能部门建群
        long l = System.currentTimeMillis();
        LinkedList<Long> collaborationPostIds = new LinkedList<>();
        LinkedList<String> collaborationPostNames = new LinkedList<>();

        // 根据上传的身份证号，查找当前用户的领导id
        UserGetVo userGetVo = imHttpClient.userPage(createGroupVO.getIdCard(), null);
        List<ImUser> successUserList = userGetVo.getResults();
        if (CollectionUtils.isEmpty(successUserList)) {
            throw new BusinessException("创建人不存在");
        }

        boolean aiIsAvailable = licenseUtil.availableByCode(LicenseEnum.AI_COLLABORATION.getCode());

        // 拉取领导
        pullDirectLeader(successUserList, collaborationPostIds, collaborationPostNames);

        log.info("调用一键建群接口，创建群组");
        // 查询默认的协同岗
        List<FunctionalDepartmentCoopDefault> defaultsList = functionalDepartmentCoopDefaultService.listAll();
        // 和传入的id进行合并
        List<String> ids = createGroupVO.getIds();
        if(CollectionUtils.isEmpty(defaultsList) && CollectionUtils.isEmpty(ids)){
            throw new BusinessException("请选择协同岗建群");
        }
        List<Long> idsList;
        if(CollectionUtils.isEmpty(defaultsList) && CollectionUtils.isNotEmpty(ids)){
            // 默认为空就用传进来的
            idsList = ids.stream().map(Long::parseLong).collect(Collectors.toList());
        }else if(CollectionUtils.isNotEmpty(defaultsList) && CollectionUtils.isEmpty(ids)){
            // 默认不为空，选择的为空，就用默认的
            idsList = defaultsList.stream().map(FunctionalDepartmentCoopDefault::getUserId).collect(Collectors.toList());
        }else {
            // 都不为空，就合并
            idsList = ids.stream().map(Long::parseLong).collect(Collectors.toList());
            List<Long> userIds = defaultsList.stream().map(FunctionalDepartmentCoopDefault::getUserId).collect(Collectors.toList());
            userIds.forEach(userId -> {
                if (!idsList.contains(userId)) {
                    idsList.add(userId);
                }
            });
        }
        List<CollaborationPost> result = collaborationPostMapper.selectBatchIds(idsList);
        if (result == null || result.isEmpty()) {
            throw new BusinessException("该职能部门没有挂靠的协同岗，不允许建群！");
        }
        // 如果标签绑定选择的人员不为空，则拉取人员加入
        List<Long> labelIds = createGroupVO.getLabelIds();
        // 根据标签id查询所有的人员
        if(CollectionUtils.isNotEmpty(labelIds)){
            // 1. 查询标签绑定的人员
            List<LabelBindingUser> labelBindingUsers = labelBindingUserMapper.selectList(
                    Wrappers.lambdaQuery(LabelBindingUser.class)
                            .in(LabelBindingUser::getLabelId, labelIds));
            // 提取所有userId并去重
            if(CollectionUtils.isNotEmpty(labelBindingUsers)){
                List<Long> parsedUserIds = labelBindingUsers.stream()
                        .filter(binding -> CollectionUtils.isNotEmpty(binding.getUserIds()))
                        .flatMap(binding -> binding.getUserIds().stream())
                        .distinct()
                        .collect(Collectors.toList());
                log.info("获取到的绑定人员为：{}", parsedUserIds);
                if(CollectionUtils.isNotEmpty(parsedUserIds)){
                    collaborationPostIds.addAll(parsedUserIds);
                    parsedUserIds.forEach(id -> collaborationPostNames.add("id为" + id + "的标签绑定人员"));
                }
            }
        }

        AtomicBoolean atomicBoolean = new AtomicBoolean(false);
        // 判断是否存在人员核查岗,ai助手可用
        if(aiIsAvailable){
            List<CollaborationPost> exitsResult = collaborationPostMapper.findBatchByType(idsList, 1);
            if(CollectionUtils.isNotEmpty(exitsResult)){
                //  存在人员核查岗，拉1:14e入群
                Long proxyUserId = oneO1p4BImHttpClient.getProxyUserId();
                if (Objects.nonNull(proxyUserId)) {
                    collaborationPostIds.add(proxyUserId);
                    collaborationPostNames.add("人员核查");
                    atomicBoolean.set(true);
                }else{
                    log.error("1o1p4BImHttpClient not login yet..cant add 1:1.4B user");
                    throw new BusinessException("人员核查配置错误，请联系管理员");
                }
                if (idsList.size() > exitsResult.size()) {
                    // 所选岗位存在普通岗，也需要拉ai助手
                    pullAI(collaborationPostIds, collaborationPostNames);
                }
            }else{
                // 不存在人员核查岗，也拉ai助手
                pullAI(collaborationPostIds, collaborationPostNames);
            }
        }

          // 在不在线，都能建群成功 所以，不需要判断人员是否在线
//        // 过滤没得在线人的协同岗
//        var userIds = result.stream()//
//                .map(CollaborationPost::getRelatedUserIds)//
//                .map(a -> a.split(","))//
//                .flatMap(Arrays::stream)//
//                .filter(a -> !a.isBlank())//
//                .distinct()//
//                .map(Long::parseLong).collect(Collectors.toSet());
//        var onDuties = attendanceSwitchMapper.listOnDutyPeople(userIds);
//        result = result.stream().filter(cp -> {
//            var relatedUserIds = cp.getRelatedUserIds();
//            var uids = Arrays.stream(relatedUserIds.split(",")).filter(a -> !a.isBlank()).map(Long::parseLong)
//                    .collect(Collectors.toSet());
//            for (var uid : uids) {
//                if (onDuties.contains(uid)) {
//                    return true;
//                }
//            }
//            return false;
//        }).collect(Collectors.toList());
//
//        if (result.isEmpty()) {
//            throw new BusinessException("该职能部门下的协同岗没有成员在线，不允许建群！");
//        }
        Long groupId = labelService.executeCreatGroup(createGroupVO, result, collaborationPostIds, collaborationPostNames, atomicBoolean, 4);
        log.info("职能建群接口调用成功，耗时：{}", System.currentTimeMillis() - l);
        return groupId;
    }

    private void pullAI(LinkedList<Long> collaborationPostIds, LinkedList<String> collaborationPostNames) {
        List<Long> groupAIUserIds = groupAiClient.getDefaultProxyUserIds();
        for (Long groupAIUserId : groupAIUserIds) {
            collaborationPostIds.add(groupAIUserId);
            collaborationPostNames.add("群AI助手");
        }
    }

    private void pullDirectLeader(List<ImUser> successUserList, LinkedList<Long> collaborationPostIds,
                                       LinkedList<String> collaborationPostNames) {
        // 获取到领导id
        Long directLeaderId = successUserList.get(0).getDirectLeaderId();
        // 将领导id拉入群中
        if (directLeaderId != null && directLeaderId != 0L) {
            collaborationPostIds.add(directLeaderId);
        }
        // 获取到领导姓名
        String directLeaderName = successUserList.get(0).getDirectLeaderName();
        // 将领导存入
        if (org.apache.commons.lang3.StringUtils.isNotBlank(directLeaderName)) {
            collaborationPostNames.add(directLeaderName);
        }
    }

    private void addNode(OrganizationVO org, List<OrganizationVO> orgs) {
        if (orgs.isEmpty()) {
            orgs.add(org);
            return;
        }
        List<OrganizationVO> needRemove = new ArrayList<>();
        OrganizationVO needAdd = null;
        boolean isAdd = false;
        for (OrganizationVO organizationVO : orgs) {
            if (org.getFullPath().contains(organizationVO.getFullPath())) {
                if (organizationVO.getChildren() == null) {
                    organizationVO.setChildren(new ArrayList<>());
                }
                addNode(org, organizationVO.getChildren());
                isAdd = true;
                break;
            } else if (organizationVO.getFullPath().contains(org.getFullPath())) {
                if (org.getChildren() == null) {
                    org.setChildren(new ArrayList<>());
                }
                addNode(organizationVO, org.getChildren());
                needAdd = org;
                needRemove.add(organizationVO);
                isAdd = true;
            }
        }
        if (!needRemove.isEmpty()) {
            orgs.removeAll(needRemove);
        }
        if (needAdd != null) {
            orgs.add(needAdd);
        }
        if (!isAdd) {
            orgs.add(org);
        }
    }

    private void copyImOrgToOrg(List<ImDepartment> src, List<OrganizationVO> dest) {
        if (CollectionUtils.isEmpty(src)) {
            return;
        }

        src.forEach(s -> {
            OrganizationVO organizationVO = new OrganizationVO();
            if (s.getId() == null) {
                String[] arr = s.getFullPath().split(",");
                if (arr.length > 0) {
                    organizationVO.setId(Long.parseLong(arr[arr.length - 1].trim()));
                }
            } else {
                organizationVO.setId(s.getId());
            }
            if (s.getCode() == null) {
                String[] arr = s.getFullPathCode().split(",");
                if (arr.length > 0) {
                    organizationVO.setCode(arr[arr.length - 1].trim());
                }
            } else {
                organizationVO.setCode(s.getCode());
            }

            if (s.getName() == null) {
                String[] arr = s.getFullPathName().split(",");
                if (arr.length > 0) {
                    organizationVO.setName(arr[arr.length - 1].trim());
                }
            } else {
                organizationVO.setName(s.getName());
            }

            organizationVO.setShortName(s.getShortName());
            organizationVO.setParentId(s.getParentId());
            organizationVO.setParentCode(s.getParentCode());
            organizationVO.setParentName(s.getParentName());
            organizationVO.setSort(s.getSort());
            organizationVO.setFullPath(s.getFullPath());
            organizationVO.setFullPathCode(s.getFullPathCode());
            organizationVO.setFullPathName(s.getFullPathName());
            organizationVO.setGmtCreated(new Date(s.getGmtCreated()));
            organizationVO.setGmtModified(new Date(s.getGmtModified()));
            if (CollectionUtils.isNotEmpty(s.getChildren())) {
                organizationVO.setChildren(new ArrayList<>());
                copyImOrgToOrg(s.getChildren(), organizationVO.getChildren());
            }
            dest.add(organizationVO);
        });
    }

    private void copyRoleToOrg(List<OrgPermissionDto> src, List<OrganizationVO> dest) {

        if (CollectionUtils.isEmpty(src)) {
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        src.forEach(s -> {
            if (!s.isHasPermission()) {
                return;
            }
            OrganizationVO organizationVO = new OrganizationVO();
            organizationVO.setId(Long.parseLong(s.getId()));
            organizationVO.setCode(s.getCode());
            organizationVO.setName(s.getName());
            organizationVO.setShortName(s.getShortName());
            organizationVO.setParentId(Long.parseLong(s.getParentId()));
            organizationVO.setParentCode(s.getParentCode());
            organizationVO.setParentName(s.getParentName());
            organizationVO.setSort(s.getSort());
            organizationVO.setFullPath(s.getFullPath());
            organizationVO.setFullPathCode(s.getFullPathCode());
            organizationVO.setFullPathName(s.getFullPathName());
            try {
                organizationVO.setGmtCreated(sdf.parse(s.getGmtCreated()));
                organizationVO.setGmtModified(sdf.parse(s.getGmtModified()));
            } catch (ParseException e) {
                log.info("时间转换异常");
            }
            if (CollectionUtils.isNotEmpty(s.getChildren())) {
                organizationVO.setChildren(new ArrayList<>());
                copyRoleToOrg(s.getChildren(), organizationVO.getChildren());
            }
            dest.add(organizationVO);
        });
    }
    @Override
    public Object pullHistoryGroup(String sync) {
        // 查询所有用户
        List<ImUser> imUsers = imService.queryUser(null, 1, null, null, null);
        List<Long> userIds = imUsers.stream().map(ImUser::getId).collect(Collectors.toList());
        log.info("查询到的用户数：{}", userIds.size());
        Set<Long> groupIds = new HashSet<>();
        for (Long userId : userIds) {
            List<GroupInfoVo> groupInfoVos = imHttpClient.queryGroupByUserIds(userId);
            if (CollectionUtils.isNotEmpty(groupInfoVos)) {
                List<Long> longs = groupInfoVos.stream().map(GroupInfoVo::getGroupId).collect(Collectors.toList());
                groupIds.addAll(longs);
            }
        }
        Set<Long> newGroupIds  = new HashSet<>();
        log.info("去重后所有群组数量：{}", groupIds.size());
        // 查询警务协同不存在的群组
        List<Long> groupIdsList = new ArrayList<>(groupIds);
        List<List<Long>> partition = Lists.partition(groupIdsList, 500);

        for (List<Long> groupIdList : partition) {
            Set<Long> existingGroupIds = createGroupMapper.selectByGroupIds(new HashSet<>(groupIdList));
            // 找出不在数据库中的群组ID
            Set<Long> newGroupId = groupIdList.stream()
                    .filter(groupId -> !existingGroupIds.contains(groupId))
                    .collect(Collectors.toSet());
            if(CollectionUtils.isNotEmpty(newGroupId)){
                newGroupIds.addAll(newGroupId);
            }
        }

        log.info("需要同步的群组数量：{}", newGroupIds.size());
        if("1".equals(sync)){
            syncGroup(newGroupIds);
        }
        return true;
    }

    @Override
    public Boolean sendApproveCard(SendApproveCardCO sendApproveCardCO) {
        ImMessageRequest<CardMsgVo> request = new ImMessageRequest<>();
        request.setCategory(1);
        request.setMsgType(5);
        request.setFrom(sendApproveCardCO.getUserId());
        request.setTo(sendApproveCardCO.getApproveUser());
        request.setPlaintext(1);
        request.setFromIdType(0);
        request.setToIdType(1);
        request.setMsg(buildApproveCardMsg(sendApproveCardCO.getToLeaderUrl()));
        imHttpClient.sendMsgByFrom(request);

        return Boolean.TRUE;
    }

    private CardMsgVo buildApproveCardMsg(String url) {
        CardMsgVo cardMsgVo = new CardMsgVo();
        cardMsgVo.setCardType("customCard");
        CardMsgDataVo data = new CardMsgDataVo();
        data.setEmergencyLevel("blue");
        data.setThumb(THUMB);
        data.setTitle("AI助手使用申请");
        data.setDescribe("您有一个问答审批待处理，请尽快处理");
        data.setUrl(url);
        cardMsgVo.setData(data);
        return cardMsgVo;
    }

    @Override
    public ImUser getUserByUserId(String userId) {
        UserGetVo userGetVo = imHttpClient.userPageById(userId);
        if (userGetVo != null && userGetVo.getResults() != null && !userGetVo.getResults().isEmpty()) {
            ImUser imUser = userGetVo.getResults().get(0);
            imUser.setAvatar(fileUtil.downloadSaveIcon(imHttpClient, imUser.getAvatar()));
            return imUser;
        }
        return null;
    }

    @Override
    public ImUser getUserByUserIdCard(String idCard) {
        var userGetVo = imHttpClient.userPage(idCard, null);
        if (userGetVo == null || userGetVo.getResults() == null || userGetVo.getResults().isEmpty()) {
            return null;
        }

        return userGetVo.getResults().get(0);
    }

    private void syncGroup(Set<Long> newGroupIds) {
        if (CollectionUtils.isNotEmpty(newGroupIds)) {
            log.info("开始同步群组信息");
            long l = System.currentTimeMillis();
            newGroupIds.forEach(groupId -> {
                long l1 = System.currentTimeMillis();
                QueryWrapper<CreateGroup> queryWrapper = new QueryWrapper<>();
                GroupVo groupVo = imHttpClient.queryGroupDetail(groupId);
                try {
                    // 新增记录创建群组记录
                    CreateGroup createGroup = new CreateGroup();
                    createGroup.setGroupId(groupVo.getId());
                    createGroup.setCreateTime(new Date(groupVo.getGmtCreated()));
                    createGroup.setUpdateTime(new Date(groupVo.getGmtModified()));
                    List<GroupMembers> groupMembers = groupVo.getGroupMembers();
                    boolean isInsert = true;
                    if (ListUtils.isNotBlankList(groupMembers)) {
                        for (GroupMembers groupMember : groupMembers) {
                            if (groupMember.getRole().equals(2)) {
                                createGroup.setOwnerId(groupMember.getUserId() + "");
                                createGroup.setOwnerName(groupMember.getName());
                                UserGetVo userGetVo = imHttpClient.userPage(groupMember.getIdCard(), null);
                                if (userGetVo.getResults() != null && userGetVo.getResults().size() > 0) {
                                    ImUser imUser = userGetVo.getResults().get(0);
                                    List<ImUser.UserDepartment> userDepartments = imUser.getUserDepartments();
                                    for (ImUser.UserDepartment department : userDepartments) {
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
                                }else{
                                    log.info("群组id{}的群主id不存在，不同步群：{}", groupId,groupMember.getIdCard());
                                    isInsert = false;
                                }
                            }
                        }
                        if(isInsert){
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
                            createGroup.setId(idWorker.nextId());
                            createGroup.setSource(3);
                            var groupNum =
                                    createGroupMapper.selectCount(queryWrapper.eq("group_id", groupId));
                            if (groupNum == 0L) {
                                createGroupMapper.upsert(createGroup,
                                        Wrappers.lambdaUpdate(CreateGroup.class).eq(CreateGroup::getGroupId, groupId).set(CreateGroup::getSource, 3));
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
                    }

                } catch (Exception e) {
                    log.error(e.getMessage());
                }
                log.info(groupId + "同步耗时：{}", System.currentTimeMillis() - l1);
            });
            log.info("同步群组信息结束，耗时：{}", System.currentTimeMillis() - l);
        }
    }

    @Override
    public ImUserDeptNodeInfoVO userDeptNodeInfo(Long userId) {
        log.info("userDeptNodeInfo userId:{}", userId);
        ImUserDeptNodeInfoVO result = new ImUserDeptNodeInfoVO();
        UserGetVo userGetVo = imHttpClient.userPageById(String.valueOf(userId));
        if (userGetVo == null || CollectionUtils.isEmpty(userGetVo.getResults())) {
            log.error("userDeptNodeInfo userId:{} userGetVo is empty", userId);
            result.setNodes(Collections.emptyList());
            result.setDepartments(Collections.emptyList());
            return result;
        }
        ImUser imUser = userGetVo.getResults().get(0);
        result.setDepartments(queryUserDepartments(imUser));
//        result.setNodes(queryPeerNodes(userId));
        return result;
    }

    @Override
    public String getPeerNodeGateWayPrefix() {
        return globalsRpcService.getGlobalsValueByName(Constants.LINKX_IN_CENTER_GATEWAY_PREFIX);
    }

    /**
     * 查询用户组织部门信息
     */
    private List<ImUserDeptInfoVO> queryUserDepartments(ImUser imUser) {
        List<ImUser.UserDepartment> userDepartments = imUser.getUserDepartments();
        if (CollectionUtils.isEmpty(userDepartments)) {
            return new ArrayList<>();
        }
        log.info("userDeptNodeInfo userDepartments size:{}", userDepartments.size());
        return userDepartments.stream().map(item -> {
            ImUserDeptInfoVO vo = new ImUserDeptInfoVO();
            BeanCopyUtils.copyBean(item, vo);
            vo.setDepartmentId(item.getId());
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 查询所有对端节点上的用户信息（含版本过滤）
     */
//    private List<ImUserNodeInfoVO> queryPeerNodes(Long userId) {
//        List<ImUserNodeInfoVO> allNodes = new ArrayList<>();
//        try {
//            Map<String, Object> nodes = p2PDataFlowRpcApi.listNodes(Constants.H5);
//            if (CollectionUtils.isEmpty(nodes)) {
//                log.info("queryPeerNodes nodes is empty");
//                return allNodes;
//            }
//            log.info("queryPeerNodes nodes:{}", nodes);
//            allNodes.addAll(dispatchNodes(userId, getTypedNodes(nodes, Constants.LIST_NODES_KEY_CLIENTS, PeerNodeClientVO.class),
//                    node -> {
//                        Integer grant = node.getGrant();
//                        return grant != null && grant == 1;
//                    },
//                    PeerNodeClientVO::getName, PeerNodeClientVO::getIp,
//                    PeerNodeClientVO::getVersion, PeerNodeClientVO::getPeerId));
//            allNodes.addAll(dispatchNodes(userId, getTypedNodes(nodes, Constants.LIST_NODES_KEY_SERVERS, PeerNodeServerVO.class),
//                    node -> {
//                        Integer authorized = node.getAuthorized();
//                        return authorized != null && authorized == 1;
//                    },
//                    PeerNodeServerVO::getName, PeerNodeServerVO::getIp,
//                    PeerNodeServerVO::getVersion, PeerNodeServerVO::getPeerId));
//        } catch (Exception e) {
//            log.error("获取节点信息失败", e);
//        }
//        return allNodes;
//    }

    /**
     * 从 nodes Map 中安全提取指定 key 的 List（类型不匹配或为空时返回空列表）
     */
    @SuppressWarnings("unchecked")
    private <T> List<T> getTypedNodes(Map<String, Object> nodes, String key, Class<T> clazz) {
        Object object = nodes.get(key);
        if (!(object instanceof List)) {
            return Collections.emptyList();
        }
        List<?> raw = (List<?>) object;
        if (raw.isEmpty() || !clazz.isInstance(raw.get(0))) {
            return Collections.emptyList();
        }
        return (List<T>) raw;
    }

    /**
     * 并发 dispatch 各节点查询用户信息，单节点失败/超时不影响其他节点
     * userId 由调用方传入，各并发 future 共用同一 userId
     */
    private <T> List<ImUserNodeInfoVO> dispatchNodes(Long userId, List<T> nodes,
                                                     java.util.function.Predicate<T> authorizedPredicate,
                                                     java.util.function.Function<T, String> nameGetter,
                                                     java.util.function.Function<T, String> ipGetter,
                                                     java.util.function.Function<T, String> versionGetter,
                                                     java.util.function.Function<T, String> peerIdGetter) {
        if (CollectionUtils.isEmpty(nodes)) {
            return Collections.emptyList();
        }
        log.info("userDeptNodeInfo {} size:{}", nodes.get(0).getClass().getSimpleName(), nodes.size());
        List<CompletableFuture<ImUserNodeInfoVO>> futures = nodes.stream()
                .map(node -> CompletableFuture.supplyAsync(
                        () -> dispatchSingleNode(userId, node, authorizedPredicate, nameGetter,
                                ipGetter, versionGetter, peerIdGetter),
                        userNodeQueryExecutor))
                .collect(Collectors.toList());
        return collectFutures(futures);
    }

    /**
     * 查询单个节点上的用户信息
     */
    private <T> ImUserNodeInfoVO dispatchSingleNode(Long userId, T node,
                                                    java.util.function.Predicate<T> authorizedPredicate,
                                                    java.util.function.Function<T, String> nameGetter,
                                                    java.util.function.Function<T, String> ipGetter,
                                                    java.util.function.Function<T, String> versionGetter,
                                                    java.util.function.Function<T, String> peerIdGetter) {
        String peerId = peerIdGetter.apply(node);
        try {
            if (!authorizedPredicate.test(node)) {
                return null;
            }
            Integer status = p2PDataFlowRpcApi.getPeerNodeStatus(peerId);
            if (Objects.isNull(status) || status != NodeStatusEnum.AUTHENTICATED.getCode()) {
                return null;
            }
            DispatchResponseDTO responseEntity = p2PDataFlowRpcApi.dispatch(peerId, "/collaboration/v1/users/" + userId, null);
            if (Objects.isNull(responseEntity)) {
                return null;
            }
            if (responseEntity.getStatusCode() != HttpStatus.OK.value()) {
                return null;
            }
            byte[] body = responseEntity.getBody();
            if (Objects.isNull(body)) {
                return null;
            }
            R<ImUser> userR = JsonUtil.parseJson(body, new TypeReference<R<ImUser>>() {});
            if (Objects.isNull(userR)) {
                return null;
            }
            ImUser nodeUser = userR.getData();
            if (Objects.isNull(nodeUser) || nodeUser.getId() == null) {
                return null;
            }
            ImUserNodeInfoVO vo = new ImUserNodeInfoVO();
            vo.setUserId(nodeUser.getId());
            vo.setName(nodeUser.getName());
            ImUser.UserDepartment primaryDept = nodeUser.getPrimaryDepartment();
            if (primaryDept != null) {
                vo.setDepartmentId(primaryDept.getId());
            }
            vo.setDepartmentPeerNode(nameGetter.apply(node));
            vo.setDepartmentPeerNodeIP(ipGetter.apply(node));
            vo.setVersion(versionGetter.apply(node));

            DispatchResponseDTO prefixEntity = p2PDataFlowRpcApi.dispatch(peerId, "/collaboration/v1/users/getPeerNodeGateWayPrefix", null);
            if (Objects.nonNull(prefixEntity) && prefixEntity.getStatusCode() == HttpStatus.OK.value()) {
                byte[] prefixBody = prefixEntity.getBody();
                if (Objects.nonNull(prefixBody)) {
                    R<String> prefixR = JsonUtil.parseJson(prefixBody, new TypeReference<R<String>>() {});
                    if (Objects.nonNull(prefixR) && Objects.nonNull(prefixR.getData())) {
                        vo.setDepartmentPeerNodeGateWayPrefix(prefixR.getData());
                    }
                }
            }

            return vo;
        } catch (Exception e) {
            log.warn("节点 {} 查询失败", peerId, e);
            return null;
        }
    }

    /**
     * 汇总 future 结果，单节点超时 2s 避免拖垮整体
     */
    private List<ImUserNodeInfoVO> collectFutures(List<CompletableFuture<ImUserNodeInfoVO>> futures) {
        List<ImUserNodeInfoVO> result = new ArrayList<>();
        for (CompletableFuture<ImUserNodeInfoVO> f : futures) {
            try {
                ImUserNodeInfoVO vo = f.get(2, TimeUnit.SECONDS);
                if (vo != null) {
                    result.add(vo);
                }
            } catch (TimeoutException te) {
                f.cancel(true);
                log.warn("节点查询超时");
            } catch (Exception e) {
                log.warn("节点查询异常:{}", e.getMessage());
            }
        }
        return result;
    }
}