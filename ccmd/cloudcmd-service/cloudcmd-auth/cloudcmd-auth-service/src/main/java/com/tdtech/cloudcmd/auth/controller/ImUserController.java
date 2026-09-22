package com.tdtech.cloudcmd.auth.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.auth.controller.admin.vo.OrgPrivDto;
import com.tdtech.cloudcmd.auth.dto.ImUserCreateCO;
import com.tdtech.cloudcmd.auth.dto.ImUserDeptQO;
import com.tdtech.cloudcmd.auth.dto.ImUserQO;
import com.tdtech.cloudcmd.auth.dto.ImUserVO;
import com.tdtech.cloudcmd.auth.dto.UserImUserRelDTO;
import com.tdtech.cloudcmd.auth.entity.ImUserDO;
import com.tdtech.cloudcmd.auth.entity.Role;
import com.tdtech.cloudcmd.auth.entity.UserAdmin;
import com.tdtech.cloudcmd.auth.service.IRoleService;
import com.tdtech.cloudcmd.auth.service.ImUserService;
import com.tdtech.cloudcmd.bean.PageResult;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.msip.enums.OperationTypeEnum;
import com.tdtech.cloudcmd.msip.util.ReportUtil;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import com.tdtech.cloudcmd.util.CollectionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

import static com.tdtech.cloudcmd.enums.ResponseCodeEnum.COMMON_ERROR_111;

@Slf4j
@RestController
@RequestMapping("/auth/v1/user")
@RequiredArgsConstructor
@Tag(name = "IM用户管理", description = "IM用户相关操作接口")
public class ImUserController {

    private final ImUserService imUserService;
    private final IRoleService roleService;

    @Resource
    private ReportUtil  reportUtil;

