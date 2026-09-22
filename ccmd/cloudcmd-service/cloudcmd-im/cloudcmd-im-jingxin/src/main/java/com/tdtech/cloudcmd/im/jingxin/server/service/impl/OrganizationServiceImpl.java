package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.im.jingxin.api.entity.department.OrganizationVO;
import com.tdtech.cloudcmd.im.jingxin.client.CachedImConfig;
import com.tdtech.cloudcmd.im.jingxin.client.ImHttpClient;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImDepartment;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImToken;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CommonFlag;
import com.tdtech.cloudcmd.im.jingxin.server.entity.Organization;
import com.tdtech.cloudcmd.im.jingxin.server.service.IOrganizationService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.CommonFlagMapper;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.OrganizationMapper;
import com.tdtech.cloudcmd.redis.RedisLockFactory;
import com.tdtech.cloudcmd.redis.RedisUtil;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.StringUtils;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrganizationServiceImpl extends ServiceImpl<OrganizationMapper, Organization>
        implements IOrganizationService {

    private static final String ORGANIZATION_CACHE_KEY_PREFIX = "cloudcmd:organization:cache:";
    private static final String ORGANIZATION_TREE_MAP_BY_PARENT_ID = ORGANIZATION_CACHE_KEY_PREFIX + "tree:parent_id";
    private static final String ORGANIZATION_TREE_MAP_BY_PARENT_CODE =
            ORGANIZATION_CACHE_KEY_PREFIX + "tree:parent_code";
    private static final String SYNC_FLAG_KEY = "sync_organization_from_im_flag";
    private static final String SYNC_LOCK_KEY = "cloudcmd:organization:sync-lock";
    private static final Duration SYNC_LOCK_EXPIRE_TIME = Duration.ofMinutes(30L);

    // 默认根节点的parentId
    private static final Long DEFAULT_ROOT_PARENT_ID = 0L;

    private static final String DEFAULT_ROOT_PARENT_CODE = "0";

    @Resource
    private OrganizationMapper organizationMapper;

    @Resource
    private CommonFlagMapper commonFlagMapper;

    @Resource
    private CachedImConfig cachedImConfig;

    @Resource
    private ImService imService;

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private RedisLockFactory redisLockFactory;

    @Resource
    private ImHttpClient imHttpClient;

    @Resource(name = "groupInfoExecutorService")
    private TaskExecutor taskExecutor;

    @EventListener(ApplicationReadyEvent.class)
    public void completeFromIMAfterStartup() {
        taskExecutor.execute(() -> syncFromIMWithLock(true, "startup"));
    }

    @Override
    public boolean syncFromIM() {
        return syncFromIMWithLock(false, "scheduled");
    }

    private boolean syncFromIMWithLock(boolean force, String source) {
        String enableFlag = cachedImConfig.getConfig("DEPARTMENT_SYNC_SIGN");
        boolean isClosed = StringUtils.isBlank(enableFlag) || (!"true".equals(enableFlag));
        if (isClosed) {
            log.info("sync organization from im is closed");
            return false;
        }
        if (!force && alreadyDone()) {
            log.info("组织部门已经同步过了");
            return true;
        }
        var redisLock = redisLockFactory.newRedisLock(SYNC_LOCK_KEY, SYNC_LOCK_EXPIRE_TIME);
        if (!redisLock.tryLock(0L, TimeUnit.MILLISECONDS)) {
            log.warn("sync organization from im skipped, lock busy, source: {}", source);
            return true;
        }
        try {
            boolean synced = alreadyDone();
            if (!force && synced) {
                log.info("organization department has already been synchronized");
                return true;
            }
            return doSyncFromIM(!synced, source);
        } finally {
            redisLock.unlock();
        }
    }

    private boolean doSyncFromIM(boolean insertSyncFlag, String source) {
        // 查询im部门列表
        List<ImDepartment> imDepartments = imService.queryDepartmentForList(null);
        if (CollectionUtils.isEmpty(imDepartments)) {
            log.warn("同步im组织部门信息全量im部门数据结果为空");
            return true;
        }
        List<Organization> dataList = imDepartments.stream().map(Organization::new).collect(Collectors.toList());
        saveOrUpdateBatch(dataList);
        if (insertSyncFlag) {
            insertFlag();
        }
        deleteCache();
        log.info("sync organization from im finished, source: {}, count: {}", source, dataList.size());
        return true;
    }


    // 组织部门数据看护，每周六凌晨1点执行
    @Scheduled(cron = "0 0 1 ? * SAT")
    public void dataStewardship(){
        log.info("开始执行组织部门数据看护定时任务");
        List<ImDepartment> imDepartments = imService.queryDepartmentForList(null);
        if (CollectionUtils.isEmpty(imDepartments)) {
            log.warn("同步im组织部门信息全量im部门数据结果为空");
            return;
        }
        List<Organization> dataList = imDepartments.stream().map(Organization::new).collect(Collectors.toList());
        // 删除部门表
        organizationMapper.delete(null);
        saveBatch(dataList);
        // 重新构建缓存
        deleteCache();
        log.info("组织部门数据看护定时任务执行完成，共更新{}条数据", dataList.size());
    }

    @Override
    public boolean reSync() {
        log.info("reSync start");
        // 删除同步标记
        LambdaQueryWrapper<CommonFlag> queryWrapper =
                new LambdaQueryWrapper<CommonFlag>().eq(CommonFlag::getFlagKey, SYNC_FLAG_KEY);
        commonFlagMapper.delete(queryWrapper);

        // 删除部门表
        organizationMapper.delete(null);

        // 清理部门缓存
        deleteCache();

//        // 刷新im的token，触发重新登录，获取虚拟账号绑定的部门数据
//        imHttpClient.deprecateToken();

        // 重新同步
        return syncFromIM();
    }

    @Override
    public void deleteCache() {
        redisUtil.del(ORGANIZATION_TREE_MAP_BY_PARENT_ID, ORGANIZATION_TREE_MAP_BY_PARENT_CODE);
    }

    @Override
    public OrganizationVO tree(Long parentId) {
        return buildTreeFromCache(Objects.nonNull(parentId) ? parentId : findRootId());
    }

    private Long findRootId() {
        ImToken token = imHttpClient.getToken();
        if (Objects.nonNull(token)) {
            ImToken.Department department = token.getDepartment();
            if (Objects.nonNull(department)) {
                return department.getDepartmentId();
            }
        }
        return DEFAULT_ROOT_PARENT_ID;
    }

    private String findRootCode() {
        ImToken token = imHttpClient.getToken();
        if (Objects.nonNull(token)) {
            ImToken.Department department = token.getDepartment();
            if (Objects.nonNull(department)) {
                return department.getDepartmentCode();
            }
        }
        return DEFAULT_ROOT_PARENT_CODE;
    }

    @Override
    public OrganizationVO tree(String parentCode) {
        return buildTreeFromCache(StringUtils.isNotBlank(parentCode) ? parentCode : findRootCode());
    }

    @Override
    public ImDepartment findOneById(Long id) {
        Organization organization = getById(id);
        return BeanCopyUtils.copyBean(organization, ImDepartment::new);
    }

    @Override
    public List<Organization> findList(List<Long> idList) {
        return listByIds(idList);
    }

    @Override
    public List<Organization> findListByCode(String code) {
        LambdaQueryWrapper<Organization> queryWrapper =
                new LambdaQueryWrapper<Organization>().eq(Organization::getCode, code);
        return list(queryWrapper);
    }

    @Override
    public List<ImDepartment> queryDepartmentForList(String parentCode) {
        if (StringUtils.isBlank(parentCode)) {
            return Collections.emptyList();
        }
        List<OrganizationVO> resultList = new ArrayList<>();
        OrganizationVO organizationVO = buildTreeFromCache(parentCode);
        recursion(organizationVO, resultList);

        return BeanCopyUtils.copyList(resultList, ImDepartment::new);
    }

    @Override
    public List<ImDepartment> queryDepartmentForListById(Long parentId) {
        if (Objects.isNull(parentId)) {
            return Collections.emptyList();
        }
        List<OrganizationVO> resultList = new ArrayList<>();
        OrganizationVO organizationVO = buildTreeFromCache(parentId);
        recursion(organizationVO, resultList);

        return BeanCopyUtils.copyList(resultList, ImDepartment::new);
    }

    private void recursion(OrganizationVO organizationVO, List<OrganizationVO> resultList) {
        if (Objects.isNull(organizationVO)) {
            return;
        }
        resultList.add(organizationVO);
        List<OrganizationVO> children = organizationVO.getChildren();
        if (CollectionUtils.isNotEmpty(children)) {
            children.forEach(child -> recursion(child, resultList));
        }
    }

    private OrganizationVO buildTreeFromCache(Long parentId) {
        OrganizationVO cache = redisUtil.hGet(ORGANIZATION_TREE_MAP_BY_PARENT_ID, parentId + "", OrganizationVO.class);
        if (cache != null) {
            return cache;
        }
        OrganizationVO matchedOrg = buildTree(parentId);
        if (Objects.nonNull(matchedOrg)) {
            redisUtil.hSet(ORGANIZATION_TREE_MAP_BY_PARENT_ID, parentId + "", matchedOrg);
        }else{
            // 如果构建树为空，说明parentCode的数据不存在，需要query一次，并重新构建缓存
            List<ImDepartment> imDepartments = imHttpClient.queryDepartmentByIds(parentId + "");
            List<Organization> dataList = imDepartments.stream().map(Organization::new).collect(Collectors.toList());
            log.info("parentId:{} 的数据不存在，需要重新同步,", parentId);
            if(CollectionUtils.isNotEmpty(dataList)){
                saveOrUpdateBatch(dataList);
                matchedOrg = buildTree(parentId);
                redisUtil.hSet(ORGANIZATION_TREE_MAP_BY_PARENT_ID, parentId + "", matchedOrg);
            }else {
                log.info("parentId:{} 查询失败，需要重新同步,", parentId);
            }
        }
        return matchedOrg;
    }

    private OrganizationVO buildTreeFromCache(String parentCode) {
        OrganizationVO cache = redisUtil.hGet(ORGANIZATION_TREE_MAP_BY_PARENT_CODE, parentCode, OrganizationVO.class);
        if (cache != null) {
            return cache;
        }
        OrganizationVO matchedOrg = buildTree(parentCode);
        if (Objects.nonNull(matchedOrg)) {
            redisUtil.hSet(ORGANIZATION_TREE_MAP_BY_PARENT_CODE, parentCode, matchedOrg);
        }else{
            // 如果构建树为空，说明parentCode的数据不存在，需要query一次，并重新构建缓存
            List<ImDepartment> imDepartments = imHttpClient.queryDepartment(parentCode);
            List<Organization> dataList = imDepartments.stream().map(Organization::new).collect(Collectors.toList());
            log.info("parentCode:{} 的数据不存在，需要重新同步,", parentCode);
            if(CollectionUtils.isNotEmpty(dataList)){
                saveOrUpdateBatch(dataList);
                matchedOrg = buildTree(parentCode);
                redisUtil.hSet(ORGANIZATION_TREE_MAP_BY_PARENT_CODE, parentCode, matchedOrg);
            }else {
                log.info("parentCode:{} 查询失败，需要重新同步,", parentCode);
            }
        }
        return matchedOrg;
    }

    /**
     * 迭代方式构建树（避免栈溢出，适合深层级）
     */
    public OrganizationVO buildTree(Long parentId) {
        List<Organization> allList = list();

        // 建立ID到实体的映射
        Map<Long, OrganizationVO> voMap = allList.stream().map(org -> {
            OrganizationVO vo = new OrganizationVO();
            BeanUtils.copyProperties(org, vo);
            return vo;
        }).collect(Collectors.toMap(OrganizationVO::getId, Function.identity()));

        // 建立父子关系（迭代设置）
        for (Organization org : allList) {
            OrganizationVO vo = voMap.get(org.getId());
            Long pid = org.getParentId();

            OrganizationVO parent = voMap.get(pid);
            if (Objects.nonNull(parent)) {
                parent.getChildren().add(vo);
            }
        }
        return voMap.get(parentId);
    }

    /**
     * 迭代方式构建树（避免栈溢出，适合深层级）
     */
    public OrganizationVO buildTree(String parentCode) {
        List<Organization> allList = list();

        // 建立ID到实体的映射
        Map<String, OrganizationVO> voMap = allList.stream().map(org -> {
            OrganizationVO vo = new OrganizationVO();
            BeanUtils.copyProperties(org, vo);
            return vo;
        }).collect(Collectors.toMap(OrganizationVO::getCode, Function.identity()));

        // 建立父子关系（迭代设置）
        for (Organization org : allList) {
            OrganizationVO vo = voMap.get(org.getCode());
            String pCode = org.getParentCode();

            OrganizationVO parent = voMap.get(pCode);
            if (Objects.nonNull(parent)) {
                parent.getChildren().add(vo);
            }
        }
        return voMap.get(parentCode);
    }

    private boolean alreadyDone() {
        LambdaQueryWrapper<CommonFlag> queryWrapper =
                new LambdaQueryWrapper<CommonFlag>().eq(CommonFlag::getFlagKey, SYNC_FLAG_KEY);
        CommonFlag commonFlag = commonFlagMapper.selectOne(queryWrapper);
        return Objects.nonNull(commonFlag) && Objects.nonNull(commonFlag.getFlagValue()) && "true".equalsIgnoreCase(
                commonFlag.getFlagValue());
    }

    private void insertFlag() {
        UserInfo user = SecurityUtils.getUser();
        // 设置全局同步标记位为已同步状态
        commonFlagMapper.insert(CommonFlag.builder().flagKey(SYNC_FLAG_KEY).flagValue("true")
                .operatorId(user == null ? null : user.getUserId()).operatorName(user == null ? null : user.getUserName())
                .build());
    }
}
