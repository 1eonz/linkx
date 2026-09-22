package com.tdtech.cloudcmd.linkx.third.service;

import com.tdtech.cloudcmd.linkx.third.api.dto.*;
import com.tdtech.cloudcmd.linkx.third.entity.AppGroup;
import com.tdtech.cloudcmd.linkx.third.vo.AppGroupDetailVo;
import com.tdtech.cloudcmd.linkx.third.api.Vo.AppUsedRankingVo;

import java.util.List;

/**
 * 北向应用分组信息表 服务类
 *
 * @author wb
 * @since 2026-05-09
 */
public interface AppGroupService {

    /**
     * 创建应用分组
     */
    Long create(AppGroupCreateDto appGroupCreateVo);

    /**
     * 更新分组排序
     * @param groupSortList 需要修改的分组排序list
     */
    void updateSort(List<GroupSortDto> groupSortList);

    /**
     *
     * @param id 分组id
     */
    void deleteGroups(Long id);

    /**
     * 获取应用分组列表
     * @param type 分组类型：1系统级 2用户级
     * @param userId 用户id
     */
    List<AppGroupDetailVo> getGroupsDetails(Integer type, Long userId,Integer scope);

    List<AppGroup> listByType(Integer type, Long userId);

    /**
     * 绑定应用到分组
     * @param groupId 分组id
     * @param appToGroupDto 分组信息
     */
    void appAddToGroup(Long groupId, AppToGroupDto appToGroupDto);

    /**
     * 删除分组中的应用
     * @param groupId 分组id
     * @param appToGroupDto 分组信息
     */
    void deleteAppFromGroup(Long groupId, AppToGroupDto appToGroupDto);

    /**
     * 创建应用使用记录
     * @param appId 应用id
     * @param appUsedDto 使用信息
     */
    void createUsed(Long appId, AppUsedDto appUsedDto);

    /**
     * 获取应用使用记录排行
     *
     * @param userId       用户id
     * @param terminalType
     * @return 应用使用记录排行列表
     */
    List<AppUsedRankingVo> getAppUsedRanking(Long userId, Integer terminalType, Integer scope);

    void createContainsApp(AppGroupCreateDto appGroupCreateVo);

    void updateContainsApp(AppGroupUpdateDto updateDto);

    void update(AppGroupUpdateDto updateDto);
}
