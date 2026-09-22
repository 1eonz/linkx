package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.tdtech.cloudcmd.bean.CagentMqFrame;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.CreateGroupCO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.LabelVO;
import com.tdtech.cloudcmd.im.jingxin.client.CachedImConfig;
import com.tdtech.cloudcmd.im.jingxin.client.ImHttpClient;
import com.tdtech.cloudcmd.im.jingxin.client.entity.*;
import com.tdtech.cloudcmd.im.jingxin.server.aiclient.React1Over1p4BClient;
import com.tdtech.cloudcmd.im.jingxin.server.aiclient.group.GroupAiClient;
import com.tdtech.cloudcmd.im.jingxin.server.entity.*;
import com.tdtech.cloudcmd.im.jingxin.server.service.GroupExtendsService;
import com.tdtech.cloudcmd.im.jingxin.server.service.GroupGlassesService;
import com.tdtech.cloudcmd.im.jingxin.server.service.IUserCoopRecievedService;
import com.tdtech.cloudcmd.im.jingxin.server.service.LabelService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.*;
import com.tdtech.cloudcmd.im.jingxin.server.util.FileUtil;
import com.tdtech.cloudcmd.msip.aop.LogReport;
import com.tdtech.cloudcmd.msip.aop.LogReportParam;
import com.tdtech.cloudcmd.msip.enums.LicenseEnum;
import com.tdtech.cloudcmd.msip.enums.OperationTypeEnum;
import com.tdtech.cloudcmd.msip.util.LicenseUtil;
import com.tdtech.cloudcmd.redis.RedisUtil;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.IdWorker;
import com.tdtech.linkx.node.api.PeerNodeRpcApi;
import com.tdtech.linkx.node.util.PeerNodeIconUrlUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author lsc
 * @date 2025/7/15
 **/
@Slf4j
@Service
public class LabelServiceImpl extends ServiceImpl<LabelMapper, Label> implements LabelService {

    private static final String LOCATION_REGEX =
            "^(-?((180(\\.\\d{1,18})?)|((1[0-7]\\d)|([1-9]?\\d))(\\.\\d{1,18})?))," + "(-?((90(\\.\\d{1,18})?)|([1-8]?\\d)(\\.\\d{1,18})?))$";
    private static final Pattern LOCATION_PATTERN = Pattern.compile(LOCATION_REGEX);
    @Resource
    private LabelMapper labelMapper;
    @Resource
    private LabelBindingMapper labelBindingMapper;
    @Resource
    private LabelBindingUserMapper labelBindingUserMapper;
    @Resource
    private CollaborationPostMapper collaborationPostMapper;
    @Resource
    @Qualifier("coopImHttpClient")
    private ImHttpClient imHttpClient;
    @Resource
    @Qualifier("oneO1p4BImHttpClient")
    private ImHttpClient oneO1p4BImHttpClient;
    @Resource
    private React1Over1p4BClient react1Over1p4BClient;
    @Resource
    private ImService imService;
    @Resource
    private CollaborationAttendanceSwitchMapper attendanceSwitchMapper;
    @Resource
    private IdWorker idWorker;
    @Resource
    private CreateGroupMapper createGroupMapper;
    @Resource
    private DepartmentLocationMapper departmentLocationMapper;

    @Resource
    private GroupExtendsMapper groupExtendsMapper;
    @Resource
    private GroupAiClient groupAiClient;
    @Resource
    private StreamBridge streamBridge;

    @Resource
    private CachedImConfig cachedImConfig;
    @Resource
    private FileUtil fileUtil;
    @Resource
    private LicenseUtil licenseUtil;

    @Resource
    private GroupGlassesService groupGlassesService;

    @Resource(name = "creatGroupExecutorService")
    private TaskExecutor taskExecutor;

    @Resource
    private GroupExtendsService groupExtendsService;

    @Resource
    private CollaborationAttendanceMapper collaborationAttendanceMapper;

    @Resource
    private IUserCoopRecievedService coopRecievedService;

    @DubboReference
    private PeerNodeRpcApi peerNodeRpcApi;

    private static final String GROUP_CREATE = "GROUP_CREATE";
    private static final String MSG_TOPIC = "cloudcmd-cagent";
    @Autowired
    private RedisUtil redisUtil;

    private static final int USER_MAX_NUM = 50;

    @Override
    public void upsertBindingsBatch(Label label, List<LabelBinding> bindings) {
        var i = labelMapper.updateById(label);
        if (i == 0) {
            //新增额外判断上级协同岗绑定
            if (label.getParentId() != null && label.getId() != 0L) {
                var cnt = labelBindingMapper.selectCount(
                        Wrappers.lambdaQuery(LabelBinding.class).eq(LabelBinding::getLabelId, label.getParentId()));
                if (cnt != 0) {
                    throw new BusinessException("上级标签存在绑定协同岗，不让新增子标签");
                }
                var bindUserCnt = labelBindingUserMapper.selectCount(
                        Wrappers.lambdaQuery(LabelBindingUser.class).eq(LabelBindingUser::getLabelId, label.getParentId()));
                if (bindUserCnt != 0) {
                    throw new BusinessException("上级标签存在绑定人员，不让新增子标签");
                }
            }
            labelMapper.insert(label);
        }

        if (CollectionUtils.isNotEmpty(bindings)) {
            labelBindingMapper.delete(Wrappers.lambdaQuery(LabelBinding.class).eq(LabelBinding::getLabelId, label.getId()));
            labelBindingMapper.insertBatch(bindings);
        } else if (Integer.valueOf(1).equals(label.getIsCancel())) {
            labelBindingMapper.delete(Wrappers.lambdaQuery(LabelBinding.class).eq(LabelBinding::getLabelId, label.getId()));
        }
    }