    /**
     * 根据ID获取用户信息
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取用户信息", description = "通过用户ID查询用户详细信息")
    public R<ImUserVO> getUserById(@Parameter(name = "id", description = "用户ID", example = "1") @PathVariable Long id) {
        var byId = imUserService.getById(id);
        var imUserVO = BeanCopyUtils.copyBean(byId, ImUserVO::new);
        // 用户默认加上自己部门的数据权限
        OrgPrivDto privDto = new OrgPrivDto();
        privDto.setId(imUserVO.getDepartmentId());
        privDto.setCode(imUserVO.getDepartmentCode());
        privDto.setName(imUserVO.getDepartmentName());
        List<OrgPrivDto> orgPrivDtos = new ArrayList<>();
        orgPrivDtos.add(privDto);
        var roles = roleService.getRoleWithDataPriv(id, orgPrivDtos, true);
        Optional.ofNullable(roles).stream().flatMap(Collection::stream).findAny().ifPresent(imUserVO::setRole);
        return R.success(imUserVO);
    }

    /**
     * 分页查询用户列表
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询用户列表", description = "支持多条件分页查询用户信息")
    public R<PageResult<ImUserVO>> queryPaged(
            @Parameter(name = "pageNum", description = "页码", example = "1") @RequestParam(defaultValue = "1",
                    value = "pageNum") Long pageNum,
            @Parameter(name = "pageSize", description = "每页数量", example = "10") @RequestParam(defaultValue = "10",
                    value = "pageSize") Long pageSize,
            ImUserQO qo) {
        IPage<ImUserDO> page = new Page<>(pageNum, pageSize);
        var data = imUserService.queryPaged(page, qo);
        var records = data.getRecords();
        if (records != null && !records.isEmpty()) {
            var imUserVOS = BeanCopyUtils.copyList(records, ImUserVO::new);
            for (var imUserVO : imUserVOS) {
                var roles = roleService.getRoleWithDataPriv(imUserVO.getId(),null,true);
                Optional.ofNullable(roles).stream().flatMap(Collection::stream).findAny().ifPresent(imUserVO::setRole);
                imUserVO.setDirectLeaderId(
                        Objects.equals(imUserVO.getDirectLeaderId(), 0L) ? null : imUserVO.getDirectLeaderId());
            }
            return R.success(PageResult.fromIPage(data, imUserVOS));
        } else {
            return R.success(PageResult.fromIPage(data, Collections.emptyList()));
        }
    }

    @GetMapping("/page/dept")
    @Operation(summary = "按部门分页查询用户列表", description = "支持按部门精确查询，isChildren=0仅本部门直属人员，isChildren=1包含子孙部门人员")
    public R<PageResult<ImUserVO>> queryPagedByDept(
            @Parameter(name = "pageNum", description = "页码", example = "1") @RequestParam(defaultValue = "1",
                    value = "pageNum") Long pageNum,
            @Parameter(name = "pageSize", description = "每页数量", example = "10") @RequestParam(defaultValue = "10",
                    value = "pageSize") Long pageSize,
            ImUserDeptQO qo) {
        IPage<ImUserDO> page = new Page<>(pageNum, pageSize);
        var data = imUserService.queryPagedWithChildren(page, qo);
        var records = data.getRecords();
        if (records != null && !records.isEmpty()) {
            var imUserVOS = BeanCopyUtils.copyList(records, ImUserVO::new);
            for (var imUserVO : imUserVOS) {
                var roles = roleService.getRoleWithDataPriv(imUserVO.getId(),null,true);
                Optional.ofNullable(roles).stream().flatMap(Collection::stream).findAny().ifPresent(imUserVO::setRole);
                imUserVO.setDirectLeaderId(
                        Objects.equals(imUserVO.getDirectLeaderId(), 0L) ? null : imUserVO.getDirectLeaderId());
            }
            return R.success(PageResult.fromIPage(data, imUserVOS));
        } else {
            return R.success(PageResult.fromIPage(data, Collections.emptyList()));
        }
    }

    /**
     * 根据ID删除用户
     */
    @DeleteMapping()
    @Operation(summary = "根据ID删除用户", description = "通过用户ID删除用户信息")
    public R<Void> deleteImUserById(
            @Parameter(name = "id", description = "用户ID,逗号分割", example = "1") @RequestParam("ids") String idStr) {
        var ids = Arrays.stream(idStr.split(",")).filter(id -> !id.isBlank()).map(Long::parseLong)
                .collect(Collectors.toList());
        List<ImUserDO> userList = imUserService.getByIdList(ids);
        if (CollectionUtils.isEmpty(userList)) {
            reportUtil.saveOperationLog(OperationTypeEnum.USER_DELETE, "删除用户：[ids=" + ids + "]不存在");
            return R.failure("用户不存在");
        }
        imUserService.deleteImUser(userList);
        return R.success();
    }

    @PostMapping
    @Operation(summary = "新增用户信息", description = "不填密码就用默认密码")
    public R<Void> addImUsers(@Parameter(name = "imUserDO", description = "用户信息") @RequestBody ImUserCreateCO imUserDOs)
            throws NoSuchAlgorithmException {
        imUserService.addImUsers(imUserDOs.getRoleId(), imUserDOs.getImUsers());
        return R.success();
    }

    @Operation(summary = "修改状态", description = "锁定状态：1 是 0 否")
    @PutMapping("/{userId}/status/{status}")
    public R<Void> changeStatus(@PathVariable("userId") Long userId,
                                @Parameter(description = "锁定状态：1 是 0 否") @PathVariable("status") Integer status) {
        ImUserDO old = imUserService.getById(userId);
        if (Objects.isNull(old)) {
            return R.failure("用户不存在");
        }
        old.setStatus(status);
        if (0 == status) {
            // 启用
            imUserService.enable(old);
        } else if (1 == status){
            // 禁用
            imUserService.disable(old);
        }
        return R.success();
    }

    @Operation(summary = "获取用户角色列表", description = "根据用户ID获取该用户拥有的所有角色信息")
    @PutMapping("/{userId}/role")
    public R<Role> getRoleByUser(@PathVariable("userId") Long userId) {
        return R.success(Optional.ofNullable(roleService.getRoleWithDataPriv(userId, null, true)).stream()
                .flatMap(Collection::stream).findAny().orElse(null));
    }

