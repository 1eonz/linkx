package com.tdtech.cloudcmd.linkx.third.service.impl;

import cloudcmd.service.rpc.AppInfoRpcService;
import cloudcmd.vo.AppInfoResp4RpcVO;
import cloudcmd.vo.UserCommonAppResp4RpcVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.linkx.third.api.dto.*;
import com.tdtech.cloudcmd.linkx.third.entity.AppGroup;
import com.tdtech.cloudcmd.linkx.third.entity.AppGroupRelation;
import com.tdtech.cloudcmd.linkx.third.entity.UserAppUsed;
import com.tdtech.cloudcmd.linkx.third.enums.Constants;
import com.tdtech.cloudcmd.linkx.third.enums.GroupTypeEnum;
import com.tdtech.cloudcmd.linkx.third.mapper.AppGroupMapper;
import com.tdtech.cloudcmd.linkx.third.service.AppGroupRelationService;
import com.tdtech.cloudcmd.linkx.third.service.AppGroupService;
import com.tdtech.cloudcmd.linkx.third.service.UserAppUsedService;
import com.tdtech.cloudcmd.linkx.third.utils.AssertUtils;
import com.tdtech.cloudcmd.linkx.third.vo.AppGroupDetailVo;
import com.tdtech.cloudcmd.linkx.third.api.Vo.AppUsedRankingVo;
import com.tdtech.cloudcmd.util.*;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 北向应用分组信息表 服务实现类
 *
 * @author wb
 * @since 2026-05-09
 */
@Service
@RequiredArgsConstructor
public class AppGroupServiceImpl extends ServiceImpl<AppGroupMapper, AppGroup> implements AppGroupService {

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private AppGroupRelationService appGroupRelationService;

    @Autowired
    private UserAppUsedService userAppUsedService;

    @DubboReference
    private AppInfoRpcService appInfoRpcService;

    @Override
    public Long create(AppGroupCreateDto appGroupCreateVo) {
        // 校验分组名称是否重复
        checkGroupNameExists(appGroupCreateVo.getType(), appGroupCreateVo.getName(), 
                appGroupCreateVo.getCreateUser(), null);
        
        AppGroup appGroup = BeanCopyUtils.copyBean(appGroupCreateVo, AppGroup::new);
        long id = idWorker.nextId();
        appGroup.setId(id);
        appGroup.setIsDeleted(Constants.VALID);
        appGroup.setGmtCreated(DateUtils.of(new Date()));
        save(appGroup);
        return id;
    }

