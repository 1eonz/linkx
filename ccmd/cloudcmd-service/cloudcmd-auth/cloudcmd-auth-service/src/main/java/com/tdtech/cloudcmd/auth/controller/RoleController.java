package com.tdtech.cloudcmd.auth.controller;

import com.tdtech.cloudcmd.auth.controller.admin.vo.OrgPrivMigrateReqVO;
import com.tdtech.cloudcmd.auth.controller.admin.vo.OrgPrivMigrateResultVO;
import com.tdtech.cloudcmd.auth.entity.ImUserDO;
import com.tdtech.cloudcmd.auth.entity.Role;
import com.tdtech.cloudcmd.auth.service.IRoleService;
import com.tdtech.cloudcmd.auth.service.ImUserService;
import com.tdtech.cloudcmd.auth.service.OrgPrivMigrationService;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author mWX556161
 * @date 2020/9/28 16:33
 */
@Slf4j
@RestController
@RequestMapping("/auth/v1/role")
@Tag(name = "角色管理接口", description = "提供角色相关操作接口")
public class RoleController {

    @Resource
    private IRoleService roleService;
    @Resource
    private ImUserService imUserService;
    @Resource
    private OrgPrivMigrationService orgPrivMigrationService;


    @GetMapping("/byUserId")
    public R<Role> getRoleByUserId(@RequestParam("userId")Long userId){
        var roleListByUserId = roleService.getRoleWithDataPriv(userId, null,true);
        return R.success(Optional.ofNullable(roleListByUserId).stream().flatMap(Collection::stream).findAny().orElse(null));
    }

    @PutMapping("/{roleId}/user")
    public R<Void> setUser(@PathVariable("roleId")Long roleId,@RequestBody List<Long> userIds){
        List<ImUserDO> userList = imUserService.getByIdList(userIds);
        roleService.setUsersRole(userList, roleId);
        imUserService.updateGmtUpdateForRole(userIds);
        return R.success();
    }

    @PostMapping("/migrate-org-priv")
    @Operation(summary = "触发组织权限数据迁移（从JSON字段到新表）")
    public R<OrgPrivMigrateResultVO> migrateOrgPriv(@RequestBody(required = false) OrgPrivMigrateReqVO reqVO) {
        return R.success(orgPrivMigrationService.executeMigration(reqVO));
    }
}