    @Operation(summary = "为用户分配角色", description = "为指定用户分配角色")
    @PutMapping("/{userId}/role/{roleId}")
    public R<Void> setRolebyUser(@PathVariable("userId") Long userId, @PathVariable("roleId") Long roleId) {
        ImUserDO userInfo = imUserService.getById(userId);
        if (Objects.isNull(userInfo)) {
            reportUtil.saveOperationLog(OperationTypeEnum.PERMISSION_UPDATE, "为用户分配角色：[userId=" + userId + "]不存在");
            return R.failure("用户不存在");
        }
        roleService.updateUserRole(userInfo, roleId);
        imUserService.updateGmtUpdateForRole(List.of(userId));
        return R.success();
    }

    @Operation(summary = "修改用户密码", description = "修改指定用户的登录密码")
    @PutMapping("/{userId}/pwd")
    public R<Void> setPassword(@PathVariable("userId") Long userId, @RequestBody ImUserDO imUserDO)
            throws NoSuchAlgorithmException {
        ImUserDO user = imUserService.getById(userId);
        if (Objects.isNull(user)) {
            throw new BusinessException(COMMON_ERROR_111.getCode(), COMMON_ERROR_111.getMsg());
        }
        imUserDO.setName(user.getName());
        imUserService.changePwd(imUserDO);
        return R.success();
    }

    @Operation(summary = "新增管理员用户")
    @PostMapping("/adminuser")
    public R<Void> saveAdmin(@RequestBody ImUserVO imUserVO) throws NoSuchAlgorithmException {
        imUserService.saveAdmin(imUserVO);
        return R.success();
    }

    @GetMapping("/adminuser/page")
    @Operation(summary = "分页查询新增的admin用户列表", description = "支持多条件分页查询用户信息")
    public R<IPage<ImUserDO>> queryAdmin(
            @Parameter(name = "pageNum", description = "页码", example = "1") @RequestParam(defaultValue = "1",
                    value = "pageNum") Long pageNum,
            @Parameter(name = "pageSize", description = "每页数量", example = "10") @RequestParam(defaultValue = "10",
                    value = "pageSize") Long pageSize,
            ImUserQO qo) {
        IPage<ImUserDO> page = new Page<>(pageNum, pageSize);
        var data = imUserService.queryAdmin(page, qo);
        return R.success(data);
    }
    @GetMapping("/adminuser/{id}")
    @Operation(summary = "查询单个用户详情", description = "支持多条件分页查询用户信息")
    public R<ImUserVO> queryAdminByid(@PathVariable("id") Long id) {
        ImUserVO data = imUserService.queryAdminById(id);
        return R.success(data);
    }

    @PutMapping("/adminuser")
    @Operation(summary = "修改用户")
    public R<Boolean> queryAdminByid(@RequestBody ImUserVO imUserVO) {
        imUserService.updateAdminUser(imUserVO);
        return R.success(true);
    }

    @GetMapping("/adminuser/bind/im-user")
    @Operation(summary = "获取管理员用户和警信用户的绑定关系")
    public R<UserAdmin> getBindAdminImUser(@RequestParam(required = false) Long userId) {
        return R.success(imUserService.getBindAdminImUser(userId));
    }

    @PutMapping("/adminuser/bind/im-user")
    @Operation(summary = "绑定管理员用户和警信用户")
    public R<Boolean> bindAdminImUser(@RequestBody UserImUserRelDTO userImUserRelDTO) {
        imUserService.bindAdminImUser(userImUserRelDTO);
        return R.success(true);
    }

    @DeleteMapping("/adminuser/im-user")
    @Operation(summary = "删除管理员用户和警信用户的绑定关系")
    public R<Boolean> deleteAdminImUserRelation(@RequestBody UserImUserRelDTO userImUserRelDTO) {
        imUserService.deleteAdminImUserRelation(userImUserRelDTO);
        return R.success(true);
    }

    @DeleteMapping("/adminuser/{id}")
    @Operation(summary = "删除用户", description = "支持多条件分页查询用户信息")
    public R<Boolean> deleteAdminByid(@PathVariable("id") Long id) {
        imUserService.deleteAdminByid(id);
        return R.success(true);
    }
}