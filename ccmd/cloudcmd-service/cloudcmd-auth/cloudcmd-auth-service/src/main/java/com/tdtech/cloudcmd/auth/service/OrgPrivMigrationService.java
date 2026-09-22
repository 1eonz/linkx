package com.tdtech.cloudcmd.auth.service;

import com.tdtech.cloudcmd.auth.controller.admin.vo.OrgPrivMigrateReqVO;
import com.tdtech.cloudcmd.auth.controller.admin.vo.OrgPrivMigrateResultVO;
import org.springframework.stereotype.Service;

/**
 * 组织权限迁移服务（内部实现接口）
 */
public interface OrgPrivMigrationService {

    OrgPrivMigrateResultVO executeMigration(OrgPrivMigrateReqVO reqVO);
}