    /**
     * 校验分组名称是否存在
     * @param type 分组类型
     * @param name 分组名称
     * @param userId 用户ID（用户级时必传）
     * @param excludeId 排除的分组ID（修改时排除自身）
     */
    private void checkGroupNameExists(Integer type, String name, Long userId, Long excludeId) {
        if (StringUtils.isBlank(name)) {
            throw new BusinessException("分组名称不能为空");
        }
        boolean isUserLevel = Objects.equals(GroupTypeEnum.USER.getCode(), type);
        if (isUserLevel) {
            AssertUtils.check(Objects.nonNull(userId), "用户id不能为空");
        }
        LambdaQueryWrapper<AppGroup> queryWrapper = Wrappers.lambdaQuery(AppGroup.class)
                .eq(AppGroup::getType, type)
                .eq(isUserLevel, AppGroup::getCreateUser, userId)
                .eq(AppGroup::getName, name.trim())
                .eq(AppGroup::getIsDeleted, Constants.VALID)
                .ne(excludeId != null, AppGroup::getId, excludeId);
        
        long count = count(queryWrapper);
        if (count > 0) {
            throw new BusinessException("分组名称已存在: " + name);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createContainsApp(AppGroupCreateDto appGroupCreateVo) {
        // 校验分组名称是否重复
        Long groupId = create(appGroupCreateVo);
        List<Long> appIdList = appGroupCreateVo.getAppIds();
        if (CollectionUtils.isNotEmpty(appIdList)) {
            AppToGroupDto appToGroupDto = new AppToGroupDto();
            appToGroupDto.setType(appGroupCreateVo.getType());
            appToGroupDto.setAppIds(appGroupCreateVo.getAppIds());
            appToGroupDto.setUserId(appGroupCreateVo.getCreateUser());
            appAddToGroup(groupId, appToGroupDto);
        }
    }

    @Override
    public void update(AppGroupUpdateDto updateDto) {
        // 校验分组是否存在
        AppGroup existingGroup = this.getById(updateDto.getId());
        if (Objects.isNull(existingGroup) || Objects.equals(existingGroup.getIsDeleted(), Constants.DELETED)) {
            throw new BusinessException("分组不存在");
        }
        // 校验分组名称是否重复（排除自身）
        checkGroupNameExists(updateDto.getType(), updateDto.getName(),
                updateDto.getCreateUser(), updateDto.getId());

        // 修改分组表
        AppGroup appGroup = new AppGroup();
        appGroup.setId(updateDto.getId());
        appGroup.setSort(updateDto.getSort());
        appGroup.setName(updateDto.getName());
        this.updateById(appGroup);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateContainsApp(AppGroupUpdateDto updateDto) {
        update(updateDto);
        List<Long> appIds = updateDto.getAppIds();
        // 逻辑删除分组应用关系表
        appGroupRelationService.deleteByAppGroupId(updateDto.getId());
        if (CollectionUtils.isNotEmpty(appIds)) {
            // 插入新数据
            AppToGroupDto appToGroupDto = new AppToGroupDto();
            appToGroupDto.setType(updateDto.getType());
            appToGroupDto.setAppIds(appIds);
            appToGroupDto.setUserId(updateDto.getCreateUser());
            appAddToGroup(updateDto.getId(), appToGroupDto);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSort(List<GroupSortDto> groupSortList) {
        if (CollectionUtils.isEmpty(groupSortList)) {
            return;
        }
        
        // 查询现有分组信息（排除已删除的）
        List<Long> groupIds = groupSortList.stream()
                .map(GroupSortDto::getId)
                .collect(Collectors.toList());
        LambdaQueryWrapper<AppGroup> queryWrapper = Wrappers.lambdaQuery(AppGroup.class)
                .in(AppGroup::getId, groupIds)
                .eq(AppGroup::getIsDeleted, Constants.VALID);
        List<AppGroup> appGroups = this.list(queryWrapper);
        
        // 校验所有分组ID是否都存在
        if (CollectionUtils.isEmpty(appGroups)) {
            throw new BusinessException("未查询到分组信息");
        }
        if (appGroups.size() != groupIds.size()) {
            Set<Long> foundIds = appGroups.stream().map(AppGroup::getId).collect(Collectors.toSet());
            List<String> missingNames = groupSortList.stream()
                    .filter(dto -> !foundIds.contains(dto.getId()))
                    .map(dto -> StringUtils.isNotBlank(dto.getName()) ? dto.getName() : String.valueOf(dto.getId()))
                    .collect(Collectors.toList());
            throw new BusinessException("分组不存在: " + String.join(", ", missingNames));
        }
        
        // 校验分组类型是否一致
        if (appGroups.stream().map(AppGroup::getType).distinct().count() > 1) {
            throw new BusinessException("分组类型不一致");
        }
        
        // 校验分组名称是否重复
        Integer type = appGroups.get(0).getType();
        Long userId = appGroups.get(0).getCreateUser();
        List<String> names = groupSortList.stream()
                .map(GroupSortDto::getName)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
        
        // 检查名称列表内部是否有重复
        if (names.size() > 1 && new HashSet<>(names).size() != names.size()) {
            throw new BusinessException("分组名称不能重复");
        }
        
        // 检查名称是否与数据库中其他分组重复
        for (GroupSortDto groupSortDto : groupSortList) {
            if (StringUtils.isNotBlank(groupSortDto.getName())) {
                checkGroupNameExists(type, groupSortDto.getName(), userId, groupSortDto.getId());
            }
        }
        
        // 执行更新
        groupSortList.forEach(groupSortDto -> {
            AppGroup appGroup = new AppGroup();
            appGroup.setId(groupSortDto.getId());
            appGroup.setSort(groupSortDto.getSort());
            appGroup.setName(groupSortDto.getName());
            this.updateById(appGroup);
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteGroups(Long id) {
        // 传入type主要是为了防止app用户删除系统分组
        AppGroup appGroup = this.getById(id);
        if (Objects.isNull(appGroup)) {
            throw new BusinessException("未查询到要删除的分组");
        }
        // 不允许删除分组下有应用的分组
        List<AppGroupRelation> appGroupRelations = appGroupRelationService.listByAppGroupId(id);
        if (CollectionUtils.isNotEmpty(appGroupRelations)) {
            throw new BusinessException("分组下有应用不支持删除");
        }
        // 逻辑删除分组表信息
        LambdaUpdateWrapper<AppGroup> updateWrapper = Wrappers.lambdaUpdate(AppGroup.class)
                .eq(AppGroup::getId, id)
                .set(AppGroup::getIsDeleted, Constants.DELETED);
        update(updateWrapper);
        // 逻辑删除分组应用关系表
//        appGroupRelationService.deleteByAppGroupId(id);
    }

    private void checkGroupType(Integer type) {
        boolean anyMatch = Arrays.stream(GroupTypeEnum.values()).anyMatch(it -> Objects.equals(it.getCode(), type));
        if (!anyMatch) {
            String msg = Arrays.stream(GroupTypeEnum.values()).map(it -> it.getCode() + ":" + it.getMsg()).collect(Collectors.joining(","));
            throw new BusinessException("分组类型应是如下：" + msg);
        }
    }

    @Override
    public List<AppGroupDetailVo> getGroupsDetails(Integer type, Long userId,Integer scope) {
        List<AppGroup> appGroups = listByType(type, userId);
        if (CollectionUtils.isEmpty(appGroups)) {
            return new ArrayList<>();
        }
        List<AppGroupDetailVo> detailVos = BeanCopyUtils.copyList(appGroups, AppGroupDetailVo::new);
        List<Long> groupIds = detailVos.stream().map(AppGroupDetailVo::getId).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(groupIds)) {
            return new ArrayList<>();
        }
        // 查询绑定的应用
        List<AppGroupRelation> appGroupRelations = appGroupRelationService.listByAppGroupIds(groupIds);
        List<Long> appIds = appGroupRelations.stream().map(AppGroupRelation::getAppId).collect(Collectors.toList());
        List<AppInfoResp4RpcVO> appInfoByIds = appInfoRpcService.getAppInfoByIds(appIds, scope);
        // 绑定app信息到分组里，Map<分组ID, List<应用ID>>
        Map<Long, List<Long>> groupRelationMap = appGroupRelations.stream()
                .collect(Collectors.groupingBy(
                        AppGroupRelation::getAppGroupId,
                        Collectors.mapping(AppGroupRelation::getAppId, Collectors.toList())
                ));
        for (AppGroupDetailVo detailVo : detailVos) {
            List<Long> appIdList = groupRelationMap.getOrDefault(detailVo.getId(), new ArrayList<>());
            HashSet<Long> appIdSet = new HashSet<>(appIdList);
            List<AppInfoResp4RpcVO> infoRespVOS = appInfoByIds.stream().filter(it -> appIdSet.contains(it.getId())).collect(Collectors.toList());
            detailVo.setAppList(infoRespVOS);
        }
        return detailVos;
    }

    @Override
    public List<AppGroup> listByType(Integer type, Long userId) {
        checkGroupType(type);
        boolean isUserLevel = Objects.equals(GroupTypeEnum.USER.getCode(), type);
        if (isUserLevel) {
            AssertUtils.check(Objects.nonNull(userId), "用户id不能为空");
        }
        LambdaQueryWrapper<AppGroup> queryWrapper = Wrappers.lambdaQuery(AppGroup.class)
                .eq(AppGroup::getType, type)
                .eq(isUserLevel, AppGroup::getCreateUser, userId)
                .eq(AppGroup::getIsDeleted, Constants.VALID)
                .orderByAsc(AppGroup::getSort)
                .orderByAsc(AppGroup::getGmtCreated);
        return list(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void appAddToGroup(Long groupId, AppToGroupDto appToGroupDto) {
        optAppToGroup(groupId, appToGroupDto);
        List<AppGroupRelation> groupRelations = new ArrayList<>();
        // 先删除所属分组的应用关系
        appGroupRelationService.deleteByAppGroupId(groupId);
        for (Long appId : appToGroupDto.getAppIds()) {
            AppGroupRelation groupRelation = new AppGroupRelation();
            groupRelation.setAppGroupId(groupId);
            groupRelation.setAppId(appId);
            groupRelation.setCreateUserId(appToGroupDto.getUserId());
            groupRelations.add(groupRelation);
        }
        appGroupRelationService.batchCreate(groupRelations);
    }

    @Override
    public void deleteAppFromGroup(Long groupId, AppToGroupDto appToGroupDto) {
        optAppToGroup(groupId, appToGroupDto);
        appGroupRelationService.deleteByAppIds(groupId, appToGroupDto.getAppIds());
    }

    public void optAppToGroup(Long groupId, AppToGroupDto appToGroupDto) {
        Integer type = appToGroupDto.getType();
//        List<Long> appIds = appToGroupDto.getAppIds();
        checkGroupType(type);
//        AssertUtils.check(CollectionUtils.isNotEmpty(appIds), "至少选择一个应用");
        // 校验分组id是否否符合type
        LambdaQueryWrapper<AppGroup> queryWrapper = Wrappers.lambdaQuery(AppGroup.class)
                .eq(AppGroup::getId, groupId)
                .eq(AppGroup::getIsDeleted, Constants.VALID);
        List<AppGroup> appGroups = list(queryWrapper);
        AssertUtils.check(appGroups.stream().allMatch(it -> Objects.equals(type, it.getType())), "分组级别和分组不匹配");
    }

    @Override
    public void createUsed(Long appId, AppUsedDto appUsedDto) {
        UserAppUsed userAppUsed = BeanCopyUtils.copyBean(appUsedDto, UserAppUsed::new);
        userAppUsedService.create(userAppUsed);
    }

    @Override
    public List<AppUsedRankingVo> getAppUsedRanking(Long userId, Integer terminalType, Integer scope) {
        // 查询用户使用的应用记录
        List<UserAppUsed> userAppUseds = userAppUsedService.listByUserId(userId);
        // 获取所有的应用列表
        List<AppInfoResp4RpcVO> appInfoByIds = appInfoRpcService.getAllAppInfoByIds(scope);
        if (CollectionUtils.isEmpty(appInfoByIds)) {
            return new ArrayList<>();
        }
//        List<UserCommonAppResp4RpcVo> myApps = appInfoRpcService.getMy(String.valueOf(userId), terminalType);
        List<AppUsedRankingVo> appUsedRankingVos = new ArrayList<>(appInfoByIds.size());
        for (AppInfoResp4RpcVO appInfoResp4RpcVO : appInfoByIds) {
            AppUsedRankingVo appUsedRankingVo = new AppUsedRankingVo();
            appUsedRankingVo.setSort(appInfoResp4RpcVO.getSort());
            appUsedRankingVo.setAppId(appInfoResp4RpcVO.getId());
            appUsedRankingVo.setUserId(String.valueOf(userId));
            appUsedRankingVo.setApp(appInfoResp4RpcVO);
            appUsedRankingVos.add(appUsedRankingVo);
        }
        if (CollectionUtils.isEmpty(userAppUseds)) {
            return appUsedRankingVos;
        }
        // 按应用ID分组统计使用次数
        Map<Long, Long> appCountMap = userAppUseds.stream()
                .collect(Collectors.groupingBy(UserAppUsed::getAppId, Collectors.counting()));
        // 组装结果并按使用次数降序排序
        appUsedRankingVos.forEach(it -> {
            if (Objects.nonNull(it.getAppId())) {
                it.setCount(appCountMap.getOrDefault(it.getAppId(), 0L).intValue());
            }
        });
        return appUsedRankingVos.stream().sorted(Comparator.comparing(AppUsedRankingVo::getCount).reversed().thenComparing(AppUsedRankingVo::getSort))
                .collect(Collectors.toList());
    }
}
