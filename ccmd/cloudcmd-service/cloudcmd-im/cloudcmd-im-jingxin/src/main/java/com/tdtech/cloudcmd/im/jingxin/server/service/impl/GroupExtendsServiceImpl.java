package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.auth.service.RoleRpcService;
import com.tdtech.cloudcmd.auth.controller.admin.vo.OrgPrivDto;
import com.tdtech.cloudcmd.base.api.service.GlobalsRpcService;
import com.tdtech.cloudcmd.bean.CagentMqFrame;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.OpenApiGroupCountVO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.UpdateGroupMemberCO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.UpdateGroupMemberResultVO;
import com.tdtech.cloudcmd.im.jingxin.client.CachedImConfig;
import com.tdtech.cloudcmd.im.jingxin.client.ImHttpClient;
import com.tdtech.cloudcmd.im.jingxin.client.entity.*;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.OpenApiGroupQO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.*;
import com.tdtech.cloudcmd.im.jingxin.server.enums.Constant;
import com.tdtech.cloudcmd.im.jingxin.server.service.CollaborationPostService;
import com.tdtech.cloudcmd.im.jingxin.server.service.GroupExtendsService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.*;
import com.tdtech.cloudcmd.im.jingxin.server.util.EncryptImUtil;
import com.tdtech.cloudcmd.im.jingxin.server.util.SQLiteDynamicUtil;
import com.tdtech.cloudcmd.im.jingxin.server.util.ZipUtils;
import com.tdtech.cloudcmd.msip.aop.LogReport;
import com.tdtech.cloudcmd.msip.aop.LogReportParam;
import com.tdtech.cloudcmd.msip.enums.AlarmTemplateZhEnum;
import com.tdtech.cloudcmd.msip.enums.OperationTypeEnum;
import com.tdtech.cloudcmd.msip.util.DiskUtil;
import com.tdtech.cloudcmd.msip.util.ReportUtil;
import com.tdtech.cloudcmd.redis.RedisLockFactory;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.IdWorker;
import com.tdtech.cloudcmd.util.json.JsonArray;
import com.tdtech.cloudcmd.util.json.JsonException;
import com.tdtech.cloudcmd.util.json.JsonObject;
import com.tdtech.cloudcmd.util.json.JsonUtil;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.tdtech.cloudcmd.util.StringUtils.isNullOrEmptyOrUndefined;


/**
 * @author ChinasoftPortal
 * @date 2025/9/10
 * @Describe：
 */
@Slf4j
@Service
public class GroupExtendsServiceImpl extends ServiceImpl<GroupExtendsMapper, GroupExtends> implements GroupExtendsService {

    @Resource
    private GroupExtendsMapper groupExtendsMapper;

    @Resource
    private TagMapper tagMapper;

    @Resource
    private LabelMapper labelMapper;
    @Resource
    private GroupTagMapper groupTagMapper;

    @Resource
    private ImHttpClient imHttpClient;

    @Resource(name = "groupInfoExecutorService")
    private TaskExecutor taskExecutor;
    @Resource
    private IdWorker idWorker;
    @Resource
    private SQLiteDynamicUtil sqliteDynamicUtil;

    @DubboReference
    RoleRpcService roleRpcService;

    private static final Integer ARCHIVING = 1; // 中归档
    private static final List<Integer> VALID_ARCHIVED_STATUSES = Arrays.asList(0, 1, 2, 3);
    private static final int MAX_FILE_PATH_LENGTH = 255;
    private static final String GROUP_ARCHIVING = "GROUP_ARCHIVING";
    private static final String GROUP_TAG = "GROUP_TAG";
    private static final String MSG_TOPIC = "cloudcmd-cagent";
    public static final String FILE_PATH_URL = "/collaboration/static/archive";
    private static final Path DEFAULT_FILE_PATH = Path.of("/home/linkx/im/default.png");
    private static final String DEFAULT_ICON_URL = "/collaboration/static/default.png";
    //private static final String DBPATH = "/home/pollink/archive/";

    private static final String MSIP_DISK_SAFE_SIZE = "MSIP_DISK_SAFE_SIZE";

    /**
     * 群组归档锁的key前缀
     */
    private static final String GROUP_ARCHIVE_LOCK_KEY_PREFIX = "group:archive:lock:";
    @Resource
    private StreamBridge streamBridge;
    @Resource
    private ImService imService;
    @DubboReference
    private GlobalsRpcService globalsRpcService;
    @Resource
    private CreateGroupMapper createGroupMapper;
    @Resource
    private UserGroupCareMapper userGroupCareMapper;

    @Resource
    private CachedImConfig cachedImConfig;

    @Resource
    private ReportUtil reportUtil;

    @Resource
    private CollaborationPostService collaborationPostService;

    @Resource
    private OrganizationDiversionService organizationDiversionService;

    @Autowired
    private EncryptImUtil encryptImUtil;

    @Autowired
    private RedisLockFactory redisLockFactory;

    @Autowired
    private CollaborationPostMapper collaborationPostMapper;


    @Override
    public GroupExtends getByGroupId(Long groupId) throws Exception {
        if (groupId == null || groupId <= 0) {
            throw new RuntimeException("Invalid group ID");
        }

        GroupExtends extendsInfo = groupExtendsMapper.selectByGroupId(groupId);
        if (extendsInfo == null) {
            throw new RuntimeException("Group extension info not found");
        }
        return extendsInfo;
    }

    @Override
    public boolean saveOrUpdateGroupExtends(GroupExtends groupExtends) throws Exception {
        validateGroupExtends(groupExtends);

        GroupExtends existing = groupExtendsMapper.selectByGroupId(groupExtends.getGroupId());
        if (existing != null) {
            groupExtends.setId(existing.getId());
            return updateById(groupExtends);
        } else {
            groupExtends.setGmtCreated(LocalDateTime.now());
            return save(groupExtends);
        }
    }

    @Override
    public boolean updateArchiveInfo(Long groupId, GroupExtends groupExtends) throws Exception {
        if (groupId == null || groupId <= 0) {
            throw new RuntimeException("Invalid group ID");
        }

        GroupExtends existing = groupExtendsMapper.selectByGroupId(groupId);
        if (existing == null) {
            throw new RuntimeException("Group extension info not found");
        }

        // Validate archive status
        if (groupExtends.getArchived() == null || !VALID_ARCHIVED_STATUSES.contains(groupExtends.getArchived())) {
            throw new RuntimeException("Invalid archive status (must be 0, 1, or 2)");
        }

        // Validate file paths
        validateFilePath(groupExtends.getArchivedFile(), "archived file");
        validateFilePath(groupExtends.getArchivedAttachxxxFile(), "multimedia file");

        // Set archive time for active statuses
        if (groupExtends.getArchived() == 1 || groupExtends.getArchived() == 2) {
            groupExtends.setArchivedTime(LocalDateTime.now());
        }

        return groupExtendsMapper.updateArchiveStatus(groupId, groupExtends) > 0;
    }

    private void validateGroupExtends(GroupExtends groupExtends) throws Exception {
        if (groupExtends.getGroupId() == null || groupExtends.getGroupId() <= 0) {
            throw new RuntimeException("Group ID is required");
        }

        if (groupExtends.getArchived() != null && !VALID_ARCHIVED_STATUSES.contains(groupExtends.getArchived())) {
            throw new RuntimeException("Invalid archive status (must be 0, 1, or 2)");
        }

        validateFilePath(groupExtends.getArchivedFile(), "archived file");
        validateFilePath(groupExtends.getArchivedAttachxxxFile(), "multimedia file");
    }

    private void validateFilePath(String path, String fieldName) throws Exception {
        if (StringUtils.hasText(path) && path.length() > MAX_FILE_PATH_LENGTH) {
            throw new RuntimeException(fieldName + " exceeds max length (" + MAX_FILE_PATH_LENGTH + ")");
        }
    }

    @Override
    public IPage<UserCareGroupDTO> getPageList(Integer pageNum, Integer pageSize, Integer archived, String archivedTime, String keywords, Integer groupType) throws Exception {
        // Validate page parameters
        if (pageNum == null || pageNum <= 0) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize <= 0) {
            pageSize = 10;
        }

        // Validate archive status if provided
        if (archived != null && !VALID_ARCHIVED_STATUSES.contains(archived)) {
            throw new RuntimeException("Invalid archive status: must be 0, 1 or 2");
        }

        List<Integer> archiveds = new ArrayList<>();
        if (archived == 2) {
            archiveds.add(archived);
        } else {
            archiveds = Arrays.asList(0, 1);
        }
        UserInfo user = SecurityUtils.getUser();
        if (Objects.isNull(user)) {
            throw new SecurityUtils.UnAuthException("access token invalid");
        }
        log.debug("getPageList SecurityUtils user:{}", user);
        Long userId = user.getUserId();
        List<String> orgIds = user.getImOrgPrivCodes();
        log.debug("getPageList SecurityUtils orgIds:{}", orgIds);
        // Create page object
        Page<UserCareGroupDTO> page = new Page<>(pageNum, pageSize);

