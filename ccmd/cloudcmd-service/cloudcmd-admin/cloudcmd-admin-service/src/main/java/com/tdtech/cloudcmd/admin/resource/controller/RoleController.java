package com.tdtech.cloudcmd.admin.resource.controller;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tdtech.cloudcmd.admin.annotation.OperationAnnotation;
import com.tdtech.cloudcmd.admin.resource.entity.Menu;
import com.tdtech.cloudcmd.admin.resource.entity.Role;
import com.tdtech.cloudcmd.admin.resource.entity.vo.AdminRoleVO;
import com.tdtech.cloudcmd.admin.resource.entity.co.RoleCreateCO;
import com.tdtech.cloudcmd.admin.resource.entity.co.RoleUpdateCO;
import com.tdtech.cloudcmd.admin.resource.entity.qo.RoleQO;
import com.tdtech.cloudcmd.admin.resource.service.IRoleService;
import com.tdtech.cloudcmd.auth.service.RoleRpcService;
import com.tdtech.cloudcmd.bean.PageResult;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.enums.ResponseCodeEnum;
import com.tdtech.cloudcmd.i18n.I18nUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * 角色信息表 前端控制器
 * </p>
 *
 * @author mWX556161
 * @since 2020-07-27
 */
@Tag(name = "角色")
@RestController
@RequestMapping("/admin/v1/role")
@Slf4j
public class RoleController {
    @Autowired
    private IRoleService roleService;

    @DubboReference
    private RoleRpcService roleRpcService;

    @Operation(description = "角色列表")
    @GetMapping("")
    public R<PageResult<AdminRoleVO>> getRoleList(@RequestParam(value = "name", required = false) String name,
        @RequestParam("pageSize") Integer pageSize, @RequestParam("pageNum") Integer pageNum) {
        PageResult<AdminRoleVO> data = roleService.listRole(new RoleQO(name, pageSize, pageNum));
        return R.success(data);
    }

    @Operation(description = "修改角色")
    @PutMapping("")
    @OperationAnnotation(value = "OPERATION_ANNOTATION_31",
        bodyKeyValue = "com.tdtech.cloudcmd.admin.resource.entity.co.RoleUpdateCO")
    public R<Void> updateRole(@Validated @RequestBody RoleUpdateCO roleDto) throws IOException {
        Integer newStatus = roleDto.getStatus();
        Role old = roleService.findById(roleDto.getId());
        // 如果是禁用或者启用操作
        if (Objects.nonNull(newStatus)) {
            old.setStatus(newStatus);
            if (1 == newStatus) {
                // 禁用
                roleService.disableRole(old);
            } else if (0 == newStatus) {
                // 启用
                roleService.enableRole(old);
            }
        } else {
            roleService.updateRole(roleDto, old);
        }
        roleRpcService.createRoleMenuCache();
        UserInfo user = SecurityUtils.getUser();
        roleRpcService.saveOrgPriv(roleDto.getId(), user == null ? null : user.getUserId(), roleDto.getOrgPrivList());
        return R.success(ResponseCodeEnum.SUCCESS.getCode(), I18nUtil.get(ResponseCodeEnum.SUCCESS.getMsg()));
    }

    @Operation(description = "创建角色")
    @PostMapping("")
    @OperationAnnotation(value = "OPERATION_ANNOTATION_32",
        bodyKeyValue = "com.tdtech.cloudcmd.admin.resource.entity.co.RoleCreateCO")
    public R<Void> crateRole(@Validated @RequestBody RoleCreateCO roleDto) {
        Role role = roleService.createRole(roleDto);
        roleRpcService.createRoleMenuCache();
        if (role != null && roleDto.getOrgPrivList() != null && !roleDto.getOrgPrivList().isEmpty()) {
            UserInfo user = SecurityUtils.getUser();
            roleRpcService.saveOrgPriv(role.getId(), user == null ? null : user.getUserId(), roleDto.getOrgPrivList());
        }
        return R.success(ResponseCodeEnum.SUCCESS.getCode(), I18nUtil.get(ResponseCodeEnum.SUCCESS.getMsg()));
    }

    @Operation(description = "删除角色")
    @PostMapping("/deleteBatch")
    @OperationAnnotation(value = "OPERATION_ANNOTATION_34", bodyKeyValue = "java.util.ArrayList")
    public R<Void> deleteRole(@RequestBody List<Long> ids) {
        List<Role> list = roleService.findList(ids);
        roleService.deleteRoleBatch(list);
        roleRpcService.createRoleMenuCache();
        roleRpcService.deleteOrgPriv(ids);
        return R.success(ResponseCodeEnum.SUCCESS.getCode(), I18nUtil.get(ResponseCodeEnum.SUCCESS.getMsg()));
    }

    @GetMapping("/h5permissions")
    public R<List<Menu>> h5Permissions(@RequestParam("id-card-num") String idCardNum) {
        return R.success(roleService.h5Permissions(idCardNum));
    }
}