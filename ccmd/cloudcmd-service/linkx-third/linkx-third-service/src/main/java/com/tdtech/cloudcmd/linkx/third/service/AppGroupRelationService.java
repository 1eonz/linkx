package com.tdtech.cloudcmd.linkx.third.service;

import com.tdtech.cloudcmd.linkx.third.entity.AppGroupRelation;

import java.util.List;

/**
 * 北向应用分组信息关联表 服务类
 *
 * @author wb
 * @since 2026-05-09
 */
public interface AppGroupRelationService {

    /**
     * 创建关联
     */
    AppGroupRelation create(AppGroupRelation relation);

    /**
     * 批量创建关联
     */
    void batchCreate(List<AppGroupRelation> relations);

    /**
     * 删除关联
     */
    void deleteById(Long id);

    void deleteByAppIds(Long groupId, List<Long> appIds);

    /**
     * 根据分组ID删除所有关联
     */
    void deleteByAppGroupId(Long appGroupId);

    /**
     * 根据分组ID列表查询关联列表
     */
    List<AppGroupRelation> listByAppGroupIds(List<Long> appGroupIds);

    /**
     * 根据分组ID查询关联列表
     */
    List<AppGroupRelation> listByAppGroupId(Long appGroupId);

    /**
     * 根据应用ID查询关联列表
     */
    List<AppGroupRelation> listByAppId(Long appId);
}
