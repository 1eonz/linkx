package com.tdtech.cloudcmd.auth.service.impl;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.tdtech.cloudcmd.auth.controller.admin.vo.OrgPrivMigrateFailedVO;
import com.tdtech.cloudcmd.auth.controller.admin.vo.OrgPrivMigrateReqVO;
import com.tdtech.cloudcmd.auth.controller.admin.vo.OrgPrivMigrateResultVO;
import com.tdtech.cloudcmd.auth.entity.OrganizationRole;
import com.tdtech.cloudcmd.auth.entity.Role;
import com.tdtech.cloudcmd.auth.entity.RoleDataPriv;
import com.tdtech.cloudcmd.auth.mapper.IcpRoleMapper;
import com.tdtech.cloudcmd.auth.mapper.CollabsOrganizationMapper;
import com.tdtech.cloudcmd.auth.mapper.OrganizationRoleMapper;
import com.tdtech.cloudcmd.auth.service.OrgPrivMigrationService;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.IdWorker;
import com.tdtech.cloudcmd.util.json.JsonArray;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrgPrivMigrationServiceImpl implements OrgPrivMigrationService {

    @Resource
    private IcpRoleMapper icpRoleMapper;

    @Resource
    private OrganizationRoleMapper organizationRoleMapper;

    @Resource
    private CollabsOrganizationMapper collabsOrganizationMapper;

    @Autowired
    private IdWorker idWorker;

    @Override
    public OrgPrivMigrateResultVO executeMigration(OrgPrivMigrateReqVO reqVO) {
        LocalDateTime startTime = LocalDateTime.now();
        OrgPrivMigrateResultVO result = new OrgPrivMigrateResultVO();
        List<OrgPrivMigrateFailedVO> failedList = new ArrayList<>();
        AtomicInteger totalRelations = new AtomicInteger(0);
        AtomicInteger migratedCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);

        if (reqVO == null) {
            reqVO = new OrgPrivMigrateReqVO();
        }
        if (reqVO.getBatchSize() == null || reqVO.getBatchSize() <= 0) {
            reqVO.setBatchSize(100);
        }

        log.info("========== 开始组织权限数据迁移 ========== ");
        log.info("参数：batchSize={}", reqVO.getBatchSize());

        try {
            List<Role> allRoles = icpRoleMapper.selectRolesWithOrgPriv();

            result.setTotalRoles(allRoles.size());
            log.info("共发现 {} 个角色需要迁移", allRoles.size());

            ListUtils.partition(allRoles, reqVO.getBatchSize()).forEach(batch -> {
                batch.forEach(role -> {
                    try {
                        Date date = new Date();
                        List<Long> orgIds = parseOrgIdsFromJson(role.getImOrgPrivJson());
                        if (CollectionUtils.isNotEmpty(orgIds)) {
                            organizationRoleMapper.deleteByRoleId(role.getId());
                            List<OrganizationRole> orgRoles = orgIds.stream()
                                    .map(orgId -> {
                                        OrganizationRole organizationRole = new OrganizationRole();
                                        organizationRole.setId(idWorker.nextId());
                                        organizationRole.setRoleId(role.getId());
                                        organizationRole.setImOrgId(orgId);
                                        organizationRole.setGrantUserId(getGrantUserId());
                                        organizationRole.setGrantTime(date);
                                        organizationRole.setGmtCreated(date);
                                        organizationRole.setGmtModified(date);
                                        return organizationRole;
                                    }).collect(Collectors.toList());
                            List<List<OrganizationRole>> partitionList = Lists.partition(orgRoles, 200);
                            partitionList.forEach(organizationRoleMapper::insertBatch);
                            totalRelations.addAndGet(orgIds.size());
                        }
                        migratedCount.incrementAndGet();
                    } catch (Exception e) {
                        log.error("角色{}迁移失败: {}", role.getId(), e.getMessage(), e);
                        failedCount.incrementAndGet();
                        failedList.add(new OrgPrivMigrateFailedVO(
                                role.getId(), role.getName(), e.getMessage()
                        ));
                    }
                });

                log.info("已处理 {}/{} 个角色", migratedCount.get(), allRoles.size());
            });

        } catch (Exception e) {
            log.error("数据迁移发生严重异常", e);
            throw new RuntimeException("数据迁移失败: " + e.getMessage(), e);
        }

        Duration duration = Duration.between(startTime, LocalDateTime.now());
        result.setMigratedRoles(migratedCount.get());
        result.setFailedRoles(failedCount.get());
        result.setTotalRelations(totalRelations.get());
        result.setDuration(formatDuration(duration));
        result.setFailedList(failedList);

        log.info("========== 数据迁移完成 ========== ");
        log.info("统计：总角色={}, 成功={}, 失败={}, 关联数={}, 耗时={}",
                result.getTotalRoles(), result.getMigratedRoles(),
                result.getFailedRoles(), result.getTotalRelations(),
                result.getDuration());

        return result;
    }

    private List<Long> parseOrgIdsFromJson(JsonArray jsonArray) {
        if (jsonArray == null || jsonArray.isEmpty()) {
            return Collections.emptyList();
        }
        List<RoleDataPriv> privs = JSON.parseArray(JSON.toJSONString(jsonArray), RoleDataPriv.class);
        Set<Long> orgIds = new HashSet<>();
        extractIdsRecursively(privs, orgIds);

        return new ArrayList<>(orgIds);
    }

    private void extractIdsRecursively(List<RoleDataPriv> nodes, Set<Long> ids) {
        if (CollectionUtils.isEmpty(nodes)) {
            return;
        }
        for (RoleDataPriv node : nodes) {
            ids.add(node.getId());
            if (CollectionUtils.isNotEmpty(node.getChildren())) {
                extractIdsRecursively(node.getChildren(), ids);
            }
        }
    }

    private Long getGrantUserId() {
        // 默认超级管理员ID
        return 1L;
    }

    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}