    @Override
    public void delete(Long id) {
        labelMapper.deleteLabelById(id);
        labelBindingMapper.delete(Wrappers.lambdaQuery(LabelBinding.class).eq(LabelBinding::getLabelId, id));
    }

    @Override
    @LogReport(type = OperationTypeEnum.COLLABORATION_LABEL_DELETE)
    public void delete(@LogReportParam Label label) {
        delete(label.getId());
    }

    @Override
    public Label getById(Long id) {
        return labelMapper.selectLabelById(id);
    }

    @Override
    public List<LabelBinding> bindingsByLabel(Long id) {
        return labelBindingMapper.selectList(Wrappers.lambdaQuery(LabelBinding.class).eq(LabelBinding::getLabelId, id));
    }

    @Override
    public List<LabelBindingUser> bindingsUserByLabel(Long id) {
        return labelBindingUserMapper.selectList(Wrappers.lambdaQuery(LabelBindingUser.class).eq(LabelBindingUser::getLabelId, id));
    }

    @Override
    public void removePostIdFromBindings(Long postId) {
        labelBindingMapper.removePostIdFromBindings(postId);
    }

    @Override
    public List<LabelBinding> findBindingByPostId(Long postId, Integer type, Long departmentId) {
        return labelBindingMapper.findByPostId(postId, type, departmentId);
    }

    @Override
    public List<Label> getLabelsByLevel(Integer level) {
        return lambdaQuery().eq(Label::getLevel, level).eq(Label::getIsDeleted, 0).list();
    }

    @Override
    public List<Label> getChildrenLabels(Long parentId, String name) {
        LambdaQueryChainWrapper<Label> labelLambdaQueryChainWrapper = lambdaQuery();
        labelLambdaQueryChainWrapper.eq(Label::getIsDeleted, 0);
        if (StringUtils.isNotBlank(name)) {
            labelLambdaQueryChainWrapper = labelLambdaQueryChainWrapper.eq(Label::getName, name);
        }
        return labelLambdaQueryChainWrapper.eq(Label::getParentId, parentId).list();
    }

    @Override
    public List<LabelVO> getLabelVOs(String name, Integer scope, int level) {
        // 判断license ai功能是否可用
        boolean aiIsAvailable = licenseUtil.availableByCode(LicenseEnum.AI_COLLABORATION.getCode());
        List<LabelVO> labelVOs;
        if (aiIsAvailable) {
            // ai可能可用，查询所有标签
            labelVOs = labelMapper.getLabelVOs(name, null, scope, level);
        } else {
            // ai功能不可用，过滤人员核查标签
            labelVOs = labelMapper.getLabelVOs(name, 0, scope, level);
        }
        Set<Long> collaborationIdSet = labelVOs.stream()
                .filter(labelVO -> labelVO.getCollaborationIds() != null && !labelVO.getCollaborationIds().isEmpty())
                .flatMap(labelVO -> Arrays.stream(labelVO.getCollaborationIds().split(","))).map(Long::parseLong)
                .collect(Collectors.toSet());
        if (collaborationIdSet.size() > 0) {
            Map<Long, CollaborationPost> collaborationPostMap =
                    collaborationPostMapper.selectBatchIds(new ArrayList<>(collaborationIdSet)).stream()
                            .collect(Collectors.toMap(CollaborationPost::getId, Function.identity()));
            for (LabelVO labelVO : labelVOs) {
                String collaborationIds = labelVO.getCollaborationIds();
                if (collaborationIds != null && !collaborationIds.isEmpty()) {
                    String collaborationNames =
                            Arrays.stream(collaborationIds.split(",")).map(Long::parseLong).map(collaborationPostMap::get)
                                    .filter(Objects::nonNull).map(CollaborationPost::getPostName)
                                    .collect(Collectors.joining(","));

                    labelVO.setCollaborationNames(collaborationNames);
                }
            }

        }
        List<LabelVO> removes = new ArrayList<>();
        var orgMap = Optional.ofNullable(labelVOs).stream().flatMap(Collection::stream)
                .collect(Collectors.toMap(LabelVO::getId, Function.identity()));
        Optional.ofNullable(labelVOs).stream().flatMap(Collection::stream).forEach(o -> {
            if (o.getParentId() != -1L && orgMap.containsKey(o.getParentId())) {
                var po = orgMap.get(o.getParentId());
                if (po.getChildren() == null) {
                    po.setChildren(new LinkedList<>());
                }
                po.getChildren().add(o);
                removes.add(o);
            }
        });
        labelVOs.removeAll(removes);
        return labelVOs;
    }