        // 当前用户关联了哪些协同岗
        List<String> postIdList = findPostIdList(userId);
        // 当前列表的群成员
        Set<Long> memberIds = new HashSet<>();
        IPage<UserCareGroupDTO> pageList = groupExtendsMapper.selectPageList(page, userId, archiveds, archivedTime, keywords, orgIds, postIdList, groupType);
        for (UserCareGroupDTO userCareGroupDTO : pageList.getRecords()) {
            if (userCareGroupDTO.getArchivedUserId() != null) {
                var userInfo = roleRpcService.getUserInfoByUserId(userCareGroupDTO.getArchivedUserId() + "");
                userCareGroupDTO.setUserName(userInfo.getName());
            }
            // 判断是否是群主
            userCareGroupDTO.setIsOwner(Objects.equals(userId, userCareGroupDTO.getOwnerId()) ? 1 : 0);
            // 判断是否是群成员
            List<Long> memberIdsCurGroup = Arrays.stream(userCareGroupDTO.getMembers().split(","))
                    .filter(id -> !id.trim().isEmpty()).map(Long::parseLong).collect(Collectors.toList());
            userCareGroupDTO.setIsMember(memberIdsCurGroup.contains(userId) ? 1 : 0);
            userCareGroupDTO.setMemberIds(memberIdsCurGroup);
            memberIds.addAll(memberIdsCurGroup);
        }
        // 判断群组是否有协同岗
        checkHasCoopUser(pageList.getRecords(), memberIds);
        // Call mapper for page query
        return pageList;
    }

    @Override
    public void updateGroupTags(Long groupId, Long tagId, Long userId, boolean isPush) throws Exception {
        // 1. Validate group ID
        if (groupId == null || groupId <= 0) {
            throw new RuntimeException("Invalid group ID");
        }
        if(userId == null){
            // 2. Validate operator ID
            var user = SecurityUtils.getUser();
            if (Objects.isNull(user)) {
                throw new SecurityUtils.UnAuthException("access token invalid");
            }
            userId = user.getUserId();
        }

        log.debug("updateGroupTags SecurityUtils userId:{}", userId);

        if (tagId == null || tagId <= 0) {
            // 4. Delete existing tag relations for the group
            groupTagMapper.updateByGroupId(groupId);
            return;
        } else {
            Label label = labelMapper.selectLabelById(tagId);
            if (label == null) {
                throw new RuntimeException("Some tags do not exist");
            }
        }
        LambdaQueryWrapper<GroupTag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GroupTag::getGroupId, groupId);
        List<GroupTag> groupTags = groupTagMapper.selectList(queryWrapper);
        if (CollectionUtils.isNotEmpty(groupTags)) {
            groupTagMapper.updateByGroupId(groupId);
        }
        GroupTag groupTag = new GroupTag();
        groupTag.setGroupId(groupId);
        groupTag.setTagId(tagId);
        groupTag.setIsDeleted(0);
        groupTag.setCreateUserId(userId);
        groupTag.setGmtCreated(new Date());
        groupTagMapper.insert(groupTag);
        if(isPush){
            //归档完成后，推送所有在线前端消息，刷新页面
            var cagentMqFrame = new CagentMqFrame().toBuilder().typeSubSystemMessage(GROUP_TAG).broadcast()
                    .body(GROUP_TAG, GROUP_TAG, groupId).build();
            streamBridge.send(MSG_TOPIC, cagentMqFrame);
        }
    }

    @Override
    public void batchUpdateGroupTags(Long groupId, List<Long> tagIds, Long userId, boolean isPush) throws Exception {
        // 1. Validate group ID
        if (groupId == null || groupId <= 0) {
            throw new RuntimeException("Invalid group ID");
        }
        if(userId == null){
            // 2. Validate operator ID
            var user = SecurityUtils.getUser();
            if (Objects.isNull(user)) {
                throw new SecurityUtils.UnAuthException("access token invalid");
            }
            userId = user.getUserId();
        }
        LambdaQueryWrapper<GroupTag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GroupTag::getGroupId, groupId);
        List<GroupTag> groupTags = groupTagMapper.selectList(queryWrapper);
        if (CollectionUtils.isNotEmpty(groupTags)) {
            groupTagMapper.updateByGroupId(groupId);
        }
        Long finalUserId = userId;
        List<GroupTag> tagList = tagIds.stream().map(tagId -> {
            GroupTag groupTag = new GroupTag();
            groupTag.setGroupId(groupId);
            groupTag.setTagId(tagId);
            groupTag.setIsDeleted(0);
            groupTag.setCreateUserId(finalUserId);
            groupTag.setGmtCreated(new Date());
            return groupTag;
        }).collect(Collectors.toList());
        groupTagMapper.insert(tagList);
        if(isPush){
            //归档完成后，推送所有在线前端消息，刷新页面
            var cagentMqFrame = new CagentMqFrame().toBuilder().typeSubSystemMessage(GROUP_TAG).broadcast()
                    .body(GROUP_TAG, GROUP_TAG, groupId).build();
            streamBridge.send(MSG_TOPIC, cagentMqFrame);
        }
    }


    @Override
    public IPage<UserCareGroupDTO> getTagsPageList(Integer pageNum, Integer pageSize, Integer archived, String archivedTime,
                                                   String keywords, String groupName, String tagName, Integer groupType, Integer scope) throws Exception {
        // Validate page parameters
        if (pageNum == null || pageNum <= 0) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize <= 0) {
            pageSize = 10;
        }
        // Validate archive status if provided
        if (archived != null && !VALID_ARCHIVED_STATUSES.contains(archived)) {
            throw new RuntimeException("Invalid archive status: must be 0, 1, 2 or 3");
        }

        UserInfo user = SecurityUtils.getUser();
        if (Objects.isNull(user)) {
            throw new SecurityUtils.UnAuthException("access token invalid");
        }
        log.debug("getTagsPageList SecurityUtils user:{}", user);
        Long userId = user.getUserId();
        List<String> orgIds = user.getImOrgPrivCodes();
        log.debug("getPageList SecurityUtils orgIds:{}", orgIds);
        // Create page object
        Page<UserCareGroupDTO> page = new Page<>(pageNum, pageSize);

        // 当前用户关联了哪些协同岗
        List<String> postIdList = findPostIdList(userId);

        // Call mapper for page query
        IPage<UserCareGroupDTO> pageList = groupExtendsMapper.getTagsPageList(page, userId, archived, archivedTime, keywords, groupName, tagName, orgIds, postIdList, groupType, scope);
        // 当前列表的群成员
        Set<Long> memberIds = new HashSet<>();
        for (UserCareGroupDTO userCareGroupDTO : pageList.getRecords()) {
            if (userCareGroupDTO.getArchivedUserId() != null) {
                var userInfo = roleRpcService.getUserInfoByUserId(userCareGroupDTO.getArchivedUserId() + "");
                if (Objects.nonNull(userInfo)) {
                    userCareGroupDTO.setUserName(userInfo.getName());
                }
            }
            // 判断是否是群主
            userCareGroupDTO.setIsOwner(Objects.equals(userId, userCareGroupDTO.getOwnerId()) ? 1 : 0);
            // 判断是否是群成员
            String members = userCareGroupDTO.getMembers();
            if (org.apache.commons.lang3.StringUtils.isBlank(members)) {
                userCareGroupDTO.setIsMember(0);
                // 计算scope: 1=我创建的; 3=可查看但不是成员
                userCareGroupDTO.setScope(userCareGroupDTO.getIsOwner() == 1 ? 1 : 3);
                continue;
            }
            List<Long> memberIdsCurGroup = Arrays.stream(userCareGroupDTO.getMembers().split(","))
                    .filter(id -> !id.trim().isEmpty()).map(Long::parseLong).collect(Collectors.toList());
            userCareGroupDTO.setIsMember(memberIdsCurGroup.contains(userId) ? 1 : 0);
            userCareGroupDTO.setMemberIds(memberIdsCurGroup);
            memberIds.addAll(memberIdsCurGroup);
            // 计算scope: 1=我创建的; 3=可查看但不是成员; 4=我是成员
            if (userCareGroupDTO.getIsOwner() == 1) {
                userCareGroupDTO.setScope(1);
            } else if (userCareGroupDTO.getIsMember() == 1) {
                userCareGroupDTO.setScope(4);
            } else {
                userCareGroupDTO.setScope(3);
            }
        }
        // 判断群组是否有协同岗
        checkHasCoopUser(pageList.getRecords(), memberIds);
        return pageList;
    }

    private void checkHasCoopUser(List<UserCareGroupDTO> userCareGroupDTOList, Set<Long> memberIds) {
        log.debug("checkHasCoopUser memberIds: {}", memberIds);
        List<Long> validMemberIds = memberIds == null ? Collections.emptyList()
                : memberIds.stream().filter(Objects::nonNull).collect(Collectors.toList());
        List<Long> postIds;
        if (CollectionUtils.isNotEmpty(validMemberIds)) {
            postIds = collaborationPostMapper.getByIds(validMemberIds);
        } else {
            postIds = new ArrayList<>();
        }
        Set<Long> postIdSet = CollectionUtils.isEmpty(postIds) ? new HashSet<>() : new HashSet<>(postIds);
        for (UserCareGroupDTO userCareGroupDTO : userCareGroupDTOList) {
            userCareGroupDTO.setHasCoopUser(Collections.disjoint(postIdSet,userCareGroupDTO.getMemberIds()) ? 0 : 1);
        }
    }

    private List<String> findPostIdList(Long userId) {
        var data = collaborationPostService.listByUserId(String.valueOf(userId));
        if (CollectionUtils.isEmpty(data)) {
            return new ArrayList<>();
        }
        return data.stream().map(CollaborationPost::getId)
                .map(String::valueOf)
                .collect(Collectors.toList());
    }

    @Override
    public List<ArchiveTimelineDTO> getArchiveTimeline(String keywords) {
        UserInfo user = SecurityUtils.getUser();
        if (Objects.isNull(user)) {
            throw new SecurityUtils.UnAuthException("access token invalid");
        }
        log.debug("getArchiveTimeline SecurityUtils user:{}", user);
        Long userId = user.getUserId();
        List<String> orgIds = user.getImOrgPrivCodes();
        log.debug("getPageList SecurityUtils orgIds:{}", orgIds);

        // 当前用户关联了哪些协同岗
        List<String> postIdList = findPostIdList(userId);

        // 先查询所有月份（不带搜索条件）
        List<ArchiveTimelineDTO> allTimelineList = groupExtendsMapper.selectCombinedArchiveTimeline(userId, null, orgIds, postIdList);

        // 如果有搜索条件，查询匹配的月份并更新count
        if (!isNullOrEmptyOrUndefined(keywords)) {
            List<ArchiveTimelineDTO> matchedTimelineList = groupExtendsMapper.selectCombinedArchiveTimeline(userId, keywords, orgIds, postIdList);

            Map<String, Long> matchedCountMap = matchedTimelineList.stream()
                    .collect(Collectors.toMap(ArchiveTimelineDTO::getYearMonth, ArchiveTimelineDTO::getCount));

            for (ArchiveTimelineDTO dto : allTimelineList) {
                dto.setCount(matchedCountMap.getOrDefault(dto.getYearMonth(), 0L));
            }
        }

        return CollectionUtils.isEmpty(allTimelineList) ? Collections.emptyList() : allTimelineList;
    }

    @Override
    public List<TagCountDTO> getCountGroupTags(Long userId, String departmentCode, String startTime, String endTime) {
        UserInfo user = SecurityUtils.getUser();
        log.debug("getCountGroupTags SecurityUtils user:{}", user);
        List<String> orgIds = new ArrayList<>();
        if (user != null) {
            orgIds = user.getImOrgPrivCodes();
            log.debug("getPageList SecurityUtils orgIds:{}", orgIds);
        }
        log.debug("getCountGroupTags SecurityUtils orgIds:{}", orgIds);
        List<String> departmentCodeList = new ArrayList<>();
        if (com.tdtech.cloudcmd.util.StringUtils.isNotBlank(departmentCode)) {
            // 查找当前部门编号
            List<ImDepartment> imDepartments = organizationDiversionService.queryDepartmentForList(departmentCode);
            departmentCodeList = imDepartments.stream().map(ImDepartment::getCode).collect(Collectors.toList());
        }

        // 当前用户关联了哪些协同岗
        List<String> postIdList = findPostIdList(user.getUserId());
        List<TagCountDTO> result = new ArrayList<>();

        // 查询全部群组
        TagCountDTO allGroupCount = groupTagMapper.getAllGroupCount(user.getUserId(), departmentCodeList, orgIds, startTime, endTime, postIdList);
        if (allGroupCount != null) {
            result.add(allGroupCount);
        }

        List<TagCountDTO> countGroupTags = groupTagMapper.getCountGroupTags(user.getUserId(), departmentCodeList, orgIds, startTime, endTime, postIdList);
        if (CollectionUtils.isNotEmpty(countGroupTags)) {
            result.addAll(countGroupTags);
        }
        TagCountDTO allGroup = groupTagMapper.getAllGroup(user.getUserId(), departmentCodeList, orgIds, startTime, endTime, postIdList);
        if (allGroup != null) {
            result.add(allGroup);
        }
        return result;
    }

    @Override
    public Integer updateArchive(Long groupId, Long userId) {
        log.debug("updateArchive SecurityUtils getUserId:{}", userId);
        if (groupId == null || groupId <= 0) {
            throw new RuntimeException("Invalid group ID");
        }

        GroupExtends groupExtend = groupExtendsMapper.selectByGroupId(groupId);
        if (groupExtend == null) {
            throw new RuntimeException("Group extension info not found");
        }
        if (!Integer.valueOf(0).equals(groupExtend.getArchived())) {
            String msg = ARCHIVING.equals(groupExtend.getArchived()) ? "群组正在归档中，请勿重复操作" : "群组已归档，请勿重复操作";
            throw new BusinessException(msg);
        }
        groupExtend.setArchivedTime(LocalDateTime.now());
        groupExtend.setArchivedUserId(userId);
        groupExtend.setArchived(ARCHIVING);
        LambdaUpdateWrapper<GroupExtends> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(GroupExtends::getGroupId, groupId)
                .eq(GroupExtends::getArchived, 0)
                .set(GroupExtends::getArchived, ARCHIVING)
                .set(GroupExtends::getArchivedTime, groupExtend.getArchivedTime())
                .set(GroupExtends::getArchivedUserId, userId);
        int rows = groupExtendsMapper.update(null, updateWrapper);
        if (rows == 0) {
            throw new BusinessException("群组正在归档中或已归档，请勿重复操作");
        }
        // 冻结群组,暂时去掉该功能
        String token = SecurityUtils.getToken();
        asyncArchive(groupId, userId, token, groupExtend, true);
        return ARCHIVING;
    }

    private boolean isSafeSize(String path) {
        // 磁盘告警阈值
        try {
            String configSize = cachedImConfig.getConfig(MSIP_DISK_SAFE_SIZE);
            Integer safeSize = Objects.isNull(configSize) ? 15 : Integer.valueOf(configSize);
            boolean enoughSpace = DiskUtil.isEnoughSpace(path, safeSize);
            log.info("judgeAndReportAlarm path: {}, safeSize: {}", enoughSpace, safeSize);
            if (enoughSpace) {
                // 空间足够，擦除告警
                clearAlarm();
                return true;
            } else {
                log.warn("归档空间不足");
                // 空间不足，告警
                reportAlarm(safeSize);
            }
        } catch (Exception e) {
            log.error("judgeAndReportAlarm error:  {}", e.getMessage());
        }
        return false;
    }

    private void reportAlarm(Integer safeSize) {
        reportUtil.saveAlarm2MSIP(AlarmTemplateZhEnum.INSUFFICIENT_DISK_SPACE, safeSize);
    }

    private void clearAlarm() {
        reportUtil.clearAlarm2MSIP(AlarmTemplateZhEnum.INSUFFICIENT_DISK_SPACE);
    }

    @Override
    public void asyncArchive(Long groupId, Long userId, String operationUserToken, GroupExtends groupExtend, Boolean isArchive) {
        String dbpathGlobal = globalsRpcService.getGlobalsValueByName("DBPATH") + "/";
        // 计算磁盘大小，并告警或者擦除告警
        boolean isSafeSize = isSafeSize(dbpathGlobal);
        if (!isSafeSize) {
            throw new BusinessException("归档空间不足");
        }
        taskExecutor.execute(() -> {
            RedisLockFactory.RedisLock lock = redisLockFactory.newRedisLock(GROUP_ARCHIVE_LOCK_KEY_PREFIX + groupId,
                    Duration.ofMinutes(30));
            if (lock.tryLock(30L, TimeUnit.SECONDS)) {
                try {
                    // 执行群组归档
                    doArchive(groupId, userId, operationUserToken, groupExtend, isArchive, dbpathGlobal);
                }catch (Exception e){
                    log.error("群组{}归档失败", groupId, e);
                } finally {
                    lock.unlock();
                }
            } else {
                log.warn("获取分布式锁失败, 跳过群组{}归档", groupId);
            }
        });
    }

    private void doArchive(Long groupId, Long userId, String operationUserToken, GroupExtends groupExtend, Boolean isArchive, String dbpathGlobal) {
        String groupPath = null;
        try {
//                String dbPath = dbpathGlobal + groupId + "/group_" + groupId + ".db";
//                Path groupDir = Path.of(dbpathGlobal).resolve(String.valueOf(groupId)); // 群组目录：FILE_PATH/groupId
//                Files.createDirectories(groupDir);
            // 创建群组目录、解密db文件供读写, 若加密的db文件不存在，不做处理，由sqlite创建写临时文件
            String dbFileName = "group_" + groupId + ".db";
            groupPath = encryptImUtil.getGroupPathAndCreate(dbpathGlobal, String.valueOf(groupId));
            String dbPath = encryptImUtil.encryptEnabled() ? encryptImUtil.getFileToWrite(groupPath, dbFileName, String.valueOf(groupId))
                    : groupPath + File.separator + dbFileName;
            log.info("updateArchive getIMGroupInfo dbPath:{}", dbPath);
            // /openapi/v2/group/{id} 获取群成员
            GroupVo groupVo = imHttpClient.queryGroupDetail(groupId);
            List<GroupMembers> groupMembers = groupVo.getGroupMembers();
            log.info("updateArchive getIMGroupInfo groupMembers:{}", groupMembers);
            if (CollectionUtils.isNotEmpty(groupMembers)) {
                List<TbChatMember> tbChatMembers = new ArrayList<>();

                String tableName = "tb_chat_member_" + groupId;
                sqliteDynamicUtil.createTable(dbPath, tableName, groupId);
                //dynamicChatMemberMapper.createTableIfNotExists(groupId);
                //删除整表数据
                sqliteDynamicUtil.deleteData(dbPath, tableName, null, null);
                //dynamicChatMemberMapper.deleteAllMembers(groupId);
                for (GroupMembers groupMember : groupMembers) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", groupMember.getUserId());
                    data.put("code", groupMember.getIdCard());
                    data.put("name", groupMember.getName());
                    data.put("alias", groupMember.getAlias());
//                        data.put("tumb_avatar", downloadSaveImageById(String.valueOf(groupMember.getUserId()),groupMember.getAvatar(), groupMember.getAvatar() + ".png", groupId, dbpathGlobal));
                    data.put("tumb_avatar", downloadSaveImageByIdEncrypt(String.valueOf(groupMember.getUserId()),groupMember.getAvatar(), groupMember.getAvatar() + ".png", groupId, dbpathGlobal));
                    data.put("gender", groupMember.getGender());
                    data.put("gender_name", groupMember.getGenderName());
                    data.put("mobile", groupMember.getMobile());
                    data.put("email", groupMember.getEmail());
                    data.put("isdn", groupMember.getIsdn());
                    data.put("direct_leader_id", groupMember.getDirectLeaderId());
                    data.put("status", groupMember.getStatus());
                    data.put("status_name", groupMember.getStatusName());
                    data.put("user_alias", groupMember.getUserAlias());
                    data.put("idcard", groupMember.getIdCard());
                    data.put("role", groupMember.getRole());
                    data.put("mute_type", groupMember.getMuteType());
                    data.put("join_type", groupMember.getJoinType());
                    data.put("invite_id", groupMember.getInviteId());
                    data.put("invite_time", groupMember.getInviteTime());
                    sqliteDynamicUtil.insertData(dbPath, tableName, data);
                }
                //dynamicChatMemberMapper.batchInsertMembers(groupId, tbChatMembers);
            }
            // messages/offline/page 获取群消息， IndexedDB会话消息表,递归循环查找所有消息
            downloadChatMessage(groupId, userId, 1, dbPath, dbpathGlobal);
            // 群组归档完成，临时文件加密，并删除读/写临时文件
            encryptImUtil.commitWrite(dbPath, String.valueOf(groupId));
        } catch (Exception ex) {
            log.error("asyncArchive error, groupId: {}", groupId, ex);
        }
        if (isArchive) {
            GroupExtends latestGroupExtend = groupExtendsMapper.selectByGroupId(groupId);
            if (latestGroupExtend == null || !ARCHIVING.equals(latestGroupExtend.getArchived())) {
                log.warn("群组{}当前状态非归档中，跳过冻结操作, archived:{}", groupId,
                        latestGroupExtend != null ? latestGroupExtend.getArchived() : null);
                return;
            }
            try {
                // 先冻结群组，如果失败，则不修改群组归档状态
                imHttpClient.freeze(groupId);

                groupExtend.setArchivedTime(LocalDateTime.now());
                groupExtend.setArchivedUserId(userId);
                groupExtend.setArchived(2);
//                    groupExtend.setArchivedFile(dbpathGlobal + groupId);
                groupExtend.setArchivedFile(groupPath);
                groupExtendsMapper.updateArchiveStatus(groupId, groupExtend);
                send2CAgent(String.valueOf(groupId), String.valueOf(userId), operationUserToken, "1");
            } catch (Exception e) {
                log.error("asyncArchive freeze error, groupId: {}", groupId, e);
                // 冻结失败，则归档失败状态应该重新变回未归档状态
                groupExtend.setArchived(0);
                //是否要保留，可定位归档失败的时间
//                groupExtend.setArchivedUserId(null);
//                groupExtend.setArchivedTime(null);
                groupExtendsMapper.updateArchiveStatus(groupId, groupExtend);
                send2CAgent(String.valueOf(groupId), String.valueOf(userId), operationUserToken, "0");
            }
        }
    }

    private void send2CAgent(String groupId, String userId, String operationUserToken, String isArchive) {
        boolean validParameters = StringUtils.hasText(groupId) && StringUtils.hasText(userId) && StringUtils.hasText(operationUserToken);
        if (!validParameters) {
            log.info("send2CAgent validParameters is false: {}, {}, {}", groupId, userId, operationUserToken);
            return;
        }
        Map<String, String> dataMap = Map.of(
                "groupId", groupId,
                "userId", userId,
                "token", operationUserToken,
                "isArchive", isArchive);
        var cagentMqFrame = new CagentMqFrame().toBuilder().typeSubSystemMessage(GROUP_ARCHIVING).broadcast()
                .body(GROUP_ARCHIVING, GROUP_ARCHIVING, dataMap).build();
        streamBridge.send(MSG_TOPIC, cagentMqFrame);
        log.info("group asyncArchive send msg topic: {}, cagentMqFrame: {}", MSG_TOPIC, cagentMqFrame);
    }

    private void downloadChatMessage(Long groupId, Long userId, Integer fromSessionSeqId, String dbPath, String dbpathGlobal) {
        downloadChatMessageInternal(groupId, userId, fromSessionSeqId, dbPath, dbpathGlobal, true);
    }

    /**
     * 下载群消息内部方法
     * @param groupId 群组ID
     * @param userId 用户ID
     * @param fromSessionSeqId 起始session_seq_id
     * @param dbPath 数据库路径
     * @param dbpathGlobal 全局数据库路径
     * @param isFirstCall 是否第一次调用（用于判断是否需要查询最大session_seq_id）
     */
    private void downloadChatMessageInternal(Long groupId, Long userId, Integer fromSessionSeqId, String dbPath, String dbpathGlobal, boolean isFirstCall) {
        String tableName = "tb_chat_group_" + groupId;

        // 只有第一次调用时才查询最大session_seq_id
        if (isFirstCall) {
            // 增量查询逻辑：先查询本地最大的session_seq_id
            Long maxSessionSeqId = null;
            try {
                maxSessionSeqId = sqliteDynamicUtil.getMaxSessionSeqId(dbPath, groupId);
                log.info("downloadChatMessage groupId: {}, maxSessionSeqId: {}", groupId, maxSessionSeqId);
            } catch (Exception e) {
                log.warn("downloadChatMessage getMaxSessionSeqId error, will create table: {}", e.getMessage());
            }

            // 如果表不存在或为空（maxSessionSeqId为0），则创建表并从1开始查询
            if (maxSessionSeqId == null || maxSessionSeqId == 0) {
                sqliteDynamicUtil.createTable(dbPath, tableName, groupId);
                fromSessionSeqId = 1;
                log.info("downloadChatMessage create table and start from 1, groupId: {}", groupId);
            } else {
                // 增量查询：从最大session_seq_id + 1开始
                fromSessionSeqId = maxSessionSeqId.intValue() + 1;
                log.info("downloadChatMessage incremental query from sessionSeqId: {}, groupId: {}", fromSessionSeqId, groupId);
            }
        }

        MessagesOfflineReq messagesOfflineReq = new MessagesOfflineReq();
        //TODO 用户ID为空定时任务，不能为空
        messagesOfflineReq.setUserId(userId);
        messagesOfflineReq.setType(1);
        messagesOfflineReq.setScope(1);
        messagesOfflineReq.setPlaintext(1);
        messagesOfflineReq.setSessionId(String.valueOf(groupId));
        messagesOfflineReq.setCategory(2);
        messagesOfflineReq.setFromSessionSeqId(fromSessionSeqId);
        IMOfflineMsgPageVo imOfflineMsg = imHttpClient.getIMOfflineMsg(messagesOfflineReq);
        if (imOfflineMsg != null) {
            List<IMOfflineMsgVo> imMsgs = imOfflineMsg.getImMsgs();
            if (CollectionUtils.isNotEmpty(imMsgs)) {

                for (IMOfflineMsgVo imOfflineMsgVo : imMsgs) {
                    Map<String, Object> data = new HashMap<>();
                    IMOfflineMsgItemVo offlineMsgItem = imOfflineMsgVo.getData();
                    data.put("msg", offlineMsgItem.getMsg());
                    if (imOfflineMsgVo.getNotifyType().equals("MEDIA_MSG")) {
                        log.info("asyncArchive img msg");
                        var msg = offlineMsgItem.getMsg();
                        // 撤回的彩信消息没有msg
                        if (Objects.nonNull(msg)) {
                            var imageMsg = JsonUtil.convert(msg, IMOfflineMsgItemVo.MMSMsgVo.class);
//                            String filePath = downloadSaveImageById(imageMsg.getFileKey(),imageMsg.getFileKey(), imageMsg.getFileName(), groupId, dbpathGlobal);
                            String filePath = downloadSaveImageByIdEncrypt(imageMsg.getFileKey(),imageMsg.getFileKey(), imageMsg.getFileName(), groupId, dbpathGlobal);
                            imageMsg.setFilePath(filePath);
                            data.put("msg", JsonUtil.parseJson(JsonUtil.toJsonStr(imageMsg)));
                        }
                    } else if (imOfflineMsgVo.getNotifyType().equals("MERGE_FORWARD")) {
                        var msg = offlineMsgItem.getMsg();
                        var mergeForwardMsgVo = JsonUtil.convert(msg, IMOfflineMsgItemVo.MergeForwardMsgVo.class);
                        List<IMOfflineMsgItemVo.ForwardMsg> forwardMsgs = mergeForwardMsgVo.getForwardMsgs();
                        //递归处理合并转发消息
                        processMergeForwardMsg(forwardMsgs, groupId, dbpathGlobal);
                        data.put("msg", JsonUtil.parseJson(JsonUtil.toJsonStr(mergeForwardMsgVo)));
                    } else if (imOfflineMsgVo.getNotifyType().equals("NAME_CARD")) {
                        var msg = offlineMsgItem.getMsg();
                        IMOfflineMsgItemVo.BCOfflineMsgVo bcOfflineMsgVo = new IMOfflineMsgItemVo.BCOfflineMsgVo();
                        JsonObject jsonObject = JsonUtil.parseJson(JsonUtil.toJsonStr(msg));
                        String cardType = jsonObject.get("cardType").toString();
                        bcOfflineMsgVo.setCardType(cardType);
                        if (cardType.equals("userCard")) {
                            bcOfflineMsgVo.setUserCardData(JsonUtil.convert(jsonObject.get("data"), IMOfflineMsgItemVo.UserCardVo.class));
                        } else if (cardType.equals("officialAccounts")) {
                            bcOfflineMsgVo.setOfficialAccountsData(JsonUtil.convert(jsonObject.get("data"), IMOfflineMsgItemVo.OfficialAccountsVo.class));
                        } else if (cardType.equals("sharedMsg")) {
                            bcOfflineMsgVo.setSharedMsgData(JsonUtil.convert(jsonObject.get("data"), IMOfflineMsgItemVo.SharedMsgVo.class));
                        } else if (cardType.equals("officialAccountCode")) {
                            bcOfflineMsgVo.setOfficialAccountsCodeData(JsonUtil.convert(jsonObject.get("data"), IMOfflineMsgItemVo.OfficialAccountsCodeData.class));
                        } else if (cardType.equals("shareConfCard")) {
                            bcOfflineMsgVo.setShareConfCardData(JsonUtil.convert(jsonObject.get("data"), IMOfflineMsgItemVo.ShareConfCardData.class));
                        } else if (cardType.equals("customCard")) {
                            bcOfflineMsgVo.setCustomCardData(JsonUtil.convert(jsonObject.get("data"), IMOfflineMsgItemVo.CustomCardVo.class));
                        } else if (cardType.equals("selfDefineCard")) {
                            JsonObject jsonData = jsonObject.getJSONObject("data");
                            JsonArray array = jsonData.getJSONArray("properties");
                            IMOfflineMsgItemVo.SelfDefineCardVo vo = toBean(array);
                            bcOfflineMsgVo.setSelfDefineCard(vo);
                        }
                        // 处理名片的头像、部门的问题
                        deal4UserCard(bcOfflineMsgVo.getUserCardData(), groupId, dbpathGlobal);
                        data.put("msg", JsonUtil.parseJson(JsonUtil.toJsonStr(bcOfflineMsgVo)));
                    } else if (imOfflineMsgVo.getNotifyType().equals("GROUP_NOTE")) {
                        var msg = offlineMsgItem.getMsg();
                        var groupNoteMsgVo = JsonUtil.convert(msg, IMOfflineMsgItemVo.GroupNoteMsgVo.class);
                        data.put("msg", JsonUtil.parseJson(JsonUtil.toJsonStr(groupNoteMsgVo)));
                    }
                    Object msg = data.get("msg");
                    if(Objects.nonNull(msg)){
                        data.put("category", imOfflineMsgVo.getData().getCategory());
                        data.put("msg_type", imOfflineMsgVo.getData().getMsgType());
                        data.put("forward_msg", imOfflineMsgVo.getData().getForwardMsg());
                        data.put("'from'", imOfflineMsgVo.getData().getFrom());
                        data.put("from_real_user_id", imOfflineMsgVo.getData().getFromRealUserId());
                        data.put("from_isdn", imOfflineMsgVo.getData().getFromIsdn());
                        data.put("'to'", imOfflineMsgVo.getData().getTo());
                        data.put("to_type", imOfflineMsgVo.getData().getToType());
                        data.put("from_type", imOfflineMsgVo.getData().getFromType());
                        data.put("to_isdn", imOfflineMsgVo.getData().getToIsdn());
                        data.put("msg_id", imOfflineMsgVo.getData().getMsgId());
                        data.put("one_by_one_msg", imOfflineMsgVo.getData().getOneByOneMsg());
                        data.put("plaintext", imOfflineMsgVo.getData().getPlaintext());
                        data.put("read", imOfflineMsgVo.getData().getRead());
                        data.put("seq", imOfflineMsgVo.getData().getSeq());
                        data.put("session_seq_id", imOfflineMsgVo.getData().getSessionSeqId());
                        data.put("time", imOfflineMsgVo.getData().getTime());
                        data.put("withdraw", imOfflineMsgVo.getData().getWithdraw());
                        data.put("client_msg_id", imOfflineMsgVo.getData().getClientMsgId());
                        // msg消息为空插入会报错，表里面msg为notnull，所以不为空才插入
                        sqliteDynamicUtil.insertData(dbPath, tableName, data);
                    }
                    //imOfflineMsgItemVos.add(imOfflineMsgVo.getData());

                }
            }
            if (!imOfflineMsg.getIsEnd()) {
                fromSessionSeqId = fromSessionSeqId + 100;
                downloadChatMessageInternal(groupId, userId, fromSessionSeqId, dbPath, dbpathGlobal, false);
            }
        }
    }

    /**
     * 处理用户名片的部门和头像的存储
     */
    private void deal4UserCard(IMOfflineMsgItemVo.UserCardVo userCardData, Long groupId, String dbpathGlobal) {
        if (Objects.isNull(userCardData) || com.tdtech.cloudcmd.util.StringUtils.isBlank(userCardData.getUserId())) {
            log.warn("deal4UserCard userCard is null");
            return;
        }
        UserGetVo userGetVo = imHttpClient.userPageById(userCardData.getUserId());
        if (Objects.isNull(userGetVo) || CollectionUtils.isEmpty(userGetVo.getResults())) {
            log.warn("deal4UserCard userCard can not find user from im");
            return;
        }
        ImUser imUser = userGetVo.getResults().get(0);
        List<ImUser.UserDepartment> userDepartments = imUser.getUserDepartments();
        if (CollectionUtils.isNotEmpty(userDepartments)) {
            userCardData.setDepartment(userDepartments.get(0).getFullPathName());
        }
        if (com.tdtech.cloudcmd.util.StringUtils.isNotBlank(imUser.getAvatar())) {
//            userCardData.setAvatar(downloadSaveImageById(String.valueOf(userCardData.getUserId()), imUser.getAvatar(),
//                    imUser.getAvatar() + ".png", groupId, dbpathGlobal + "userCard"));
            userCardData.setAvatar(downloadSaveImageByIdEncrypt(String.valueOf(userCardData.getUserId()), imUser.getAvatar(),
                    imUser.getAvatar() + ".png", groupId, dbpathGlobal + "userCard"));
        }
    }


    private IMOfflineMsgItemVo.SelfDefineCardVo toBean(JsonArray array) {
        IMOfflineMsgItemVo.SelfDefineCardVo card = new IMOfflineMsgItemVo.SelfDefineCardVo();
        for (int i = 0; i < array.size(); i++) {
            JsonObject obj = array.getJSONObject(i);
            String name = obj.getString("propertyName");
            String value = obj.getString("propertyValue");
            switch (name) {
                case "msgtype":
                    card.setMsgType(value);
                    break;
                case "selfDefineType":
                    card.setSelfDefineType(value);
                    break;
                case "callee":
                    card.setCallee(value);
                    break;
                case "calleeName":
                    card.setCalleeName(value);
                    break;
                case "department":
                    card.setDepartment(value);
                    break;
                case "deviceType":
                    card.setDeviceType(value);
                    break;
                case "thumb":
                    card.setThumb(value);
                    break;
                case "PTZControl":
                    card.setPtzControl(value);
                    break;
                case "confirm":
                    card.setConfirm(value);
                    break;
                case "deleteState":
                    card.setDeleteState(value);
                    break;
            }
        }
        return card;
    }

    private void processMergeForwardMsg(List<IMOfflineMsgItemVo.ForwardMsg> forwardMsgs, Long groupId, String dbpathGlobal) {
        if (CollectionUtils.isNotEmpty(forwardMsgs)) {
            for (IMOfflineMsgItemVo.ForwardMsg forwardMsg : forwardMsgs) {
                if (forwardMsg.getMsgType().equals(2)) {
                    // 转发消息暂时不确定唯一id
//                    String filePath = downloadSaveImageById(String.valueOf(forwardMsg.getFileKey()),String.valueOf(forwardMsg.getFileKey()), forwardMsg.getFileName(), groupId, dbpathGlobal);
                    String filePath = downloadSaveImageByIdEncrypt(String.valueOf(forwardMsg.getFileKey()),String.valueOf(forwardMsg.getFileKey()), forwardMsg.getFileName(), groupId, dbpathGlobal);
                    forwardMsg.setFilePath(filePath);
                }
                if (forwardMsg.getMsgType().equals(5)) {
                    String cardType = forwardMsg.getCardType();
                    if (cardType.equals("userCard")) {
                        forwardMsg.setUserCardData(JsonUtil.convert(forwardMsg.getData(), IMOfflineMsgItemVo.UserCardVo.class));
                        // 处理名片的头像、部门的问题
                        deal4UserCard(forwardMsg.getUserCardData(), groupId, dbpathGlobal);
                    } else if (cardType.equals("officialAccounts")) {
                        forwardMsg.setOfficialAccountsData(JsonUtil.convert(forwardMsg.getData(), IMOfflineMsgItemVo.OfficialAccountsVo.class));
                    } else if (cardType.equals("sharedMsg")) {
                        forwardMsg.setSharedMsgData(JsonUtil.convert(forwardMsg.getData(), IMOfflineMsgItemVo.SharedMsgVo.class));
                    } else if (cardType.equals("officialAccountCode")) {
                        forwardMsg.setOfficialAccountsCodeData(JsonUtil.convert(forwardMsg.getData(), IMOfflineMsgItemVo.OfficialAccountsCodeData.class));
                    } else if (cardType.equals("shareConfCard")) {
                        forwardMsg.setShareConfCardData(JsonUtil.convert(forwardMsg.getData(), IMOfflineMsgItemVo.ShareConfCardData.class));
                    } else if (cardType.equals("customCard")) {
                        forwardMsg.setCustomCardData(JsonUtil.convert(forwardMsg.getData(), IMOfflineMsgItemVo.CustomCardVo.class));
                    }
                }
                if (forwardMsg.getMsgType().equals(8)) {
                    String userTxt = forwardMsg.getUserTxt();
                    IMOfflineMsgItemVo.MergeForwardMsgVo mergeForwardMsgVo1 = JsonUtil.parseJson(userTxt, IMOfflineMsgItemVo.MergeForwardMsgVo.class);
                    processMergeForwardMsg(mergeForwardMsgVo1.getForwardMsgs(), groupId, dbpathGlobal);
                    forwardMsg.setUserTxt(JsonUtil.toJsonStr(mergeForwardMsgVo1));
                }
            }
        }
    }


    //    private String downloadSaveImage(String fileId, String fileName, Long groupId,String dbpathGlobal) {
