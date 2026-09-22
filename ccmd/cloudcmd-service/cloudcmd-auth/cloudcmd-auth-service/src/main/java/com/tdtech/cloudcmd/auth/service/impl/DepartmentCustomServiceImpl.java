package com.tdtech.cloudcmd.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.tdtech.cloudcmd.auth.dto.DepartmentCustomCO;
import com.tdtech.cloudcmd.auth.dto.DepartmentCustomDutyScheduleVO;
import com.tdtech.cloudcmd.auth.dto.DepartmentCustomUserVO;
import com.tdtech.cloudcmd.auth.dto.DepartmentCustomVO;
import com.tdtech.cloudcmd.auth.dto.DepartmentNodeCustomCO;
import com.tdtech.cloudcmd.auth.dto.DepartmentNodeCustomVO;
import com.tdtech.cloudcmd.auth.dto.ImUserQO;
import com.tdtech.cloudcmd.auth.entity.*;
import com.tdtech.cloudcmd.auth.mapper.DepartmentCustomMapper;
import com.tdtech.cloudcmd.auth.mapper.DepartmentNodeCustomMapper;
import com.tdtech.cloudcmd.auth.mapper.DepartmentUserCustomMapper;
import com.tdtech.cloudcmd.auth.mapper.ImUserMapper;
import com.tdtech.cloudcmd.auth.mapper.UserAdminMapper;
import com.tdtech.cloudcmd.auth.service.IDepartmentCustomService;
import com.tdtech.cloudcmd.auth.service.IRoleService;
import com.tdtech.cloudcmd.auth.service.ImUserEsService;
import com.tdtech.cloudcmd.auth.service.ImUserService;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.encryptor.service.EncryptionService;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.im.jingxin.api.DutyScheduleRpcApi;
import com.tdtech.cloudcmd.im.jingxin.api.UserAvatarRpcApi;
import com.tdtech.cloudcmd.im.jingxin.api.entity.duty.DutyScheduleUserVO;
import com.tdtech.cloudcmd.im.jingxin.client.ImHttpClient;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImPage;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImUser;
import com.tdtech.cloudcmd.im.jingxin.client.entity.UserGetVo;
import com.tdtech.cloudcmd.util.*;
import com.tdtech.cloudcmd.util.json.JsonUtil;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class DepartmentCustomServiceImpl extends ServiceImpl<DepartmentCustomMapper, DepartmentCustom>
        implements IDepartmentCustomService {

    private static final long DEFAULT_USER_ID = 1L;
    private static final long MAX_PAGE_SIZE = 200L;
    private static final Long VIRTUAL_ROOT_ID = 0L;
    private static final long DEFAULT_TENANT_ID = 0L;
    private static final String NO_PERMISSION_PRIV_STRING = "-1";
    private static final String DEFAULT_USER_AVATAR_PATH = "/collaboration/static/default-head.png";
    public static final String DEFAULT_PASSWORD = "Aa@123456";
    private final DepartmentCustomMapper departmentCustomMapper;
    private final DepartmentNodeCustomMapper departmentNodeCustomMapper;
    private final DepartmentUserCustomMapper departmentUserCustomMapper;
    private final ImUserMapper imUserMapper;
    private final UserAdminMapper userAdminMapper;
    private final ImUserService imUserService;
    private final ElasticsearchRestTemplate template;
    private final ImHttpClient imHttpClient;
    private final IdWorker idWorker;

    @Autowired
    private ImUserEsService imUserEsService;

    @Autowired
    private EncryptionService encryptionService;

    @Resource
    private IRoleService roleService;

    @DubboReference
    private DutyScheduleRpcApi dutyScheduleRpcApi;


    @DubboReference
    private UserAvatarRpcApi userAvatarRpcApi;

    public DepartmentCustomServiceImpl(DepartmentCustomMapper departmentCustomMapper,
                                       DepartmentNodeCustomMapper departmentNodeCustomMapper, DepartmentUserCustomMapper departmentUserCustomMapper,
                                       ImUserMapper imUserMapper, UserAdminMapper userAdminMapper, ImUserService imUserService,
                                       ImUserEsService imUserEsService, EncryptionService encryptionService,
                                       ElasticsearchRestTemplate template,
                                       ImHttpClient imHttpClient, IdWorker idWorker) {
        this.departmentCustomMapper = departmentCustomMapper;
        this.departmentNodeCustomMapper = departmentNodeCustomMapper;
        this.departmentUserCustomMapper = departmentUserCustomMapper;
        this.imUserMapper = imUserMapper;
        this.userAdminMapper = userAdminMapper;
        this.imUserService = imUserService;
        this.template = template;
        this.imHttpClient = imHttpClient;
        this.idWorker = idWorker;
    }

    @Override
    public List<DepartmentCustomVO> listTrees(Long imUserId) {

        Long ownerUserId = null;
//        if(imUserId != null) {
//            // 没传imuser，表示后台查询，可以查询所有通讯录
//            ownerUserId = resolveQueryOwnerUserId(imUserId);
//            if (ownerUserId == null) {
//                log.info("im用户:{}, 未被管理员绑定", imUserId);
//                return Collections.emptyList();
//            }
//        }

        List<DepartmentCustom> trees = departmentCustomMapper.selectList(Wrappers.lambdaQuery(DepartmentCustom.class)
                .eq(ownerUserId != null, DepartmentCustom::getCreateUserId, ownerUserId)
                .orderByAsc(DepartmentCustom::getGmtCreated));
        if (trees == null || trees.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Long> rootCountMap = queryRootCountMap(trees.stream().map(DepartmentCustom::getId)
                .collect(Collectors.toList()));
        return trees.stream().map(tree -> {
            DepartmentCustomVO vo = BeanCopyUtils.copyBean(tree, DepartmentCustomVO::new);
            vo.setRootNodeCount(rootCountMap.getOrDefault(tree.getId(), 0L));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public DepartmentCustom detailTree(Long id, Long imUserId) {
        Long ownerUserId = null;
//        if(imUserId != null) {
//            ownerUserId = resolveQueryOwnerUserId(imUserId);
//            if (ownerUserId == null) {
//                return null;
//            }
//        }
        return mustGetTree(id, ownerUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DepartmentCustom createTree(DepartmentCustomCO co) {
        validateTreeSave(null, co);
        DepartmentCustom tree = new DepartmentCustom()
                .setId(idWorker.nextId())
                .setTenantId(DEFAULT_TENANT_ID)
                .setName(trimToNull(co.getName()))
                .setCreateUserId(currentUserId())
                .setGmtCreated(new Date())
                .setDutyType(co.getDutyType());
        departmentCustomMapper.insert(tree);
        return tree;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DepartmentCustom updateTree(Long id, DepartmentCustomCO co) {
        mustGetTree(id);
        validateTreeSave(id, co);
        departmentCustomMapper.updateNameAndDutyTypeById(id, trimToNull(co.getName()), co.getDutyType());
        return mustGetTree(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTree(Long id) {
        mustGetTree(id);
        Long nodeCount = departmentNodeCustomMapper.selectCount(Wrappers.lambdaQuery(DepartmentNodeCustom.class)
                .eq(DepartmentNodeCustom::getDepartmentCustomId, id));
        if (nodeCount != null && nodeCount > 0) {
            throw new BusinessException("通讯录下存在节点，无法删除");
        }
        departmentCustomMapper.deleteById(id);
    }

    @Override
    public List<DepartmentNodeCustomVO> nodeTree(Long departmentCustomId, Long imUserId) {
        Long ownerUserId = null;
//        if(imUserId != null) {
//            ownerUserId = resolveQueryOwnerUserId(imUserId);
//            if (ownerUserId == null) {
//                return Collections.emptyList();
//            }
//        }
        mustGetTree(departmentCustomId, ownerUserId);
        List<DepartmentNodeCustom> nodes = departmentNodeCustomMapper.selectList(
                Wrappers.lambdaQuery(DepartmentNodeCustom.class)
                        .eq(DepartmentNodeCustom::getDepartmentCustomId, departmentCustomId)
                        .orderByAsc(DepartmentNodeCustom::getType)
                        .orderByAsc(DepartmentNodeCustom::getGmtCreated));
        if (nodes == null || nodes.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Long> userCountMap = queryUserCountMap(nodes.stream().map(DepartmentNodeCustom::getId)
                .collect(Collectors.toList()));
        Map<Long, DepartmentNodeCustomVO> nodeMap = new LinkedHashMap<>();
        for (DepartmentNodeCustom node : nodes) {
            nodeMap.put(node.getId(), toNode(node, userCountMap));
        }

        List<DepartmentNodeCustomVO> roots = new ArrayList<>();
        for (DepartmentNodeCustomVO node : nodeMap.values()) {
            DepartmentNodeCustomVO parent = nodeMap.get(node.getParentId());
            if (parent == null) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        return roots;
    }

    @Override
    public List<DepartmentNodeCustomVO> children(Long departmentCustomId, Long parentId, String keyword, Long imUserId) {
        return listNodes(departmentCustomId, parentId, keyword, imUserId);
    }

    @Override
    public List<DepartmentNodeCustomVO> listNodes(Long departmentCustomId, Long parentId, String keyword,
                                                  Long imUserId) {
        Long ownerUserId = null;
//        if(imUserId != null) {
//            ownerUserId = resolveQueryOwnerUserId(imUserId);
//            if (ownerUserId == null) {
//                return Collections.emptyList();
//            }
//        }
        mustGetTree(departmentCustomId, ownerUserId);
        Long normalizedParentId = normalizeParentId(parentId);
        if (normalizedParentId != null) {
            DepartmentNodeCustom parent = mustGetNode(normalizedParentId);
            if (!Objects.equals(parent.getDepartmentCustomId(), departmentCustomId)) {
                throw new BusinessException("父节点不属于当前通讯录");
            }
        }
        String normalizedKeyword = trimToNull(keyword);
        List<DepartmentNodeCustom> nodes = departmentNodeCustomMapper.selectList(
                Wrappers.lambdaQuery(DepartmentNodeCustom.class)
                        .eq(DepartmentNodeCustom::getDepartmentCustomId, departmentCustomId)
                        .and(normalizedParentId == null, wrapper -> wrapper.isNull(DepartmentNodeCustom::getParentId)
                                .or().eq(DepartmentNodeCustom::getParentId, VIRTUAL_ROOT_ID))
                        .eq(normalizedParentId != null, DepartmentNodeCustom::getParentId, normalizedParentId)
                        .and(normalizedKeyword != null, wrapper -> wrapper.like(DepartmentNodeCustom::getName, normalizedKeyword)
                                .or().like(DepartmentNodeCustom::getCode, normalizedKeyword))
                        .orderByAsc(DepartmentNodeCustom::getType)
                        .orderByAsc(DepartmentNodeCustom::getGmtCreated));
        if (nodes == null || nodes.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Long> userCountMap = queryUserCountMap(nodes.stream().map(DepartmentNodeCustom::getId)
                .collect(Collectors.toList()));
        return nodes.stream().map(node -> toNode(node, userCountMap)).collect(Collectors.toList());
    }

    @Override
    public DepartmentNodeCustom detailNode(Long id, Long imUserId) {
        Long ownerUserId = null;
//        if(imUserId != null) {
//            ownerUserId = resolveQueryOwnerUserId(imUserId);
//            if (ownerUserId == null) {
//                return null;
//            }
//        }
        DepartmentNodeCustom node = mustGetNode(id);
        mustGetTree(node.getDepartmentCustomId(), ownerUserId);
        return node;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DepartmentNodeCustom createNode(DepartmentNodeCustomCO co) {
        validateNodeSave(null, co);
        DepartmentNodeCustom node = new DepartmentNodeCustom()
                .setId(idWorker.nextId())
                .setDepartmentCustomId(co.getDepartmentCustomId())
                .setCode(trimToNull(co.getCode()))
                .setName(trimToNull(co.getName()))
                .setParentId(normalizeParentIdForSave(co.getParentId()))
                .setType(co.getType())
                .setCreateUserId(currentUserId())
                .setGmtCreated(new Date());
        departmentNodeCustomMapper.insert(node);
        return node;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DepartmentNodeCustom updateNode(Long id, DepartmentNodeCustomCO co) {
        DepartmentNodeCustom oldNode = mustGetNode(id);
        mustGetTree(oldNode.getDepartmentCustomId());
        validateNodeSave(id, co);
        if (Objects.equals(co.getType(), DepartmentNodeCustom.TYPE_DEPARTMENT) && hasChildUnit(id)) {
            throw new BusinessException("部门节点下不能存在子单位");
        }

        DepartmentNodeCustom node = new DepartmentNodeCustom()
                .setId(id)
                .setDepartmentCustomId(co.getDepartmentCustomId())
                .setCode(trimToNull(co.getCode()))
                .setName(trimToNull(co.getName()))
                .setParentId(normalizeParentIdForSave(co.getParentId()))
                .setType(co.getType());
        departmentNodeCustomMapper.updateById(node);
        return mustGetNode(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNode(Long id) {
        DepartmentNodeCustom node = mustGetNode(id);
        mustGetTree(node.getDepartmentCustomId());
        if (hasChildren(id)) {
            throw new BusinessException("存在子单位或部门，无法删除");
        }
        Long userCount = departmentUserCustomMapper.selectCount(Wrappers.lambdaQuery(DepartmentUserCustom.class)
                .eq(DepartmentUserCustom::getCustomDeptId, id));
        if (userCount != null && userCount > 0) {
            throw new BusinessException("通讯录节点下存在用户，无法删除");
        }
        departmentNodeCustomMapper.deleteById(id);
    }

    @Override
    public List<DepartmentCustomUserVO> listUsers(Long nodeId, Long imUserId) {
        Long ownerUserId = null;
//        if(imUserId != null) {
//            ownerUserId = resolveQueryOwnerUserId(imUserId);
//            if (ownerUserId == null) {
//                return Collections.emptyList();
//            }
//        }
        DepartmentNodeCustom node = mustGetNode(nodeId);
        mustGetTree(node.getDepartmentCustomId(), ownerUserId);
        List<DepartmentUserCustom> relationList = departmentUserCustomMapper.selectList(
                Wrappers.lambdaQuery(DepartmentUserCustom.class).eq(DepartmentUserCustom::getCustomDeptId, nodeId));
        if (relationList == null || relationList.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> userIds = relationList.stream().map(DepartmentUserCustom::getUserId).collect(Collectors.toList());
        Map<Long, ImUserDO> userMap = Optional.ofNullable(imUserService.getByIdList(userIds))
                .orElseGet(Collections::emptyList).stream()
                .collect(Collectors.toMap(this::userId, Function.identity(), (first, second) -> first));

        return relationList.stream().map(relation -> {
            DepartmentCustomUserVO vo = BeanCopyUtils.copyBean(relation, DepartmentCustomUserVO::new);
            vo.setUser(userMap.get(relation.getUserId()));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public IPage<DepartmentCustomUserVO> pageUsers(Long nodeId, Long pageNum, Long pageSize, String keyword,
                                                   Long imUserId) {
        Page<DepartmentCustomUserVO> result = new Page<>(normalizePageNum(pageNum), normalizePageSize(pageSize));
        Long ownerUserId = null;
//        if(imUserId != null) {
//            ownerUserId = resolveQueryOwnerUserId(imUserId);
//            if (ownerUserId == null) {
//                result.setRecords(Collections.emptyList());
//                return result;
//            }
//        }
        DepartmentNodeCustom node = mustGetNode(nodeId);
        mustGetTree(node.getDepartmentCustomId(), ownerUserId);
        List<DepartmentUserCustom> relationList = departmentUserCustomMapper.selectList(
                Wrappers.lambdaQuery(DepartmentUserCustom.class).eq(DepartmentUserCustom::getCustomDeptId, nodeId));
        if (relationList == null || relationList.isEmpty()) {
            result.setRecords(Collections.emptyList());
            return result;
        }

        Map<Long, DepartmentUserCustom> relationMap = relationList.stream()
                .collect(Collectors.toMap(DepartmentUserCustom::getUserId, Function.identity(), (first, second) -> first));
        String normalizedKeyword = trimToNull(keyword);
        boolean hasKeywords = normalizedKeyword != null;

        // 判断是否需要使用ES查询
        boolean useES = encryptionService.encryptEnabled() && hasKeywords;

        IPage<ImUserDO> userPage;
        if (useES) {
            // 使用ES查询（支持加密字段模糊搜索）
            userPage = queryUsersByES(result, relationMap.keySet(), normalizedKeyword);
        } else {
            // 使用MySQL查询
            userPage = queryUsersByMySQL(result, relationMap.keySet(), normalizedKeyword);
        }

        List<DepartmentCustomUserVO> records = Optional.ofNullable(userPage.getRecords()).orElseGet(Collections::emptyList)
                .stream().map(user -> {
                    DepartmentCustomUserVO vo = BeanCopyUtils.copyBean(relationMap.get(userId(user)),
                            DepartmentCustomUserVO::new);
                    vo.setUser(user);
                    return vo;
                }).collect(Collectors.toList());
        result.setTotal(userPage.getTotal());
        result.setRecords(records);
        return result;
    }

    /**
     * 使用ES查询用户（支持加密字段模糊搜索）
     */
    private IPage<ImUserDO> queryUsersByES(Page<DepartmentCustomUserVO> result, Set<Long> userIds, String keyword) {
        // ES查询：先查所有用户ID，再在内存中过滤关键词
        // 由于ES中存储的是明文，可以直接模糊搜索
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        boolQuery.must(QueryBuilders.termsQuery("id", userIds));

        // keywords 模糊查询（name OR mobile OR idCard OR code OR email OR departmentCode OR departmentName）
        BoolQueryBuilder keywordsQuery = QueryBuilders.boolQuery();
        keywordsQuery.should(QueryBuilders.wildcardQuery("name", "*" + keyword + "*"));
        keywordsQuery.should(QueryBuilders.wildcardQuery("code", "*" + keyword + "*"));
        keywordsQuery.should(QueryBuilders.wildcardQuery("mobile", "*" + keyword + "*"));
        keywordsQuery.should(QueryBuilders.wildcardQuery("email", "*" + keyword + "*"));
        keywordsQuery.should(QueryBuilders.wildcardQuery("idCard", "*" + keyword + "*"));
        keywordsQuery.should(QueryBuilders.wildcardQuery("departmentCode", "*" + keyword + "*"));
        keywordsQuery.should(QueryBuilders.wildcardQuery("departmentName", "*" + keyword + "*"));
        keywordsQuery.minimumShouldMatch(1);
        boolQuery.must(keywordsQuery);

        // type != 0
        boolQuery.mustNot(QueryBuilders.termQuery("type", 0));

        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(boolQuery)
                .withPageable(PageRequest.of((int) result.getCurrent() - 1, (int) result.getSize()))
                .withSort(SortBuilders.fieldSort("gmtUpdated").order(SortOrder.DESC))
                .build();

        SearchHits<ImUserES> searchHits = template.search(searchQuery, ImUserES.class);

        List<ImUserDO> records = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(this::convertEsToDO)
                .collect(Collectors.toList());

        Page<ImUserDO> userPage = new Page<>(result.getCurrent(), result.getSize());
        userPage.setRecords(records);
        userPage.setTotal(searchHits.getTotalHits());
        return userPage;
    }

    /**
     * 使用MySQL查询用户
     */
    private IPage<ImUserDO> queryUsersByMySQL(Page<DepartmentCustomUserVO> result, Set<Long> userIds, String keyword) {
        return imUserMapper.selectPage(new Page<>(result.getCurrent(), result.getSize()),
                Wrappers.lambdaQuery(ImUserDO.class)
                        .in(ImUserDO::getId, new ArrayList<>(userIds))
                        .and(keyword != null, wrapper -> wrapper.like(ImUserDO::getName, keyword)
                                .or().like(ImUserDO::getCode, keyword)
                                .or().like(ImUserDO::getMobile, keyword)
                                .or().like(ImUserDO::getEmail, keyword)
                                .or().like(ImUserDO::getIdCard, keyword)
                                .or().like(ImUserDO::getDepartmentCode, keyword)
                                .or().like(ImUserDO::getDepartmentName, keyword))
                        .ne(ImUserDO::getType, 0)
                        .orderByDesc(ImUserDO::getGmtUpdated));
    }

    /**
     * 将ES实体转换为DO实体
     */
    private ImUserDO convertEsToDO(ImUserES es) {
        ImUserDO dto = new ImUserDO();
        BeanUtils.copyProperties(es, dto);
        return dto;
    }

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public List<DepartmentCustomDutyScheduleVO> listDutyScheduleUsers(Long nodeId, String dutyStartDate,
                                                                      String dutyEndDate, Long dutyType, Long imUserId) {
        Long ownerUserId = null;
//        if(imUserId != null){
//             ownerUserId = resolveQueryOwnerUserId(imUserId);
//            if (ownerUserId == null) {
//                return Collections.emptyList();
//            }
//        }

        DepartmentNodeCustom node = mustGetNode(nodeId);
        mustGetTree(node.getDepartmentCustomId(), ownerUserId);

        // 查询组织下所有人员
        List<DepartmentUserCustom> relationList = departmentUserCustomMapper.selectList(
                Wrappers.lambdaQuery(DepartmentUserCustom.class).eq(DepartmentUserCustom::getCustomDeptId, nodeId));
        if (relationList == null || relationList.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> userIds = relationList.stream().map(DepartmentUserCustom::getUserId).filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 没有排班类型的树或者没有排班时间，则直接返回静态人员
        if (null == dutyType || (dutyStartDate == null && dutyEndDate == null)) {
            return getStaticUsers(userIds);
        } else {
            // 动态树查询，需要根据时间查询值班人员
            return getDepartmentCustomDutyScheduleVOS(dutyStartDate, dutyEndDate, dutyType, userIds);
        }
    }

    @NotNull
    private List<DepartmentCustomDutyScheduleVO> getDepartmentCustomDutyScheduleVOS(String dutyStartDate, String dutyEndDate, Long dutyType, List<Long> userIds) {
        // 解析时间参数
        LocalDateTime startDateTime = parseDateTime(dutyStartDate);
        LocalDateTime endDateTime = parseDateTime(dutyEndDate);
        if (startDateTime != null && endDateTime != null && startDateTime.isAfter(endDateTime)) {
            throw new BusinessException("排班开始时间不能晚于排班结束时间");
        }

        List<DutyScheduleUserVO> dutyScheduleUsers =
                dutyScheduleRpcApi.listDutyScheduleUsers(userIds, startDateTime, endDateTime, dutyType);
        if (dutyScheduleUsers == null || dutyScheduleUsers.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, DepartmentCustomDutyScheduleVO> resultMap = new LinkedHashMap<>();
        for (DutyScheduleUserVO dutyScheduleUser : dutyScheduleUsers) {
            DepartmentCustomDutyScheduleVO userVO = resultMap.computeIfAbsent(dutyScheduleUser.getUserId(), userId -> {
                DepartmentCustomDutyScheduleVO vo = BeanCopyUtils.copyBean(dutyScheduleUser,
                        DepartmentCustomDutyScheduleVO::new);
                vo.setId(userId);
                return vo;
            });
            userVO.getDutyTimeList().add(BeanCopyUtils.copyBean(dutyScheduleUser,
                    DepartmentCustomDutyScheduleVO.DutyTimeVO::new));
        }
        return new ArrayList<>(resultMap.values());
    }

    @NotNull
    private List<DepartmentCustomDutyScheduleVO> getStaticUsers(List<Long> userIds) {
        List<ImUserDto> imUserDtos = imUserMapper.listByIds(userIds);
        if (CollectionUtils.isEmpty(imUserDtos)) {
            return Collections.emptyList();
        }
        List<String> fileIds = imUserDtos.stream().map(ImUserDto::getAvatar).distinct().collect(Collectors.toList());
        Map<String, String> userAvatar = userAvatarRpcApi.getUserAvatar(fileIds);

        return imUserDtos.stream().map(imUserDto -> {
            DepartmentCustomDutyScheduleVO departmentCustomDutyScheduleVO = new DepartmentCustomDutyScheduleVO();
            departmentCustomDutyScheduleVO.setId(imUserDto.getId());
            departmentCustomDutyScheduleVO.setUserName(imUserDto.getName());
            departmentCustomDutyScheduleVO.setUserId(imUserDto.getId());
            String avatarPath = userAvatar.get(imUserDto.getAvatar());
            departmentCustomDutyScheduleVO.setAvatar(StringUtils.isBlank(avatarPath) ? DEFAULT_USER_AVATAR_PATH : avatarPath);
            return departmentCustomDutyScheduleVO;
        }).collect(Collectors.toList());
    }

    /**
     * 解析时间字符串，支持格式：
     * - yyyy-MM-dd HH:mm:ss
     * - yyyy-MM-dd
     *
     * @param dateTimeStr 时间字符串
     * @return LocalDateTime，如果为空则返回null
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }
        dateTimeStr = dateTimeStr.trim();
        try {
            // 尝试解析 yyyy-MM-dd HH:mm:ss
            if (dateTimeStr.length() == 19) {
                return LocalDateTime.parse(dateTimeStr, DATE_TIME_FORMATTER);
            }
            // 尝试解析 yyyy-MM-dd（当作当天 00:00:00）
            if (dateTimeStr.length() == 10) {
                return LocalDate.parse(dateTimeStr).atStartOfDay();
            }
            throw new BusinessException("时间格式错误，支持格式：yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd");
        } catch (Exception e) {
            throw new BusinessException("时间格式错误，支持格式：yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addUsers(Long nodeId, List<ImUserDO> users) {
        DepartmentNodeCustom node = mustGetNode(nodeId);
        mustGetTree(node.getDepartmentCustomId(), null);
        List<Long> userIds = users.stream().map(ImUserDO::getId).collect(Collectors.toList());
        List<Long> normalizedUserIds = normalizeUserIds(userIds);
        // 从 imuser 表中查询存在的用户
        List<ImUserDO> existingUsers = imUserService.getByIdList(normalizedUserIds);
        // 过滤出存在于 imuser 表中的用户ID
        List<Long> validUserIds = existingUsers.stream()
                .map(ImUserDO::getId)
                .collect(Collectors.toList());

        // 记录被过滤掉的用户ID
        List<Long> invalidUserIds = normalizedUserIds.stream()
                .filter(userId -> !validUserIds.contains(userId))
                .collect(Collectors.toList());
        // 需要新增查询用户并新增到表中
        if (CollectionUtils.isNotEmpty(invalidUserIds)) {
            try {
                dealWithInvalidUserIds(invalidUserIds);
            } catch (NoSuchAlgorithmException e) {
                throw new BusinessException("存在未登录用户，请先登录app");
            }
        }else{
            log.info("所有用户都已登录，无需新增用户");
        }


        validateUserPermission(users);
        Long existCount = departmentUserCustomMapper.selectCount(Wrappers.lambdaQuery(DepartmentUserCustom.class)
                .eq(DepartmentUserCustom::getCustomDeptId, nodeId)
                .in(DepartmentUserCustom::getUserId, normalizedUserIds));
        if (existCount != null && existCount > 0) {
            throw new BusinessException("通讯录节点下已存在重复用户");
        }

        Date now = new Date();
        Long createUserId = currentUserId();
        List<DepartmentUserCustom> relations = normalizedUserIds.stream()
                .map(userId -> new DepartmentUserCustom()
                        .setId(idWorker.nextId())
                        .setCustomDeptId(nodeId)
                        .setUserId(userId)
                        .setCreateUserId(createUserId)
                        .setGmtCreated(now))
                .collect(Collectors.toList());
        departmentUserCustomMapper.insertBatch(relations);
    }

    private void dealWithInvalidUserIds(List<Long> invalidUserIds) throws NoSuchAlgorithmException {
        if (invalidUserIds == null || invalidUserIds.isEmpty()) {
            return;
        }

        log.info("开始处理未登录用户ID，总数: {}", invalidUserIds.size());

        // 分组处理，每组50个用户
        int batchSize = 50;
        List<List<Long>> batches = new ArrayList<>();
        for (int i = 0; i < invalidUserIds.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, invalidUserIds.size());
            batches.add(invalidUserIds.subList(i, endIndex));
        }

        // 批量查询用户信息
        List<ImUser> allUsers = new ArrayList<>();
        for (int i = 0; i < batches.size(); i++) {
            List<Long> batch = batches.get(i);
            String userIdsStr = batch.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));

            try {
                UserGetVo userGetVo = imHttpClient.userPageById(userIdsStr);
                if (userGetVo != null && userGetVo.getResults() != null) {
                    allUsers.addAll(userGetVo.getResults());
                    log.info("第 {} 批查询成功，查询 {} 个用户ID，返回 {} 个用户",
                            i + 1, batch.size(), userGetVo.getResults().size());

                    // 记录失败的用户ID
                    if (userGetVo.getFailures() != null && !userGetVo.getFailures().isEmpty()) {
                        log.warn("第 {} 批查询中部分用户ID查询失败: {}", i + 1, userGetVo.getFailures());
                    }
                } else {
                    log.warn("第 {} 批查询返回空结果，查询的用户ID: {}", i + 1, userIdsStr);
                }
            } catch (Exception e) {
                log.error("第 {} 批查询失败，查询的用户ID: {}", i + 1, userIdsStr, e);
            }
        }

        log.info("处理完成，共查询到 {} 个用户", allUsers.size());
        String passWord = PBKDF2Util.PBKDF2ForPassStandard(DEFAULT_PASSWORD, PBKDF2Util.generateSalt());

        // 例如：插入到 imuser 表、记录日志等
        List<ImUserDO> userList = allUsers.stream().map(imUser -> {
            ImUserDO userDORaw = BeanCopyUtils.copyBean(imUser, ImUserDO::new);
            // 设置部门信息
            Optional.ofNullable(imUser.getUserDepartments()).stream().flatMap(Collection::stream)
                    .filter(ImUser.UserDepartment::getIsPrimary).findAny().ifPresent(dep -> {
                        userDORaw.setDepartmentCode(dep.getDepartmentCode());
                        userDORaw.setDepartmentName(dep.getDepartmentName());
                        userDORaw.setDepartmentId(dep.getId());
                    });

            // 设置密码
            userDORaw.setPwdTime(new Date()).setType(1).setStatus(0)
                    .setPassword(passWord);
            return userDORaw;
        }).collect(Collectors.toList());

        if (encryptionService.encryptEnabled()) {
            // 写入es
            imUserEsService.insert(userList);
        }
        imUserMapper.insert(userList);
        // 异步设置用户角色
        CompletableFuture.runAsync(() -> roleService.setRole(invalidUserIds,6L));

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUsers(Long nodeId, List<Long> userIds) {
        DepartmentNodeCustom node = mustGetNode(nodeId);
        mustGetTree(node.getDepartmentCustomId(), null);
        departmentUserCustomMapper.delete(Wrappers.lambdaQuery(DepartmentUserCustom.class)
                .eq(DepartmentUserCustom::getCustomDeptId, nodeId)
                .in(DepartmentUserCustom::getUserId, normalizeUserIds(userIds)));
    }

    @Override
    public IPage<ImUserDO> pageAvailableUsers(Long pageNum, Long pageSize, ImUserQO qo) {
        ImUserQO query = qo == null ? new ImUserQO() : qo;
        long current = normalizePageNum(pageNum);
        long size = normalizePageSize(pageSize);
        IPage<ImUserDO> userPage = queryAvailableUsersFromIm(current, size, query);
        fillBoundCustomDepartments(userPage.getRecords());
        return userPage;
    }

    private IPage<ImUserDO> queryAvailableUsersFromIm(long pageNum, long pageSize, ImUserQO qo) {
        ImPage<ImUser> imPage = imHttpClient.userPageByDepartment((int) pageNum, (int) pageSize,
                trimToNull(qo.getDepartmentCode()), 1, availableUserKeywords(qo), trimToNull(qo.getPrivString()),
                trimToNull(qo.getName()), null);
        Page<ImUserDO> result = new Page<>(pageNum, pageSize);
        if (imPage == null) {
            result.setRecords(Collections.emptyList());
            return result;
        }
        List<ImUserDO> records = Optional.ofNullable(imPage.getRecords()).orElseGet(Collections::emptyList)
                .stream().map(this::toImUserDO).collect(Collectors.toList());
        result.setRecords(records);
        result.setTotal(Optional.ofNullable(imPage.getTotal()).orElseGet(() ->
                Optional.ofNullable(imPage.getTotalCount()).orElse(records.size())));
        return result;
    }

    private String availableUserKeywords(ImUserQO qo) {
        return Stream.of(qo.getCode(), qo.getMobile(), qo.getIdCard(), qo.getEmail(), qo.getDepartmentName())
                .map(this::trimToNull)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private ImUserDO toImUserDO(ImUser user) {
        ImUserDO result = new ImUserDO()
                .setId(user.getId())
                .setCode(user.getCode())
                .setName(user.getName())
                .setAvatar(user.getAvatar())
                .setGender(user.getGender())
                .setMobile(user.getMobile())
                .setEmail(user.getEmail())
                .setIsdn(user.getIsdn())
                .setIdCard(user.getIdCard())
                .setDistrict(user.getDistrict())
                .setDirectLeaderId(user.getDirectLeaderId())
                .setDirectLeaderName(user.getDirectLeaderName())
                .setType(1);
        ImUser.UserDepartment department = user.getPrimaryDepartment();
        if (department != null) {
            result.setDepartmentId(department.getId())
                    .setDepartmentCode(department.getDepartmentCode())
                    .setDepartmentName(department.getDepartmentName());
        }
        return result;
    }

    private void validateTreeSave(Long id, DepartmentCustomCO co) {
        if (co == null) {
            throw new BusinessException("请求体不能为空");
        }
        String name = trimToNull(co.getName());
        if (name == null) {
            throw new BusinessException("通讯录名称不能为空");
        }
        if (name.length() > 255) {
            throw new BusinessException("通讯录名称长度不能超过255个字符");
        }
        Long exists = departmentCustomMapper.selectCount(Wrappers.lambdaQuery(DepartmentCustom.class)
                .eq(DepartmentCustom::getName, name)
                .eq(DepartmentCustom::getCreateUserId, currentUserId())
                .ne(id != null, DepartmentCustom::getId, id));
        if (exists != null && exists > 0) {
            throw new BusinessException("通讯录名称已存在");
        }
    }

    private void validateNodeSave(Long id, DepartmentNodeCustomCO co) {
        if (co == null) {
            throw new BusinessException("请求体不能为空");
        }
        mustGetTree(co.getDepartmentCustomId());
        if (trimToNull(co.getName()) == null) {
            throw new BusinessException("节点名称不能为空");
        }
        if (trimToNull(co.getName()).length() > 255) {
            throw new BusinessException("节点名称长度不能超过255个字符");
        }
        if (!Objects.equals(co.getType(), DepartmentNodeCustom.TYPE_UNIT)
                && !Objects.equals(co.getType(), DepartmentNodeCustom.TYPE_DEPARTMENT)) {
            throw new BusinessException("类型必须是1(单位)或2(部门)");
        }

        String code = trimToNull(co.getCode());
        if (code != null && code.length() > 255) {
            throw new BusinessException("编码长度不能超过255个字符");
        }
        Long exists = departmentNodeCustomMapper.selectCount(Wrappers.lambdaQuery(DepartmentNodeCustom.class)
                .eq(DepartmentNodeCustom::getDepartmentCustomId, co.getDepartmentCustomId())
                .eq(code != null, DepartmentNodeCustom::getCode, code)
                .ne(id != null, DepartmentNodeCustom::getId, id));
        if (code != null && exists != null && exists > 0) {
            throw new BusinessException("当前通讯录下编码已存在");
        }

        Long parentId = normalizeParentIdForSave(co.getParentId());
        if (isVirtualRoot(parentId)) {
            return;
        }
        if (Objects.equals(parentId, id)) {
            throw new BusinessException("父节点不能是当前节点");
        }
        DepartmentNodeCustom parent = mustGetNode(parentId);
        if (!Objects.equals(parent.getDepartmentCustomId(), co.getDepartmentCustomId())) {
            throw new BusinessException("父节点不属于当前通讯录");
        }
        if (Objects.equals(parent.getType(), DepartmentNodeCustom.TYPE_DEPARTMENT)
                && Objects.equals(co.getType(), DepartmentNodeCustom.TYPE_UNIT)) {
            throw new BusinessException("部门节点下不能新增单位");
        }
        if (id != null && isDescendant(parentId, id)) {
            throw new BusinessException("父节点不能是当前节点的子孙节点");
        }
    }

    private boolean isDescendant(Long candidateId, Long ancestorId) {
        Long currentId = candidateId;
        Set<Long> visited = new HashSet<>();
        while (currentId != null && visited.add(currentId)) {
            if (Objects.equals(currentId, ancestorId)) {
                return true;
            }
            DepartmentNodeCustom current = departmentNodeCustomMapper.selectById(currentId);
            currentId = current == null ? null : normalizeParentIdForTraversal(current.getParentId());
        }
        return false;
    }

    private boolean hasChildren(Long id) {
        Long count = departmentNodeCustomMapper.selectCount(Wrappers.lambdaQuery(DepartmentNodeCustom.class)
                .eq(DepartmentNodeCustom::getParentId, id));
        return count != null && count > 0;
    }

    private boolean hasChildUnit(Long id) {
        Long count = departmentNodeCustomMapper.selectCount(Wrappers.lambdaQuery(DepartmentNodeCustom.class)
                .eq(DepartmentNodeCustom::getParentId, id)
                .eq(DepartmentNodeCustom::getType, DepartmentNodeCustom.TYPE_UNIT));
        return count != null && count > 0;
    }

    private DepartmentCustom mustGetTree(Long id) {
        return mustGetTree(id, null);
    }

    private DepartmentCustom mustGetTree(Long id, Long ownerUserId) {
        if (id == null || id <= 0) {
            throw new BusinessException("通讯录ID不能为空且必须大于0");
        }
        DepartmentCustom tree = departmentCustomMapper.selectOne(Wrappers.lambdaQuery(DepartmentCustom.class)
                .eq(DepartmentCustom::getId, id)
                .eq(ownerUserId != null, DepartmentCustom::getCreateUserId, ownerUserId));
        if (tree == null) {
            throw new BusinessException("通讯录不存在");
        }
        return tree;
    }

    private Long resolveQueryOwnerUserId(Long imUserId) {
        if (imUserId == null) {
            return currentSecurityUserId();
        }
        UserAdmin userAdmin = userAdminMapper.selectOne(Wrappers.lambdaQuery(UserAdmin.class)
                .eq(UserAdmin::getImUserId, imUserId)
                .last("LIMIT 1"));
        if (userAdmin == null || userAdmin.getId() == null) {
            throw new BusinessException("关联警信用户未绑定管理员用户");
        }
        return userAdmin.getId();
    }

    private DepartmentNodeCustom mustGetNode(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException("节点ID不能为空且必须大于0");
        }
        DepartmentNodeCustom node = departmentNodeCustomMapper.selectById(id);
        if (node == null) {
            throw new BusinessException("通讯录节点不存在");
        }
        return node;
    }

    private void fillBoundCustomDepartments(List<ImUserDO> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        users.forEach(user -> user.setBoundCustomDepartments(Collections.emptyList()));
        List<Long> userIds = users.stream().map(this::userId).filter(Objects::nonNull).distinct()
                .collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return;
        }

        List<DepartmentUserCustom> relationList = departmentUserCustomMapper.selectList(
                Wrappers.lambdaQuery(DepartmentUserCustom.class).in(DepartmentUserCustom::getUserId, userIds));
        if (relationList == null || relationList.isEmpty()) {
            return;
        }

        List<Long> ownerTreeIds = departmentCustomMapper.selectList(Wrappers.lambdaQuery(DepartmentCustom.class)
                        .select(DepartmentCustom::getId)
                        .eq(DepartmentCustom::getCreateUserId, currentUserId()))
                .stream().map(DepartmentCustom::getId).collect(Collectors.toList());
        if (ownerTreeIds.isEmpty()) {
            return;
        }

        List<Long> nodeIds = relationList.stream().map(DepartmentUserCustom::getCustomDeptId).filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());
        if (nodeIds.isEmpty()) {
            return;
        }
        Map<Long, DepartmentNodeCustom> nodeMap = departmentNodeCustomMapper.selectList(
                        Wrappers.lambdaQuery(DepartmentNodeCustom.class)
                                .in(DepartmentNodeCustom::getId, nodeIds)
                                .in(DepartmentNodeCustom::getDepartmentCustomId, ownerTreeIds))
                .stream()
                .collect(Collectors.toMap(DepartmentNodeCustom::getId, Function.identity(), (first, second) -> first));

        Map<Long, List<DepartmentNodeCustom>> userNodeMap = new LinkedHashMap<>();
        for (DepartmentUserCustom relation : relationList) {
            DepartmentNodeCustom node = nodeMap.get(relation.getCustomDeptId());
            if (node != null) {
                userNodeMap.computeIfAbsent(relation.getUserId(), key -> new ArrayList<>()).add(node);
            }
        }
        users.forEach(user -> user.setBoundCustomDepartments(
                userNodeMap.getOrDefault(userId(user), Collections.emptyList())));
    }

    private DepartmentNodeCustomVO toNode(DepartmentNodeCustom node, Map<Long, Long> userCountMap) {
        DepartmentNodeCustomVO vo = BeanCopyUtils.copyBean(node, DepartmentNodeCustomVO::new);
        vo.setUserCount(userCountMap.getOrDefault(node.getId(), 0L));
        return vo;
    }

    private Map<Long, Long> queryRootCountMap(List<Long> treeIds) {
        if (treeIds == null || treeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = departmentNodeCustomMapper.selectMaps(new QueryWrapper<DepartmentNodeCustom>()
                .select("department_custom_id", "COUNT(1) AS node_count")
                .in("department_custom_id", treeIds)
                .and(wrapper -> wrapper.isNull("parent_id").or().eq("parent_id", VIRTUAL_ROOT_ID))
                .groupBy("department_custom_id"));
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyMap();
        }
        return rows.stream().collect(Collectors.toMap(row -> getMapLong(row, "department_custom_id"),
                row -> getMapLong(row, "node_count"), (first, second) -> first));
    }

    private Map<Long, Long> queryUserCountMap(List<Long> nodeIds) {
        if (nodeIds == null || nodeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = departmentUserCustomMapper.selectMaps(new QueryWrapper<DepartmentUserCustom>()
                .select("custom_dept_id", "COUNT(1) AS user_count")
                .in("custom_dept_id", nodeIds)
                .groupBy("custom_dept_id"));
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyMap();
        }
        return rows.stream().collect(Collectors.toMap(row -> getMapLong(row, "custom_dept_id"),
                row -> getMapLong(row, "user_count"), (first, second) -> first));
    }

    private List<Long> normalizeUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            throw new BusinessException("用户ID列表不能为空");
        }
        List<Long> normalized = userIds.stream().filter(userId -> userId != null && userId > 0).distinct()
                .collect(Collectors.toList());
        if (normalized.isEmpty()) {
            throw new BusinessException("用户ID列表不能为空且必须大于0");
        }
        return normalized;
    }

    private void validateUserPermission(List<ImUserDO> users) {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        Set<Long> permittedIds = permittedDepartmentIds();
        if (permittedIds.isEmpty()) {
            throw new BusinessException("当前用户没有可用的警信组织权限");
        }
        boolean denied = users.stream().map(this::userDepartmentId)
                .anyMatch(departmentId -> departmentId == null || !permittedIds.contains(departmentId));
        if (denied) {
            throw new BusinessException("用户超出当前权限范围");
        }
    }

    private Set<Long> permittedDepartmentIds() {
        UserInfo user = SecurityUtils.getUser();
        if (user == null || CollectionUtils.isEmpty(user.getImOrgPrivIds())) {
            return Collections.emptySet();
        }
        return new HashSet<>(user.getImOrgPrivIds());
    }

    private Long currentUserId() {
        return Optional.ofNullable(SecurityUtils.getUser()).map(UserInfo::getUserId).orElse(DEFAULT_USER_ID);
    }

    private Long currentSecurityUserId() {
        return Optional.ofNullable(SecurityUtils.getUser()).map(UserInfo::getUserId).orElse(null);
    }

    private Long normalizeParentId(Long parentId) {
        return isVirtualRoot(parentId) ? null : parentId;
    }

    private Long normalizeParentIdForSave(Long parentId) {
        return isVirtualRoot(parentId) ? VIRTUAL_ROOT_ID : parentId;
    }

    private Long normalizeParentIdForTraversal(Long parentId) {
        return isVirtualRoot(parentId) ? null : parentId;
    }

    private boolean isVirtualRoot(Long parentId) {
        return parentId == null || Objects.equals(parentId, VIRTUAL_ROOT_ID);
    }

    private long normalizePageNum(Long pageNum) {
        return pageNum == null || pageNum < 1 ? 1L : pageNum;
    }

    private long normalizePageSize(Long pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10L;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private Long userId(ImUserDO user) {
        return user.getId();
    }

    private Long userDepartmentId(ImUserDO user) {
        return user.getDepartmentId();
    }

    private Long toLong(Object value) {
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    private Long getMapLong(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) {
            value = row.get(key.toUpperCase());
        }
        Long longValue = toLong(value);
        if (longValue == null) {
            throw new BusinessException("查询通讯录统计数据失败");
        }
        return longValue;
    }
}