    /**
     * 批量查询多个标签关联的在线协同岗（优化：合并DB查询，避免N+1问题）
     * <p>
     * 处理流程：
     * 1. 一次性查询所有标签在当前组织路径下的绑定关系
     * 2. 按组织层级从深到浅匹配每个标签最接近的协同岗绑定
     * 3. 批量查询所有协同岗信息
     * 4. 批量查询在岗人员，一次性筛选出有在线人员的协同岗
     *
     * @param labels  标签列表
     * @param depPath 组织路径（逗号分隔的组织ID）
     * @param depName 组织名称（用于异常提示）
     * @return 有在线人员的协同岗列表
     */
    private List<CollaborationPost> findCollaborationPostsForLabels(List<Label> labels, String depPath, String depName) {
        // 解析组织路径为ID列表
        var depIds = Arrays.stream(depPath.split(",")).filter(a -> !a.isBlank()).map(Long::parseLong)
                .collect(Collectors.toList());

        // 一次性查询所有标签在当前组织路径下的绑定关系（替代逐标签查询）
        List<Long> labelIds = labels.stream().map(Label::getId).collect(Collectors.toList());
        List<LabelBinding> allLabelBindings = labelBindingMapper.selectList(
                Wrappers.lambdaQuery(LabelBinding.class)
                        .in(LabelBinding::getLabelId, labelIds)
                        .in(LabelBinding::getDepartmentId, depIds));
        if (allLabelBindings == null || allLabelBindings.isEmpty()) {
            throw new BusinessException("标签：" + labels.stream().map(Label::getName).collect(Collectors.joining(",")) + "，组织：" + depName + "，没有关联协同岗，不允许建群！");
        }

        // 找最接近当前组织的协同岗绑定：从组织路径最深层级向上匹配，优先匹配最深层
        Map<Long, LabelBinding> labelIdToBinding = new LinkedHashMap<>();
        for (var i = depIds.size() - 1; i >= 0; i--) {
            var depId = depIds.get(i);
            for (Label label : labels) {
                if (labelIdToBinding.containsKey(label.getId())) {
                    continue;
                }
                var lbo = allLabelBindings.stream()
                        .filter(lb -> Objects.equals(lb.getLabelId(), label.getId()) && Objects.equals(depId, lb.getDepartmentId()))
                        .findAny();
                lbo.ifPresent(labelBinding -> labelIdToBinding.put(label.getId(), labelBinding));
            }
        }

        // 校验每个标签都找到了对应的协同岗绑定
        for (Label label : labels) {
            LabelBinding binding = labelIdToBinding.get(label.getId());
            if (binding == null || binding.getPostIds() == null || binding.getPostIds().isEmpty()) {
                throw new BusinessException("标签：" + label.getName() + "，组织：" + depName + "，没有关联协同岗，不允许建群！");
            }
        }

        // 汇总所有标签关联的协同岗ID，一次性查询协同岗详情（替代逐标签查询）
        Set<Long> allPostIds = labelIdToBinding.values().stream()
                .map(LabelBinding::getPostIds)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        if (allPostIds.isEmpty()) {
            throw new BusinessException("标签：" + labels.stream().map(Label::getName).collect(Collectors.joining(",")) + "，组织：" + depName + "，没有关联协同岗，不允许建群！");
        }

        List<CollaborationPost> allPosts = collaborationPostMapper.selectBatchIds(new ArrayList<>(allPostIds));
        if (allPosts == null || allPosts.isEmpty()) {
            throw new BusinessException("标签：" + labels.stream().map(Label::getName).collect(Collectors.joining(",")) + "，组织：" + depName + "，没有关联协同岗，不允许建群！");
        }

        long l = System.currentTimeMillis();

        // 提取所有协同岗关联的人员ID
        Set<Long> allUserIds = allPosts.stream()
                .map(CollaborationPost::getRelatedUserIds)
                .map(a -> a.split(","))
                .flatMap(Arrays::stream)
                .filter(a -> !a.isBlank())
                .distinct()
                .map(Long::parseLong)
                .collect(Collectors.toSet());

        // 批量筛选在线协同岗：先查开关表获取在岗人员，再用批量SQL筛选有上岗记录的协同岗（替代逐岗查询）
        List<Long> onDuties;
        Set<Long> onDutyPostIds = Collections.emptySet();
        if (!allUserIds.isEmpty()) {
            onDuties = attendanceSwitchMapper.listOnDutyPeople(allUserIds);
            if (CollectionUtils.isNotEmpty(onDuties)) {
                Set<Long> onDutyUserSet = new HashSet<>(onDuties);
                // 先在内存中快速过滤出关联了在岗人员的协同岗，缩小SQL查询范围
                Set<Long> candidatePostIds = allPosts.stream()
                        .filter(post -> CollectionUtils.isNotEmpty(post.getUids()) && !Collections.disjoint(post.getUids(), onDutyUserSet))
                        .map(CollaborationPost::getId)
                        .collect(Collectors.toSet());
                if (!candidatePostIds.isEmpty()) {
                    // 一次性批量查询哪些协同岗有最新上岗记录（替代逐岗调用selectPersonIdsWithLatestOnDuty）
                    onDutyPostIds = new HashSet<>(collaborationAttendanceMapper.selectPostIdsWithOnDutyPeople(candidatePostIds, onDutyUserSet));
                }
            }
        }

        // 根据批量查询结果过滤出有在线人员的协同岗
        Set<Long> finalOnDutyPostIds = onDutyPostIds;
        List<CollaborationPost> result = allPosts.stream()
                .filter(post -> finalOnDutyPostIds.contains(post.getId()))
                .collect(Collectors.toList());

        log.info("批量处理协同岗在线耗时：{}", System.currentTimeMillis() - l);

        if (result.isEmpty()) {
            String labelNames = labels.stream().map(Label::getName).collect(Collectors.joining(","));
            throw new BusinessException("标签：" + labelNames + "，关联协同岗没有成员在线，不允许建群！");
        }

        Set<Long> onlinePostExist = result.stream().map(CollaborationPost::getId).collect(Collectors.toSet());
        String labelNames = labels.stream().filter(label -> {
            LabelBinding binding = labelIdToBinding.get(label.getId());
            if (binding == null || CollectionUtils.isEmpty(binding.getPostIds())) {
                return true;
            }
            return binding.getPostIds().stream().noneMatch(onlinePostExist::contains);
        }).map(Label::getName).collect(Collectors.joining(","));
        if (StringUtils.isNotBlank(labelNames)) {
            throw new BusinessException("标签：" + labelNames + "，关联协同岗没有成员在线，不允许建群！");
        }

        return result;
    }