//        // 定义本地文件路径
//        fileName = idWorker.nextId() + "_" + fileName;
//        // 构建文件路径（使用Path的resolve方法，自动处理不同系统的路径分隔符）
//        Path filePath = Path.of(dbpathGlobal, groupId + "", fileName); // 完整文件路径：FILE_PATH/groupId/文件名
//        // 关键：获取文件所在目录，若不存在则创建（包括所有父目录）
//        var r = imHttpClient.downloadIconOrDefault(fileId, filePath, DEFAULT_FILE_PATH);
//        if (r == DEFAULT_FILE_PATH) {
//            return DEFAULT_ICON_URL;
//        } else {
//            return FILE_PATH_URL + filePath.toString();
//        }
//    }
    private String downloadSaveImage(String fileId, String fileName, Long groupId, String dbpathGlobal) {
        // 定义本地文件路径
        fileName = idWorker.nextId() + "_" + fileName;
        // 构建文件路径（使用Path的resolve方法，自动处理不同系统的路径分隔符）
        Path filePath = Path.of(dbpathGlobal, groupId + "", fileName); // 完整文件路径：FILE_PATH/groupId/文件名
        // 关键：获取文件所在目录，若不存在则创建（包括所有父目录）
        // 不返回默认图片，文件不存在就显示裂开
        var r = imHttpClient.downloadIconOrDefault(fileId, filePath, null);
        if (Objects.isNull(r)) {
            return "";
        } else {
            return FILE_PATH_URL + filePath.toString();
        }
    }

    private String downloadSaveImageByIdEncrypt(String id,String fileId, String fileName, Long groupId, String dbpathGlobal) {
        // 未启用加密，使用原逻辑
        if (!encryptImUtil.encryptEnabled()) {
            return downloadSaveImageById(id, fileId, fileName, groupId, dbpathGlobal);
        }
        // 启用加密走新逻辑
        return encryptImUtil.downloadSaveImageById(id, fileId, fileName, groupId, dbpathGlobal);
    }

    private String downloadSaveImageById(String id,String fileId, String fileName, Long groupId, String dbpathGlobal) {
        // 定义本地文件路径 以唯一id作为文件名，防止每次归档数据越来越大
        fileName = id + "_" + fileName;
        // 构建文件路径（使用Path的resolve方法，自动处理不同系统的路径分隔符）
        Path filePath = Path.of(dbpathGlobal, groupId + "", fileName); // 完整文件路径：FILE_PATH/groupId/文件名
        // 关键：获取文件所在目录，若不存在则创建（包括所有父目录）
        // 不返回默认图片，文件不存在就显示裂开
        // 如果文件存在甚至不网络请求，直接返回文件路径
        if(new File(filePath.toString()).exists()) {
            return FILE_PATH_URL + filePath;
        }
        var r = imHttpClient.downloadIconOrDefault(fileId, filePath, null);
        if (Objects.isNull(r)) {
            return "";
        } else {
            return FILE_PATH_URL + filePath;
        }
    }


    @Override
    public List<ArchiveTimelineDTO> getCaredArchivedPageList(String keywords) {
        UserInfo user = SecurityUtils.getUser();
        if (Objects.isNull(user)) {
            throw new SecurityUtils.UnAuthException("access token invalid");
        }
        log.debug("getCaredArchivedPageList SecurityUtils user:{}", user);
        Long userId = user.getUserId();
        List<String> orgIds = user.getImOrgPrivCodes();
        log.debug("getPageList SecurityUtils orgIds:{}", orgIds);       // 当前用户关联了哪些协同岗
        List<String> postIdList = findPostIdList(userId);

        // 先查询所有月份（不带搜索条件）
        List<ArchiveTimelineDTO> allTimelineList = groupExtendsMapper.selectCaredArchivedPageList(userId, null, orgIds, postIdList);
        // 如果有搜索条件，查询匹配的月份并更新count
        if (StringUtils.hasText(keywords)) {
            List<ArchiveTimelineDTO> matchedTimelineList = groupExtendsMapper.selectCaredArchivedPageList(userId, keywords, orgIds, postIdList);

            // 创建一个Map存储匹配结果
            Map<String, Long> matchedCountMap = matchedTimelineList.stream()
                    .collect(Collectors.toMap(ArchiveTimelineDTO::getYearMonth, ArchiveTimelineDTO::getCount));

            // 更新所有月份的count，没有匹配的月份设置为0
            for (ArchiveTimelineDTO dto : allTimelineList) {
                dto.setCount(matchedCountMap.getOrDefault(dto.getYearMonth(), 0L));
            }
        }

        // 空值处理
        return CollectionUtils.isEmpty(allTimelineList) ? Collections.emptyList() : allTimelineList;
    }


    public static List<String> collectAllOrgCodes(JsonArray orgJsonArray) {
        List<String> codeList = new ArrayList<>();
        // 边界处理：空数组直接返回
        if (orgJsonArray == null || orgJsonArray.isEmpty()) {
            return codeList;
        }

        // 1. 遍历当前JsonArray中的每个组织对象
        for (int i = 0; i < orgJsonArray.size(); i++) {
            // 获取当前索引的组织JsonObject（依赖自定义JsonArray的getJSONObject方法）
            JsonObject orgObj = orgJsonArray.getJSONObject(i);
            if (orgObj == null) {
                continue; // 跳过空对象，避免空指针
            }

            // 2. 提取当前组织对象的code（依赖自定义JsonObject的getString方法）
            String currentCode = orgObj.getString("code");
            if (currentCode != null && !currentCode.trim().isEmpty()) {
                codeList.add(currentCode);
            }

            // 3. 递归处理当前组织的children数组（若存在）
            JsonArray childrenArray = null;
            try {
                // 尝试获取children字段（可能不存在，需捕获异常）
                childrenArray = orgObj.getJSONArray("children");
            } catch (JsonException e) {
                // 若children不是JsonArray（或不存在），跳过递归
                continue;
            }
            // 递归收集子组织的code，并合并到主列表
            List<String> childCodes = collectAllOrgCodes(childrenArray);
            codeList.addAll(childCodes);
        }

        return codeList;
    }

    @Override
    public IPage<IMOfflineMsgItemVo> getArchiveMsgPage(Long groupId, String keywords, Integer pageNum, Integer pageSize, Date startTime, Date endTime, String from) {
        if (groupId == null) {
            throw new RuntimeException("Group ID is required");
        }
        if (pageNum == null || pageNum <= 0) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize <= 0) {
            pageSize = 10;
        }
        try {
            GroupExtends groupExtends = groupExtendsMapper.selectByGroupId(groupId);
            if (groupExtends == null) {
                return null;
            }
            //dataSourceManager.switchToSqliteGroupDb(groupId);
            //IPage<IMOfflineMsgItemVo> page = new Page<>(pageNum, pageSize);
            //IPage<IMOfflineMsgItemVo> resultPage = dynamicChatGroupMessageMapper.selectArchiveMsgByPage(page, groupId, keywords);
            String archivedFilePath = groupExtends.getArchivedFile();
            if (!StringUtils.hasText(archivedFilePath)) {
                // 已归档用已归档路径，未归档的使用全局配置的路径，因为自动归档不会修改群组的archivedFile字段
                String dbpathGlobal = globalsRpcService.getGlobalsValueByName("DBPATH") + "/";
                archivedFilePath = encryptImUtil.getGroupPath(dbpathGlobal,String.valueOf(groupId));
            }
//            String dbPath = archivedFilePath + "/group_" + groupId + ".db";
            String groupPath = archivedFilePath;
            String dbFileName = "group_" + groupId + ".db";
            String tableName = "tb_chat_group_" + groupId;

            String condition = null;
            List<String> paramList = new ArrayList<>();
            if (StringUtils.hasText(keywords)) {
                // SQL注入防护：转义单引号
                String escapedKeywords = keywords.replace("'", "''");
                // LIKE通配符转义：转义 % 和 _，避免被当作通配符匹配
                escapedKeywords = escapedKeywords.replace("%", "\\%").replace("_", "\\_");
                StringBuffer sb = new StringBuffer();
                sb.append("(");
                // 提取text=后面的文本内容进行匹配，避免误匹配endFlag/msgSeq/stopLength等元数据字段
                // text_content = SUBSTR(msg, INSTR(msg, 'text=') + 5) 即从'text='之后开始截取
                String textContentExpr = "SUBSTR(msg, INSTR(msg, 'text=') + 5)";
                // 直接匹配text内容
                sb.append(String.format("%s LIKE '%%%s%%' ESCAPE '\\'", textContentExpr, escapedKeywords));
                sb.append(" OR ");
                // 处理后匹配（移除text内容中[...]部分后）
                sb.append(String.format("TRIM(CASE WHEN %s LIKE '%%]%%' THEN SUBSTR(%s, 1, INSTR(%s, '[') - 1) || SUBSTR(%s, INSTR(%s, ']') + 1) ELSE %s END) LIKE '%%%s%%' ESCAPE '\\'",
                    textContentExpr, textContentExpr, textContentExpr, textContentExpr, textContentExpr, textContentExpr, escapedKeywords));
                sb.append(" OR ");
                // 冒号格式匹配（仅在text内容中）
                sb.append(String.format("%s LIKE '%%:%%%s%%' ESCAPE '\\'", textContentExpr, escapedKeywords));
                if ("@所有人".equals(keywords)) {
                    sb.append(" OR ");
                    sb.append(String.format("%s LIKE '%%:%%%s%%'", textContentExpr, "@all"));
                }
                sb.append(")");
                paramList.add(sb.toString());
            }
            // 发送者ID（tb_chat_member_xxxx中id）
            if (StringUtils.hasText(from)) {
                paramList.add(String.format("\"from\" = '%s'", from));
            }
            if (Objects.nonNull(startTime)) {
                long time = startTime.getTime();
                paramList.add(String.format("time >= %d", time));
            }
            if (Objects.nonNull(endTime)) {
                long time = endTime.getTime();
                paramList.add(String.format("time <= %d", time));
            }
            if (CollectionUtils.isNotEmpty(paramList)) {
                condition = String.join(" AND ", paramList);
            }

//            Map<String, Object> map = sqliteDynamicUtil.queryByPageWithCondition(dbPath, tableName, condition, "time DESC", pageNum, pageSize);
            Map<String, Object> map = sqliteDynamicUtil.queryByPageWithConditionEncrypt(groupPath, dbFileName,
                    tableName, condition, "time DESC", pageNum, pageSize, String.valueOf(groupId));
            IPage<IMOfflineMsgItemVo> resultPage = new Page<>();
            if (map.get("totalPages") != null) {
                resultPage.setPages(Long.valueOf(map.get("totalPages").toString()));
            }
            if (map.get("total") != null) {
                resultPage.setTotal(Long.valueOf(map.get("total").toString()));
            }
            if (map.get("pageNum") != null) {
                resultPage.setCurrent(Long.valueOf(map.get("pageNum").toString()));
            }
            if (map.get("pageSize") != null) {
                resultPage.setSize(Long.valueOf(map.get("pageSize").toString()));
            }
            List<Map<String, Object>> dataList = (List<Map<String, Object>>) map.get("data");

            List<IMOfflineMsgItemVo> items = new ArrayList<>();
            if (dataList != null && dataList.size() > 0) {
                for (Map<String, Object> data : dataList) {
                    log.debug("tb_chat_group_ datakey column" + data.keySet());
                    IMOfflineMsgItemVo item = new IMOfflineMsgItemVo(data);
                    items.add(item);
                }
            }
            resultPage.setRecords(items);
            return resultPage;
        } catch (Exception e) {
            // 捕获SQLite表不存在异常（动态表未创建，说明无归档数据）
            /*if (e.getMessage().contains("no such table")) {
                throw new RuntimeException("该群组无归档消息");
            }*/
            // 其他异常（如SQL错误）
            log.error("查询归档消息失败：groupId={}, keywords={}", groupId, keywords, e);
            throw new RuntimeException("查询归档消息失败，请重试");
        } finally {
            //dataSourceManager.clearGroupDbContext();
        }
    }

    @Override
    public IPage<TbChatMember> getArchiveMemberPage(Long groupId, Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum <= 0) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize <= 0) {
            pageSize = 10;
        }
        try {
            GroupExtends groupExtends = groupExtendsMapper.selectByGroupId(groupId);
            if (groupExtends == null) {
                return null;
            }
            //dataSourceManager.switchToSqliteGroupDb(groupId);

            String archivedFilePath = groupExtends.getArchivedFile();
            if (!StringUtils.hasText(archivedFilePath)) {
                // 已归档用已归档路径，未归档的使用全局配置的路径，因为自动归档不会修改群组的archivedFile字段
                String dbpathGlobal = globalsRpcService.getGlobalsValueByName("DBPATH") + "/";
                archivedFilePath = encryptImUtil.getGroupPath(dbpathGlobal, String.valueOf(groupId));
            }

//            String dbPath = archivedFilePath + "/group_" + groupId + ".db";
            String groupPath = archivedFilePath;
            String dbFileName = "group_" + groupId + ".db";
            String tableName = "tb_chat_member_" + groupId;
//            Map<String, Object> map = sqliteDynamicUtil.queryByPageWithCondition(dbPath, tableName, null, "join_type ASC, invite_time DESC", pageNum, pageSize);
            Map<String, Object> map = sqliteDynamicUtil.queryByPageWithConditionEncrypt(groupPath, dbFileName,
                    tableName, null, "join_type ASC, invite_time DESC", pageNum, pageSize, String.valueOf(groupId));
            //IPage<TbChatMember> resultPage = dynamicChatMemberMapper.selectArchiveMemberByPage(page, groupId);
            IPage<TbChatMember> resultPage = new Page<>();
            log.info("设置分页参数");
            if (map.get("totalPages") != null) {
                resultPage.setPages(Long.valueOf(map.get("totalPages").toString()));
            }
            if (map.get("total") != null) {
                resultPage.setTotal(Long.valueOf(map.get("total").toString()));
            }
            if (map.get("pageNum") != null) {
                resultPage.setCurrent(Long.valueOf(map.get("pageNum").toString()));
            }
            if (map.get("pageSize") != null) {
                resultPage.setSize(Long.valueOf(map.get("pageSize").toString()));
            }
            log.info("设置分页结束");
            List<TbChatMember> resultList = new ArrayList<>();
            List<Map<String, Object>> dataList = (List<Map<String, Object>>) map.get("data");
            if (dataList != null && dataList.size() > 0) {

                for (Map<String, Object> data : dataList) {
                    TbChatMember item = new TbChatMember();
                    log.info("tb_chat_member_ datakey column" + data.keySet());
                    if (data.get("id") != null) {
                        item.setId(Long.parseLong(data.get("id").toString()));
                    }
                    if (data.get("code") != null) {
                        item.setCode(data.get("code").toString());
                    }
                    if (data.get("name") != null) {
                        item.setName(data.get("name").toString());
                    }
                    if (data.get("alias") != null) {
                        item.setAlias(data.get("alias").toString());
                    }
                    if (data.get("tumb_avatar") != null) {
                        item.setTumbAvatar(data.get("tumb_avatar").toString());
                    }
                    if (data.get("gender") != null) {
                        item.setGender(Integer.parseInt(data.get("gender").toString()));
                    }
                    if (data.get("gender_name") != null) {
                        item.setGenderName(data.get("gender_name").toString());
                    }
                    if (data.get("mobile") != null) {
                        item.setMobile(data.get("mobile").toString());
                    }
                    if (data.get("email") != null) {
                        item.setEmail(data.get("email").toString());
                    }
                    if (data.get("isdn") != null) {
                        item.setIsdn(data.get("isdn").toString());
                    }
                    if (data.get("direct_leader_id") != null) {
                        item.setDirectLeaderId(Long.parseLong(data.get("direct_leader_id").toString()));
                    }
                    if (data.get("status") != null) {
                        item.setStatus(Integer.parseInt(data.get("status").toString()));
                    }
                    if (data.get("status_name") != null) {
                        item.setStatusName(data.get("status_name").toString());
                    }
                    if (data.get("user_alias") != null) {
                        item.setUserAlias(data.get("user_alias").toString());
                    }
                    if (data.get("idcard") != null) {
                        item.setIdcard(data.get("idcard").toString());
                    }
                    if (data.get("role") != null) {
                        item.setRole(Integer.parseInt(data.get("role").toString()));
                    }
                    if (data.get("mute_type") != null) {
                        item.setMuteType(Integer.parseInt(data.get("mute_type").toString()));
                    }
                    if (data.get("join_type") != null) {
                        item.setJoinType(Integer.parseInt(data.get("join_type").toString()));
                    }
                    if (data.get("invite_id") != null) {
                        item.setInviteId(Long.parseLong(data.get("invite_id").toString()));
                    }
                    if (data.get("invite_time") != null) {
                        item.setInviteTime(Long.parseLong(data.get("invite_time").toString()));
                    }
                    resultList.add(item);
                }
            }
            resultPage.setRecords(resultList);
            return resultPage;
        } catch (Exception e) {
            /*if (e.getMessage().contains("no such table")) {
                throw new RuntimeException("该群组无归档成员");
            }*/
            log.error("查询归档成员失败：groupId={}", groupId, e);
            throw new RuntimeException("查询归档成员失败，请重试");
        } finally {
            // dataSourceManager.clearGroupDbContext();
        }
    }

    @Override
    public IMOfflineMsgItemVo getGroupMessageById(Long groupId, Long messageId) {
        GroupExtends groupExtends = groupExtendsMapper.selectByGroupId(groupId);
        if (groupExtends == null) {
            return null;
        }
//        String dbPath = groupExtends.getArchivedFile() + "/group_" + groupId + ".db";
        String groupPath = groupExtends.getArchivedFile();
        String dbFileName = "group_" + groupId + ".db";
        String tableName = "tb_chat_group_" + groupId;
        String condition = "msg_id = " + messageId;
//        Map<String, Object> groupMessageById = sqliteDynamicUtil.getGroupMessageById(dbPath, tableName, condition);
        Map<String, Object> groupMessageById = sqliteDynamicUtil.getGroupMessageByIdEncrypt(groupPath, dbFileName,
                tableName, groupId.toString(), condition);
        if (groupMessageById != null) {
            return new IMOfflineMsgItemVo(groupMessageById);
        }
        return null;
    }

    /**
     * 查找已归档的数据列表
     */
    @Override
    public IPage<GroupExtendsVO> getArchiveList(String startTime, String endTime, String keywords, Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum <= 0) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize <= 0) {
            pageSize = 10;
        }
        Page<GroupExtendsVO> page = new Page<>(pageNum, pageSize);

        return groupExtendsMapper.getArchiveList(page, startTime, endTime, keywords);
    }

    @Override
    @LogReport(type = OperationTypeEnum.GROUP_ARCHIVE_EXPORT)
    public void downloadArchivedGroupFiles(@LogReportParam(field = "groupName") List<GroupExtendsVO> groupExtendsList, HttpServletResponse response) {
        log.info("群组列表：{}", groupExtendsList);
        String totalZipPath = null;
        String tempBaseDir = null;
        try {
            String dbpath = globalsRpcService.getGlobalsValueByName("DBPATH");

            tempBaseDir = dbpath + "/temp/" + System.currentTimeMillis() + "/";
            String tempSingleZipDir = tempBaseDir + "zip/";
            File tempDir = new File(tempSingleZipDir);
            if (!tempDir.exists()) {
                tempDir.mkdirs();
                log.info("创建临时目录成功");
            }

            // 为每个源文件夹生成单独的ZIP包（临时文件）
            for (GroupExtendsVO groupExtends : groupExtendsList) {
                File sourceFolder = new File(groupExtends.getArchivedFile());
                if (!sourceFolder.exists() || !sourceFolder.isDirectory()) {
                    throw new FileNotFoundException("源文件夹不存在或不是目录：" + groupExtends.getArchivedFile());
                }

                // 单个ZIP命名：群组名 + ".zip"
                // 单个ZIP命名：群组名 + groupId + ".zip"
//                String singleZipName = groupExtends.getGroupName() + ".zip";
                // 文件命名修改：群组名 + groupId + ".zip"（用来防止名字重复导致文件夹不能生成的问题）
                String singleZipName = String.format("%s(%d).zip", groupExtends.getGroupName(), groupExtends.getGroupId());
                String singleZipPath = tempSingleZipDir + singleZipName;
                // 调用之前的单个文件夹压缩方法
                // 加解密的相关
                boolean isEncrypt = groupExtends.getArchivedFile().endsWith(Constant.ENCRYPT_DIR);
                if (!isEncrypt) {
                    ZipUtils.compressFolder(groupExtends.getArchivedFile(), singleZipPath);
                } else {
                    encryptImUtil.compressGroupFile(groupExtends.getArchivedFile(), singleZipPath,
                            String.valueOf(groupExtends.getGroupId()));
                }
                log.info("压缩完成：{}", singleZipPath);
            }

            // 将所有单个ZIP包压缩为总ZIP
            totalZipPath = tempBaseDir + "archives.zip";
            try (ZipOutputStream totalZos = new ZipOutputStream(new FileOutputStream(totalZipPath))) {

                // 遍历临时目录中的单个ZIP包，添加到总ZIP
                File[] singleZips = tempDir.listFiles((dir, name) -> name.endsWith(".zip"));
                if (singleZips != null && singleZips.length > 0) {
                    log.info("开始压缩总ZIP");
                    for (File singleZip : singleZips) {
                        // 总ZIP中的条目名称：单个ZIP的文件名（保持原名称）
                        ZipEntry totalEntry = new ZipEntry(singleZip.getName());
                        totalZos.putNextEntry(totalEntry);

                        // 读取单个ZIP内容，写入总ZIP
                        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(singleZip))) {
                            byte[] buffer = new byte[1024 * 8];
                            int len;
                            while ((len = bis.read(buffer)) != -1) {
                                totalZos.write(buffer, 0, len);
                            }
                        }
                        totalZos.closeEntry();
                    }
                    log.info("压缩完成：{}", totalZipPath);
                } else {
                    throw new IOException("未生成任何单个ZIP包，无法创建总压缩包");
                }
            }

            // 设置响应头并下载文件
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment; filename=archives.zip");
            response.setHeader("Content-Length", String.valueOf(new File(totalZipPath).length()));

            // 将总ZIP文件写入响应输出流
            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(totalZipPath));
                 BufferedOutputStream bos = new BufferedOutputStream(response.getOutputStream())) {
                byte[] buffer = new byte[1024 * 8];
                int len;
                while ((len = bis.read(buffer)) != -1) {
                    bos.write(buffer, 0, len);
                }
                bos.flush();
            }

        } catch (Exception e) {
            log.error("下载归档文件失败，群组信息：{}, error: {}", groupExtendsList, e.getMessage());
            throw new RuntimeException("文件打包下载失败: " + e.getMessage());
        } finally {
            // 下载完成后，删除临时文件
            if (tempBaseDir != null) {
                deleteTempDirectory(new File(tempBaseDir));
            }
        }
    }

    @Override
    public List<GroupExtendsVO> getGroupExtendsList(List<Long> groupIds) {
        return groupExtendsMapper.getArchiveListByIds(groupIds);
    }

    @Override
    @LogReport(type = OperationTypeEnum.GROUP_ARCHIVE_DELETE)
    public void deleteArchivedGroupFiles(@LogReportParam(field = "groupName") List<GroupExtendsVO> groupExtendsList) {
        //删除归档文件
        for (GroupExtendsVO groupExtends : groupExtendsList) {
            log.info("删除了{}的归档文件及{}的数据", groupExtends.getGroupName(), groupExtends.getGroupId());
            Path path = Paths.get(groupExtends.getArchivedFile());
            if (Files.exists(path)) {
                try (Stream<Path> stream = Files.walk(path)) {
                    stream.sorted(Comparator.reverseOrder())
                            .forEach(p -> {
                                try {
                                    Files.delete(p);
                                } catch (IOException e) {
                                    throw new UncheckedIOException(e);
                                }
                            });
                } catch (Exception e) {
                    log.error("删除归档文件失败: {}", e.getMessage());
                }
            }
            //删除群组数据
            groupExtendsMapper.delete(new QueryWrapper<GroupExtends>().eq("group_id", groupExtends.getGroupId()));
            groupTagMapper.updateByGroupId(groupExtends.getGroupId());
            createGroupMapper.deleteByGroupId(groupExtends.getGroupId());
            userGroupCareMapper.delete(new QueryWrapper<UserGroupCare>().eq("group_id", groupExtends.getGroupId()));


        }
    }

    @Override
    public List<GroupExtends> findListByGroupIds(List<Long> groupIdList) {
        LambdaQueryWrapper<GroupExtends> queryWrapper = new LambdaQueryWrapper<GroupExtends>()
                .in(GroupExtends::getGroupId, groupIdList);
        return list(queryWrapper);
    }

    @Override
    public IPage<UserCareGroupDTO> listGroupsByUser(OpenApiGroupQO qo) {
        Long userId = qo.getUserId();
        if (userId == null) {
            throw new RuntimeException("userId is required");
        }
        int pageNum = qo.getPageNum() == null || qo.getPageNum() <= 0 ? 1 : qo.getPageNum();
        int pageSize = qo.getPageSize() == null || qo.getPageSize() <= 0 ? 10 : qo.getPageSize();

        List<String> postIdList = findPostIdList(userId);
        List<String> orgIds = findOrgIdsByUserId(userId);
        log.info("listGroupsByUser userId:{}, scope:{}, groupType:{}, createType:{}, postIdList:{}, orgIds:{}", userId, qo.getScope(), qo.getGroupType(), qo.getCreateType(), postIdList, orgIds);
        Page<UserCareGroupDTO> page = new Page<>(pageNum, pageSize);
        return groupExtendsMapper.listGroupsByUser(page, userId, qo.getScope(), qo.getGroupType(), qo.getCreateType(), postIdList, orgIds);
    }

    private List<String> findOrgIdsByUserId(Long userId) {
        try {
            var roleDto = roleRpcService.getRoleByUserId(userId + "");
            if (roleDto == null || CollectionUtils.isEmpty(roleDto.getOrgPrivList())) {
                return Collections.emptyList();
            }
            return roleDto.getOrgPrivList().stream()
                    .map(OrgPrivDto::getCode)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("findOrgIdsByUserId failed for userId={}, error={}", userId, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public OpenApiGroupCountVO getGroupCountByUserId(Long userId) {
        List<String> postIdList = findPostIdList(userId);
        return groupExtendsMapper.getGroupCountByUserId(userId, postIdList);
    }

    @Override
    public UpdateGroupMemberResultVO updateGroupMember(Long groupId, UpdateGroupMemberCO co, Long operatorUserId) {
        String userIdsStr = co.getUserIds();
        String idCardsStr = co.getIdCards();

        if (co.getOpType() == 1) {
            // 添加成员
            Map<String, Object> reqBody = new HashMap<>();
            List<Map<String, Object>> members = new ArrayList<>();
            if (StringUtils.hasText(userIdsStr)) {
                for (String userId : userIdsStr.split(";")) {
                    if (StringUtils.hasText(userId.trim())) {
                        Map<String, Object> member = new HashMap<>();
                        member.put("userIdentity", userId.trim());
                        member.put("idType", 0);
                        member.put("joinType", co.getJoinType());
                        members.add(member);
                    }
                }
            }
            if (StringUtils.hasText(idCardsStr)) {
                for (String idCard : idCardsStr.split(";")) {
                    if (StringUtils.hasText(idCard.trim())) {
                        Map<String, Object> member = new HashMap<>();
                        member.put("userIdentity", idCard.trim());
                        member.put("idType", 1);
                        member.put("joinType", co.getJoinType());
                        members.add(member);
                    }
                }
            }
            reqBody.put("members", members);
            reqBody.put("userIdentity", operatorUserId);
            reqBody.put("idType", 0);
            reqBody.put("addWording", co.getComment());
            var imResp = imHttpClient.addGroupMembers(groupId, reqBody, operatorUserId);
            touchGroupUpdateTime(groupId);
            List<String> originalIdentities = members.stream()
                    .map(m -> (String) m.get("userIdentity"))
                    .collect(Collectors.toList());
            return convertToResultVO(imResp, operatorUserId, originalIdentities);
        } else {
            // 删除成员
            Map<String, Object> reqBody = new HashMap<>();
            List<Map<String, Object>> delMembers = new ArrayList<>();
            if (StringUtils.hasText(userIdsStr)) {
                for (String userId : userIdsStr.split(";")) {
                    if (StringUtils.hasText(userId.trim())) {
                        Map<String, Object> member = new HashMap<>();
                        member.put("userIdentity", userId.trim());
                        member.put("idType", 0);
                        delMembers.add(member);
                    }
                }
            }
            if (StringUtils.hasText(idCardsStr)) {
                for (String idCard : idCardsStr.split(";")) {
                    if (StringUtils.hasText(idCard.trim())) {
                        Map<String, Object> member = new HashMap<>();
                        member.put("userIdentity", idCard.trim());
                        member.put("idType", 1);
                        delMembers.add(member);
                    }
                }
            }
            reqBody.put("delMembers", delMembers);
            log.info("deleteGroupMembers groupId:{}, reqBody:{}, operatorUserId:{}", groupId, reqBody, operatorUserId);
            var imResp = imHttpClient.deleteGroupMembers(groupId, reqBody, operatorUserId);
            // 成员删除后推进 tb_create_group.update_time，服务于 tb_static_create_group 增量游标
            touchGroupUpdateTime(groupId);
            List<String> originalIdentities = delMembers.stream()
                    .map(m -> (String) m.get("userIdentity"))
                    .collect(Collectors.toList());
            return convertToResultVO(imResp, operatorUserId, originalIdentities);
        }
    }

    /**
     * 推进 tb_create_group.update_time，服务于 tb_static_create_group 增量游标。
     * <p>
     * 成员增删通过 IM HTTP 接口完成，tb_create_group 本身不存成员变更明细，
     * 但需要推进 update_time 让统计表能增量拉取到群组有变更。
     * 失败不阻断主流程（成员增删已通过 IM 完成）。
     *
     * @param groupId 群组ID
     */
    private void touchGroupUpdateTime(Long groupId) {
        try {
            CreateGroup update = new CreateGroup();
            update.setUpdateTime(new Date());
            createGroupMapper.update(update,
                new LambdaUpdateWrapper<CreateGroup>().eq(CreateGroup::getGroupId, groupId));
        } catch (Exception e) {
            log.warn("touchGroupUpdateTime error, groupId:{}", groupId, e);
        }
    }

    private UpdateGroupMemberResultVO convertToResultVO(ImResponse<JsonObject> imResp, Long operatorUserId, List<String> originalIdentities) {
        UpdateGroupMemberResultVO vo = new UpdateGroupMemberResultVO();
        vo.setOperatorUserIdentity(operatorUserId != null ? String.valueOf(operatorUserId) : null);
        if (imResp == null) {
            log.warn("convertToResultVO imResp is null");
            return vo;
        }

        if (imResp.getCode() != null && imResp.getCode() != 0) {
            vo.setCode(imResp.getCode());
            vo.setMsg(imResp.getMsg());
            return vo;
        }
        var data = imResp.getData();
        if (data == null) {
            log.warn("convertToResultVO data is null, imResp code:{}, msg:{}", imResp.getCode(), imResp.getMsg());
            return vo;
        }
        if (data.get("groupId") != null) {
            vo.setGroupId(data.getLong("groupId"));
        }

        var memberResults = data.containsKey("memberResults") && data.get("memberResults") != null
                ? data.getJSONArray("memberResults") : null;
        if (memberResults != null) {
            List<UpdateGroupMemberResultVO.MemberResult> results = new ArrayList<>();
            Map<Integer, List<UpdateGroupMemberResultVO.MemberResult>> failedByCode = new LinkedHashMap<>();
            for (int i = 0; i < memberResults.size(); i++) {
                var item = memberResults.getJSONObject(i);
                UpdateGroupMemberResultVO.MemberResult mr = new UpdateGroupMemberResultVO.MemberResult();
                mr.setCode(item.getInteger("code"));
                mr.setMsg(item.getString("msg"));
                if (originalIdentities != null && i < originalIdentities.size()) {
                    mr.setUserIdentity(originalIdentities.get(i));
                } else {
                    mr.setUserIdentity(item.getString("userIdentity"));
                }
                results.add(mr);
                if (mr.getCode() != null && mr.getCode() != 0) {
                    failedByCode.computeIfAbsent(mr.getCode(), k -> new java.util.ArrayList<>()).add(mr);
                }
            }
            vo.setMemberResults(results);
            if (!failedByCode.isEmpty()) {
                var firstEntry = failedByCode.entrySet().iterator().next();
                Integer imErrorCode = firstEntry.getKey();
                vo.setCode(imErrorCode);
                vo.setMsg(formatMemberErrorMsg(imErrorCode, firstEntry.getValue()));
            } else {
                vo.setMemberResults(results);
            }
        }
        return vo;
    }

    private static final Map<Integer, String> MEMBER_ERR_MSG_TPL = Map.of(
            1080012, "被邀请成员{}已在群组中",
            1080011, "群组不能添加/删除三方用户:{}",
            204, "群组不能添加/删除三方用户:{}"
    );

    private String formatMemberErrorMsg(Integer code, List<UpdateGroupMemberResultVO.MemberResult> failedMembers) {
        String tpl = MEMBER_ERR_MSG_TPL.get(code);
        if (tpl == null) {
            return failedMembers.get(0).getMsg();
        }
        String ids = failedMembers.stream()
                .map(UpdateGroupMemberResultVO.MemberResult::getUserIdentity)
                .collect(Collectors.joining("、"));
        return tpl.replace("{}", ids);
    }

    /**
     * 递归删除临时目录
     */
    private void deleteTempDirectory(File directory) {
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteTempDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }

}