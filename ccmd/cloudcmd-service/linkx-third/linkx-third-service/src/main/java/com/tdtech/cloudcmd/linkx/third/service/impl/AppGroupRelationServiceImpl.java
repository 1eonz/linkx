package com.tdtech.cloudcmd.linkx.third.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.linkx.third.entity.AppGroupRelation;
import com.tdtech.cloudcmd.linkx.third.enums.Constants;
import com.tdtech.cloudcmd.linkx.third.mapper.AppGroupRelationMapper;
import com.tdtech.cloudcmd.linkx.third.service.AppGroupRelationService;
import com.tdtech.cloudcmd.util.DateUtils;
import com.tdtech.cloudcmd.util.IdWorker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 北向应用分组信息关联表 服务实现类
 *
 * @author wb
 * @since 2026-05-09
 */
@Service
@RequiredArgsConstructor
public class AppGroupRelationServiceImpl extends ServiceImpl<AppGroupRelationMapper, AppGroupRelation> implements AppGroupRelationService {

    private final IdWorker idWorker;

    @Override
    public AppGroupRelation create(AppGroupRelation relation) {
        relation.setId(idWorker.nextId());
        relation.setIsDeleted(Constants.VALID);
        relation.setGmtCreated(DateUtils.of(new Date()));
        save(relation);
        return relation;
    }

    @Override
    public void batchCreate(List<AppGroupRelation> relations) {
        for (AppGroupRelation relation : relations) {
            relation.setId(idWorker.nextId());
            relation.setIsDeleted(Constants.VALID);
            relation.setGmtCreated(DateUtils.of(new Date()));
        }
        saveBatch(relations);
    }

    @Override
    public void deleteById(Long id) {
        lambdaUpdate()
                .eq(AppGroupRelation::getId, id)
                .set(AppGroupRelation::getIsDeleted, Constants.DELETED)
                .update();
    }

    @Override
    public void deleteByAppIds(Long groupId, List<Long> appIds) {
        lambdaUpdate()
                .eq(AppGroupRelation::getAppGroupId, groupId)
                .in(AppGroupRelation::getAppId, appIds)
                .set(AppGroupRelation::getIsDeleted, Constants.DELETED)
                .update();
    }

    @Override
    public void deleteByAppGroupId(Long appGroupId) {
        lambdaUpdate()
                .eq(AppGroupRelation::getAppGroupId, appGroupId)
                .set(AppGroupRelation::getIsDeleted, Constants.DELETED)
                .update();
    }

    @Override
    public List<AppGroupRelation> listByAppGroupId(Long appGroupId) {
        return lambdaQuery()
                .eq(AppGroupRelation::getAppGroupId, appGroupId)
                .eq(AppGroupRelation::getIsDeleted, Constants.VALID)
                .list();
    }

    @Override
    public List<AppGroupRelation> listByAppGroupIds(List<Long> appGroupIds) {
        return lambdaQuery()
                .in(AppGroupRelation::getAppGroupId, appGroupIds)
                .eq(AppGroupRelation::getIsDeleted, Constants.VALID)
                .list();
    }

    @Override
    public List<AppGroupRelation> listByAppId(Long appId) {
        return lambdaQuery()
                .eq(AppGroupRelation::getAppId, appId)
                .eq(AppGroupRelation::getIsDeleted, 0)
                .list();
    }
}