    @Override
    public Long createGroup(CreateGroupCO createGroupVO) {
        log.info("createGroupVO:{}", createGroupVO);
        long l1 = System.currentTimeMillis();

        // 获取协同用户
        CompletableFuture<UserGetVo> userFuture = CompletableFuture.supplyAsync(
                () -> imHttpClient.userPage(createGroupVO.getIdCard(), null), taskExecutor);

        List<Label> labels = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(createGroupVO.getIds())) {
            // 根据标签id查找对应的协同岗id
            labels = labelMapper.selectBatchByIds(createGroupVO.getIds());
        }
        UserGetVo userGetVo;
        try {
            userGetVo = userFuture.get();
        } catch (Exception e) {
            throw new BusinessException("建群前置查询失败：" + e.getMessage());
        }

        var collaborationPostIds = new LinkedList<Long>();
        var collaborationPostNames = new LinkedList<String>();

        // 根据上传的身份证号，查找当前用户的领导id
        List<ImUser> successUserList = userGetVo.getResults();
        if (CollectionUtils.isEmpty(successUserList)) {
            throw new BusinessException("创建人不存在");
        }
        // 获取到领导id
        Long directLeaderId = successUserList.get(0).getDirectLeaderId();
        // 将领导id拉入群中
        if (directLeaderId != null && directLeaderId != 0L) {
            collaborationPostIds.add(directLeaderId);
        }
        // 获取到领导姓名
        String directLeaderName = successUserList.get(0).getDirectLeaderName();
        // 将领导存入
        if (StringUtils.isNotBlank(directLeaderName)) {
            collaborationPostNames.add(directLeaderName);
        }
        // 1Over1.4B
        AtomicBoolean oneO1p4BFlag = new AtomicBoolean(false);

        Integer source = createGroupVO.getSource();
        if (source != null && source == 1) {
            // 判断license ai功能是否可用
            boolean aiIsAvailable = licenseUtil.availableByCode(LicenseEnum.AI_COLLABORATION.getCode());
            log.info("label createGroup aiIsAvailable: {}", aiIsAvailable);
            // ai功能可用才拉人员核查或者群AI助手入群
            if (aiIsAvailable) {
                labels.stream().filter(l -> Objects.equals(l.getType(), 1)).findAny().ifPresent(label -> {
                    Optional.ofNullable(oneO1p4BImHttpClient.getToken())//
                            .map(ImToken::getProxyUser)//
                            .map(ImToken.ProxyUser::getId)//
                            .ifPresentOrElse(a -> {
                                collaborationPostIds.add(a);
                                collaborationPostNames.add("人员核查");
                                oneO1p4BFlag.set(true);
                            }, () -> {
                                log.error("1o1p4BImHttpClient not login yet..cant add 1:1.4B user");
                                throw new BusinessException("配置错误，请联系管理员");
                            });
                });
                labels.stream().filter(l -> Objects.equals(l.getType(), 0)).findAny().ifPresent(label -> {
                    List<Long> groupAIUserIds = groupAiClient.getDefaultProxyUserIds();
                    for (Long groupAIUserId : groupAIUserIds) {
                        collaborationPostIds.add(groupAIUserId);
                        collaborationPostNames.add("群AI助手");
                    }
                });
            }
        }

        List<CollaborationPost> posts = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(labels)) {
            posts.addAll(findCollaborationPostsForLabels(labels, createGroupVO.getDepartmentFullPath(), createGroupVO.getDepartmentName()));
        }
        // 合并额外选择的协同岗（本端+跨节点），保持标签关联协同岗优先，按ID去重
        if (CollectionUtils.isNotEmpty(createGroupVO.getCoopUserIds())) {
            Set<Long> existingIds = posts.stream().map(CollaborationPost::getId).collect(Collectors.toSet());
            // 本端协同岗：直接从协同岗表查询
            List<CollaborationPost> localExtraPosts = collaborationPostMapper.selectByIds(createGroupVO.getCoopUserIds());
            if (CollectionUtils.isNotEmpty(localExtraPosts)) {
                localExtraPosts.stream()
                        .filter(p -> !existingIds.contains(p.getId()))
                        .forEach(p -> {
                            posts.add(p);
                            existingIds.add(p.getId());
                        });
            }
            // 跨节点协同岗：从接收表查询对端分享给本节点的协同岗
            List<UserCoopRecieved> receivedExtraPosts = coopRecievedService.list(
                    new LambdaQueryWrapper<UserCoopRecieved>()
                            .in(UserCoopRecieved::getCoopUserId, createGroupVO.getCoopUserIds())
                            .eq(UserCoopRecieved::getStatus, 1));
            if (CollectionUtils.isNotEmpty(receivedExtraPosts)) {
                // 批量查询接收协同岗来源节点 IP，用于拼接对端图标完整 URL（iconUrl 为对端相对路径，前端无法直接访问）
                Set<String> originPeerIds = receivedExtraPosts.stream()
                        .map(UserCoopRecieved::getOriginPeerId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
                Map<String, String> peerBaseUrlMap = Collections.emptyMap();
                if (!originPeerIds.isEmpty()) {
                    try {
                        Map<String, PeerNodeRpcApi.PeerNodeInfo> infoMap =
                                peerNodeRpcApi.getPeerInfoMap(new ArrayList<>(originPeerIds));
                        if (infoMap != null) {
                            peerBaseUrlMap = infoMap.entrySet().stream()
                                    .collect(Collectors.toMap(Map.Entry::getKey,
                                            e -> PeerNodeIconUrlUtil.buildBaseUrl(e.getValue().getIp()),
                                            (a, b) -> a));
                        }
                    } catch (Exception e) {
                        log.warn("createGroup: getPeerInfoMap failed, originPeerIds={}", originPeerIds, e);
                    }
                }
                final Map<String, String> finalPeerBaseUrlMap = peerBaseUrlMap;
                receivedExtraPosts.stream()
                        .filter(rec -> !existingIds.contains(rec.getCoopUserId()))
                        .map(rec -> {
                            CollaborationPost post = new CollaborationPost();
                            post.setId(rec.getCoopUserId());
                            post.setPostName(rec.getCoopUserName());
                            // iconUrl 为对端相对路径，需拼对端节点 baseUrl 后前端才能访问
                            String baseUrl = rec.getOriginPeerId() != null
                                    ? finalPeerBaseUrlMap.get(rec.getOriginPeerId()) : null;
                            post.setIconUrl(PeerNodeIconUrlUtil.prepend(baseUrl, rec.getIconUrl()));
                            post.setOrgId(rec.getOrgId());
                            post.setOrgName(rec.getOrgName());
                            return post;
                        })
                        .forEach(p -> {
                            posts.add(p);
                            existingIds.add(p.getId());
                        });
            }
            log.info("createGroup merge extra coopUsers, coopUserIds={}, afterMergePosts={}",
                    createGroupVO.getCoopUserIds(), posts.stream().map(CollaborationPost::getId).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(createGroupVO.getIds())) {
            List<Long> labelIds = createGroupVO.getIds().stream().map(Long::parseLong).collect(Collectors.toList());
            createGroupVO.setLabelIds(labelIds);
        }
        // 合并协同岗id（去重，避免与直接领导/AI用户/标签关联协同岗重复入群）
        if (CollectionUtils.isNotEmpty(createGroupVO.getCPostIds())) {
            Set<Long> existingIds = new HashSet<>(collaborationPostIds);
            posts.stream().map(CollaborationPost::getId).filter(Objects::nonNull).forEach(existingIds::add);
            createGroupVO.getCPostIds().stream()
                    .filter(Objects::nonNull)
                    .filter(existingIds::add)
                    .forEach(collaborationPostIds::add);
        }
        Long groupId = executeCreatGroup(createGroupVO, posts, collaborationPostIds, collaborationPostNames, oneO1p4BFlag, createGroupVO.getSource() == null ? 1 : createGroupVO.getSource());
        log.info("一键建群耗时：{}", System.currentTimeMillis() - l1);
        return groupId;
    }

    public Long executeCreatGroup(CreateGroupCO createGroupVO, List<CollaborationPost> posts, LinkedList<Long> collaborationPostIds, LinkedList<String> collaborationPostNames, AtomicBoolean oneO1p4BFlag, int source) {
        if (CollectionUtils.isNotEmpty(createGroupVO.getIds()) && posts.isEmpty()) {
            if ("1".equals(createGroupVO.getBindGlasses())){
                log.error("语音建群未找到关联的协同岗");
            }
            throw new BusinessException("未找到关联的协同岗");
        }
        collaborationPostIds.addAll(posts.stream().map(CollaborationPost::getId).collect(Collectors.toList()));
        collaborationPostNames.addAll(
                posts.stream().map(CollaborationPost::getPostName).collect(Collectors.toList()));

        // if (( 创建人 + 协同岗的数量 ) < 3 ) return 群成员小于3不允许建群
        int userIdsSize = CollectionUtils.isEmpty(createGroupVO.getUserIds()) ? 0 : createGroupVO.getUserIds().size();
        if ((1 + collaborationPostIds.size() + userIdsSize) < 3) {
            throw new BusinessException("群成员不足3个，不允许建群");
        }

        log.info("collaborationPostIds: {}, collaborationPostNames: {}", collaborationPostIds, collaborationPostNames);
        // 一键建群使用的是协同岗的id不是人员id，
        List<Long> postMembers = new ArrayList<>();
        List<AddMember> addMembers = collaborationPostIds.stream().map(collaborationPostId -> {
            postMembers.add(collaborationPostId);
            AddMember addMember = new AddMember();
            addMember.setUserIdentity(collaborationPostId + "");
            addMember.setIdType(0);
            return addMember;
        }).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(createGroupVO.getUserIds())) {
            for (Long userId : createGroupVO.getUserIds()) {
                AddMember addMember = new AddMember();
                addMember.setUserIdentity(userId + "");
                addMember.setIdType(0);
                addMembers.add(addMember);
            }
        }

        String location = createGroupVO.getLocation();
        log.info("createGroup start location:{}", location);
        if (StringUtils.isBlank(location)) {
            // 没有就获取部门location
            LambdaQueryWrapper<DepartmentLocation> lambdaWrapper = new LambdaQueryWrapper<>();
            lambdaWrapper.eq(DepartmentLocation::getDepartmentCode, createGroupVO.getDepartmentCode());
            DepartmentLocation departmentLocation = departmentLocationMapper.selectOne(lambdaWrapper);
            if (departmentLocation != null) {
                location = departmentLocation.getLocation();
                log.info("createGroup departmentCode: {} DepartmentLocation location: {}",
                        createGroupVO.getDepartmentCode(), location);
            }
        } else {
            // 验证格式
            Matcher matcher = LOCATION_PATTERN.matcher(location);
            if (!matcher.matches()) {
                throw new BusinessException(
                        "经纬度格式错误，正确格式：经度,纬度，经度(-180~180)，纬度(-90~90)，小数点后最多18位");
            }
        }
        log.info("createGroup end location:{}", location);
        // 构建参数
        CreateGroup createGroup = new CreateGroup();
        createGroup.setId(idWorker.nextId());
        createGroup.setOwnerId(createGroupVO.getOwnerId());
        createGroup.setDepartmentName(createGroupVO.getDepartmentName());
        createGroup.setOwnerName(createGroupVO.getOwnerName());
        createGroup.setDepartmentId(createGroupVO.getDepartmentCode());
        createGroup.setLocation(location);

        Set<Long> members = new HashSet<>();
        members.add(Long.parseLong(createGroupVO.getOwnerId()));
        if (CollectionUtils.isNotEmpty(createGroupVO.getUserIds())) {
            members.addAll(createGroupVO.getUserIds());
        }
        if (CollectionUtils.isNotEmpty(postMembers)) {
            List<String> postMapMembers = collaborationPostMapper.selectMemberByPostIds(postMembers);
            postMapMembers.forEach(m -> {
                String[] ids = m.split(",");
                for (String id : ids) {
                    members.add(Long.parseLong(id.trim()));
                }
            });
        }

        CreateGroupReq createGroupReq = new CreateGroupReq();
        GroupCreateReq groupCreateReq = new GroupCreateReq();
        groupCreateReq.setAddMembers(addMembers);
        if(StringUtils.isNotBlank(createGroupVO.getGroupName())){
            groupCreateReq.setName(createGroupVO.getGroupName());
        }

        // 根据协同岗id查找到人员id

        Owner owner = new Owner();
        owner.setUserIdentity(createGroupVO.getOwnerId());
        owner.setIdType(0);
        groupCreateReq.setOwner(owner);

        createGroupReq.setCreateGroupReq(groupCreateReq);
        Long group = imHttpClient.createGroup(createGroupReq);
        createGroup.setGroupId(group);
        // 异步执行群组信息落库
        taskExecutor.execute(()->{
            GroupVo groupVo = imHttpClient.queryGroupDetail(group);
            createGroup.setGroupName(
                    StringUtils.isNotBlank(groupVo.getName()) ? groupVo.getName() : groupVo.getUndefinedName());
            // 成员信息不能直接存入协同岗的ids，会丢失创建人，改为从im获取，直接存入im已经处理好的成员
            List<GroupMembers> groupMembers = groupVo.getGroupMembers();
            List<Long> userIdList =
                    groupMembers.stream().map(GroupMembers::getUserId).collect(Collectors.toList());
            List<String> nameList =
                    groupMembers.stream().map(GroupMembers::getName).collect(Collectors.toList());

            createGroup.setUserIds(CollectionUtils.join(userIdList, ","));
            createGroup.setUserNames(CollectionUtils.join(nameList, ","));
            createGroup.setAvatar(groupVo.getAvatar());
            createGroup.setAvatarImg(fileUtil.downloadSaveIcon(imHttpClient, groupVo.getAvatar()));
            createGroup.setCreateTime(new Date());
            createGroup.setUpdateTime(new Date());
            createGroup.setSource(source);
            createGroupMapper.upsert(createGroup,
                    Wrappers.lambdaUpdate(CreateGroup.class).eq(CreateGroup::getGroupId, group).set(CreateGroup::getSource, source));
            if (source == 1 && oneO1p4BFlag.get()) {
                var department = oneO1p4BImHttpClient.getToken().getDepartment();
                react1Over1p4BClient.reportGroupAsync(
                        department == null || department.getDepartmentCode() == null ? "0" : department.getDepartmentCode(), 3,
                        group, groupCreateReq.getName(), () -> send1o1p4bMsg("链接1：14e失败", group));
            }
            GroupExtends existing = groupExtendsMapper.selectByGroupId(group);
            if (existing == null) {
                GroupExtends groupExtends = new GroupExtends();
                groupExtends.setGmtCreated(LocalDateTime.now());
                groupExtends.setGroupId(group);
                groupExtends.setArchived(0);
                groupExtends.setGroupType(2);
                groupExtendsMapper.insertIgnoreDuplicated(groupExtends);
            }

            // 一键建群完成后，自动绑定标签
            bindGroupLabel(createGroupVO, group);
        });

        Map<String, Object> map = new HashMap<>();
        map.put("groupId", group);
        map.put("members", members);
        //建群完成后，推送所有在线前端消息，刷新页面
        List<String> membersList = members.stream().map(String::valueOf).collect(Collectors.toList());
        var cagentMqFrame = new CagentMqFrame().toBuilder().typeSubSystemMessage(GROUP_CREATE)
                .unicast().userIds(membersList).build()
                .body(GROUP_CREATE, GROUP_CREATE, map).build();
        streamBridge.send(MSG_TOPIC, cagentMqFrame);
        return group;
    }

    private void bindGroupLabel(CreateGroupCO createGroupVO, Long group) {
        try {
            List<Long> labelIds = createGroupVO.getLabelIds();
            if (CollectionUtils.isNotEmpty(labelIds)) {
                groupExtendsService.batchUpdateGroupTags(group, labelIds, Long.valueOf(createGroupVO.getOwnerId()), false);
            }
        } catch (Exception e) {
            log.error("自动绑定群组标签失败", e);
        }
    }

    private void send1o1p4bMsg(String text, Long groupId) {
        var request = new ImMessageRequest<TxtMsgVo>();
        request.setCategory(2);
        request.setMsgType(1);
        request.setPlaintext(1);
        request.setFrom(oneO1p4BImHttpClient.getToken().getProxyUser().getId());
        request.setTo(groupId + "");
        TxtMsgVo txtVo = new TxtMsgVo();
        txtVo.setText(text);
        request.setMsg(txtVo);
        oneO1p4BImHttpClient.sendMsg(request);
    }

    @Override
    public List<Label> getLabelsByCollaborationPostId(Long collaborationPostId) {
        return labelMapper.getLabelsByCollaborationPostId(collaborationPostId);
    }

    @Override
    public void updateGroupLocation(GroupLocationVO groupLocationVO) {
        String location = groupLocationVO.getLocation();
        Long userId = groupLocationVO.getUserId();
        // 位置为空时仅记录错误日志（不抛异常，符合void无返回的设计）
        if (StringUtils.isBlank(location)) {
            log.warn("userId [{}] 位置更新失败：位置信息为空（可能未开启定位服务）", userId);
            return; // 直接结束方法
        }
        log.debug("userId [{}] location service, uploaded location: {}", userId, location);

        // 构建更新条件
        LambdaUpdateWrapper<CreateGroup> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CreateGroup::getOwnerId, userId).set(CreateGroup::getLocation, location)
                .set(CreateGroup::getUpdateTime, LocalDateTime.now());

        // 执行更新并记录影响行数（便于排查问题）
        int affectedRows = createGroupMapper.update(null, updateWrapper);
        log.debug("userId [{}] Location update processing completed, number of groups matched and updated: {}", userId,
                affectedRows);
    }

    @Override
    @LogReport(type = OperationTypeEnum.COLLABORATION_LABEL_INSERT)
    public void saveLabel(@LogReportParam LabelCO label) {
        //check binding count
        var bindings = label.createBinding(idWorker);
        String bindingMaxNum = cachedImConfig.getConfig("COLLABORATIONPOST_MAX_NUM");
        for (var binding : bindings) {
            if (binding.getPostIds().size() > Integer.parseInt(bindingMaxNum)) {
                throw new BusinessException("关联协同岗不能超过" + bindingMaxNum + "个");
            }
        }
        log.debug("save label:{} {}", label, bindings);
        //save
        upsertBindingsBatch(label, bindings);
    }

    @Override
    @LogReport(type = OperationTypeEnum.COLLABORATION_LABEL_UPDATE)
    public void updateLabel(@LogReportParam LabelCO label) {
        Map<Long, LabelCO.Binding> orgCollaborations = label.getOrgCollaborations();
        if (CollectionUtils.isNotEmpty(orgCollaborations)) {
            var cnt = labelMapper.selectCount(
                    Wrappers.lambdaQuery(Label.class).eq(Label::getParentId, label.getId()).eq(Label::getIsDeleted, 0));
            if (cnt != 0) {
                throw new BusinessException("存在子标签，无法绑定协同岗");
            }
        }
        saveLabel(label);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> deleteBatch(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return R.success(true);
        }
        List<Label> labels = labelMapper.selectAllByIds(ids);
        return deleteBatchLabels(labels);
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    @LogReport(type = OperationTypeEnum.COLLABORATION_LABEL_DELETE)
    public R<Boolean> deleteBatchLabels(@LogReportParam List<Label> labelList) {
        if (CollectionUtils.isEmpty(labelList)) {
            return R.success(true);
        }
        List<Long> ids = labelList.stream().map(Label::getId).collect(Collectors.toList());
        List<Label> childrenLabelList = labelMapper.getAllChildrenLabels(ids);
        if (!CollectionUtils.isEmpty(childrenLabelList)) {
            List<Long> childrenLabelIds = childrenLabelList.stream().map(Label::getId).collect(Collectors.toList());
            boolean hasNotSelectNext = childrenLabelIds.stream().anyMatch(id -> !ids.contains(id));
            if (hasNotSelectNext) {
                throw new BusinessException("存在下级标签，请先删除下级标签或选择所有下级标签！");
            }
            if (!childrenLabelIds.isEmpty()) {
                List<Label> grandchildrenLabelList = labelMapper.getAllChildrenLabels(childrenLabelIds);
                hasNotSelectNext = grandchildrenLabelList.stream().map(Label::getId).anyMatch(id -> !ids.contains(id));
                if (hasNotSelectNext) {
                    throw new BusinessException("存在下级标签，请先删除下级标签或选择所有下级标签！");
                }
            }
        }

        var labelBindings = labelBindingMapper.selectList(Wrappers.lambdaQuery(LabelBinding.class).in(LabelBinding::getLabelId, ids));
        if (labelBindings != null && !labelBindings.isEmpty()) {
            throw new BusinessException("标签已绑定协同岗，请先删除绑定！");
        }
        var labelBindingUsers = labelBindingUserMapper.selectList(Wrappers.lambdaQuery(LabelBindingUser.class).in(LabelBindingUser::getLabelId, ids));
        if (CollectionUtils.isNotEmpty(labelBindingUsers)) {
            throw new BusinessException("标签已绑定人员，请先删除绑定！");
        }

        labelMapper.deleteBatchByIds(ids);
        labelBindingMapper.delete(Wrappers.lambdaQuery(LabelBinding.class).in(LabelBinding::getLabelId, ids));
        labelBindingUserMapper.delete(Wrappers.lambdaQuery(LabelBindingUser.class).in(LabelBindingUser::getLabelId, ids));
        return R.success(true);
    }

    @Override
    public List<Label> selectAllByIds(List<Long> ids) {
        return labelMapper.selectAllByIds(ids);
    }

    @Override
    @LogReport(type = OperationTypeEnum.COLLABORATION_LABEL_UPDATE)
    public void bindingUser(LabelCO label) {
        // 先删后增
        labelBindingUserMapper.delete(Wrappers.lambdaQuery(LabelBindingUser.class).eq(LabelBindingUser::getLabelId, label.getId()));
        List<LabelBindingUser> bindingUser = label.createBindingUser(idWorker);
        if (CollectionUtils.isNotEmpty(bindingUser)) {
            for (var binding : bindingUser) {
                if (binding.getUserIds().size() > USER_MAX_NUM) {
                    throw new BusinessException("关联人员不能超过" + USER_MAX_NUM + "个");
                }
            }
            labelBindingUserMapper.insertBatch(bindingUser);
        }
    }

    @Override
    public List<UserIDNameInfo> getBindingUser(Long id, Long departmentId) {
        // 1. 查询标签绑定的人员
        List<LabelBindingUser> labelBindingUsers = labelBindingUserMapper.selectList(
                Wrappers.lambdaQuery(LabelBindingUser.class)
                        .eq(LabelBindingUser::getLabelId, id)
                        .eq(departmentId != null, LabelBindingUser::getDepartmentId, departmentId)
        );

        if (CollectionUtils.isEmpty(labelBindingUsers)) {
            return List.of();
        }

        // 2. 提取所有用户ID（去重）
        List<Long> allUserIds = labelBindingUsers.stream()
                .filter(binding -> CollectionUtils.isNotEmpty(binding.getUserIds()))
                .flatMap(binding -> binding.getUserIds().stream())
                .distinct()
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(allUserIds)) {
            return List.of();
        }

        // 3. 分批查询用户信息（警信接口一次最多支持50人）
        var results = queryUserDetailsByUserIds(allUserIds);
        if(CollectionUtils.isNotEmpty(results)){
            // 设置默认头像，等之后头像做了同步，在更新为新的头像
            results.forEach(e-> e.setAvatar(fileUtil.getDefaultPath()));
        }
        return results;
    }

    @Override
    public List<UserIDNameInfo> queryUserDetailsByUserIds(List<Long> allUserIds){
        return  Lists.partition(allUserIds, USER_MAX_NUM).stream()
                .map(this::queryUserDetails)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    @Override
    public IPage<Label> getLabelVOs(Integer pageNum, Integer pageSize, String name) {
        if(pageNum == null){
            pageNum = 1;
        }
        if(pageSize == null){
            pageSize = 10;
        }
        Page<Label> page = new Page<>(pageNum, pageSize);
        return labelMapper.getLabelPage(page,name);
    }

    @Override
    public List<Label> getLableByName(String name) {
        LambdaQueryChainWrapper<Label> labelLambdaQueryChainWrapper = lambdaQuery();
        labelLambdaQueryChainWrapper.eq(Label::getIsDeleted, 0).eq(Label::getName, name);
        return labelLambdaQueryChainWrapper.list();
    }

    @Override
    public List<Label> getAllLabel(Integer scope) {
        List<Label> labels = labelMapper.selectAllList(scope);
        if (CollectionUtils.isEmpty(labels)) {
            return labels;
        }

        // scope=2时，判断是否为最底层标签
        if (Integer.valueOf(2).equals(scope)) {
            Set<Long> parentIds = labels.stream()
                    .map(Label::getParentId)
                    .filter(Objects::nonNull)
                    .filter(parentId -> parentId != -1L)
                    .collect(Collectors.toSet());
            labels.forEach(label -> label.setIsAssociatedCoop(parentIds.contains(label.getId()) ? 0 : 1));
        }

        return labels;
    }

    /**
     * 批量查询用户详情
     */
    private List<UserIDNameInfo> queryUserDetails(List<Long> userIds) {
        String userIdsStr = userIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        List<UserIDNameInfo> userDetails = imHttpClient.queryUserDetail(userIdsStr);
        return CollectionUtils.isEmpty(userDetails) ? List.of() : userDetails;
    }
}