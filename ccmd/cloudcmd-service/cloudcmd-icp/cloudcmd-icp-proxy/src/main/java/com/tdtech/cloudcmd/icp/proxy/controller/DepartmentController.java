package com.tdtech.cloudcmd.icp.proxy.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.icp.proxy.controller.vo.DepartmentQO;
import com.tdtech.cloudcmd.icp.proxy.entity.Department;
import com.tdtech.cloudcmd.icp.proxy.service.AuthService;
import com.tdtech.cloudcmd.icp.proxy.service.DepartmentService;
import com.tdtech.cloudcmd.icp.proxy.service.IcpPrivService;
import com.tdtech.cloudcmd.icp.proxy.util.SyncUtil;
import com.tdtech.cloudcmd.mysql.entity.CcmdPage;
import com.tdtech.cloudcmd.util.StringUtils;
import com.tdtech.cloudcmd.util.TreeUtil;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/proxy/icp/v1/department")
@RequiredArgsConstructor
@Tag(name = "部门管理", description = "部门信息查询接口")
public class DepartmentController {

    private final DepartmentService departmentService;
    private final AuthService authService;
    private final ThreadPoolTaskExecutor asyncMessageTaskExecutor;
    private final IcpPrivService icpPrivService;

    @GetMapping
    @Operation(summary = "分页查询部门", description = "根据条件分页查询部门信息")
    @Parameters({@Parameter(name = "pageNo", description = "页码", example = "1"),
        @Parameter(name = "pageSize", description = "每页大小", example = "10"),
        @Parameter(name = "upperDepartmentId", description = "上级部门ID"),
        @Parameter(name = "rootDepartmentId", description = "根部门ID")})
    @ApiResponse(responseCode = "200", description = "查询成功",
        content = @Content(schema = @Schema(implementation = R.class)))
    public R<CcmdPage<Department>> selectPage(DepartmentQO qo) {
        Department root = null;
        if (qo.getRootDepartmentId() != null && !qo.getRootDepartmentId().isBlank()) {
            root = departmentService.selectOne(
                Wrappers.lambdaQuery(Department.class).eq(Department::getUpperdepartmentId, qo.getRootDepartmentId()));
            if (root == null) {
                return R.success();
            }
        } else {
            var loginUser = authService.getLoginUser();
            if (loginUser.getDepartmentid() != null) {
                root = departmentService.selectOne(Wrappers.lambdaQuery(Department.class)
                    .eq(Department::getUpperdepartmentId, loginUser.getDepartmentid()));
            }
        }
        var page = departmentService.selectPage(qo, Wrappers.lambdaQuery(Department.class)
            .eq(qo.getUpperDepartmentId() != null && !qo.getUpperDepartmentId().isBlank(),
                Department::getUpperdepartmentId, qo.getUpperDepartmentId())
            .likeRight(root != null, Department::getDepartmentIdPath,
                Optional.ofNullable(root).map(Department::getDepartmentIdPath).orElse("")));
        return R.success(page);
    }

    @GetMapping("/tree")
    @Operation(summary = "查询部门树", description = "根据条件查询部门树结构")
    @Parameters({@Parameter(name = "rootDepartmentId", description = "根部门ID", example = "1001"),
        @Parameter(name = "upperDepartmentId", description = "上级部门ID", example = "2001")})
    @ApiResponse(responseCode = "200", description = "查询成功",
        content = @Content(schema = @Schema(implementation = R.class)))
    public R<List<TreeUtil.TreeNode<Department>>> selectTree(
        @Schema(description = "上级部门ID", example = "upper_dept001") @RequestParam(value = "upperDepartmentId",
            required = false) String upperDepartmentId,
        @Schema(description = "根部门ID", example = "root_dept001") @RequestParam(value = "rootDepartmentId",
            required = false) String rootDepartmentId) {
        Department root = null;
        if (rootDepartmentId != null && !rootDepartmentId.isBlank()) {
            root = departmentService.selectOne(
                Wrappers.lambdaQuery(Department.class).eq(Department::getUpperdepartmentId, rootDepartmentId));
            if (root == null) {
                return R.success();
            }
        }

        var list = departmentService.selectAll(Wrappers.lambdaQuery(Department.class)
            .eq(upperDepartmentId != null && !upperDepartmentId.isBlank(), Department::getUpperdepartmentId,
                upperDepartmentId).likeRight(root != null, Department::getDepartmentIdPath,
                Optional.ofNullable(root).map(Department::getDepartmentIdPath).orElse("")));
        var tree = TreeUtil.buildTreeOptimized(
            list, 
            Department::getDepartmentid,
            Department::getUpperdepartmentId
        );
        return R.success(tree);
    }

    @GetMapping("/tree/privs")
    @Operation(summary = "查询带权限的部门树", description = "根据条件查询部门树结构")
    @Parameters({@Parameter(name = "rootDepartmentId", description = "根部门ID", example = "1001"),
            @Parameter(name = "upperDepartmentId", description = "上级部门ID", example = "2001")})
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(schema = @Schema(implementation = R.class)))
    public R<List<TreeUtil.TreeNode<Department>>> selectPrivsTree(
            @Schema(description = "上级部门ID", example = "upper_dept001") @RequestParam(value = "upperDepartmentId",
                    required = false) String upperDepartmentId,
            @Schema(description = "根部门ID", example = "root_dept001") @RequestParam(value = "rootDepartmentId",
                    required = false) String rootDepartmentId) {

        // 指定根部门时，按根部门路径查询
        if (rootDepartmentId != null && !rootDepartmentId.isBlank()) {
            Department root = departmentService.selectOne(
                    Wrappers.lambdaQuery(Department.class).eq(Department::getDepartmentid, rootDepartmentId));
            if (root == null) {
                return R.success();
            }
            List<Department> list = departmentService.selectAll(Wrappers.lambdaQuery(Department.class)
                    .eq(StringUtils.isNotBlank(upperDepartmentId), Department::getUpperdepartmentId, upperDepartmentId)
                    .likeRight(true, Department::getDepartmentIdPath, root.getDepartmentIdPath()));
            return R.success(buildTree(list));
        }

        // 未指定根部门时，按用户权限查询
        var user = SecurityUtils.getUser();
        if (user == null) {
            log.warn("User not found in security context when selecting privs tree");
            return R.success(Collections.emptyList());
        }

        var privs = icpPrivService.getUserPrivByUser(user.getUserId());
        if (privs == null || privs.isEmpty()) {
            return R.success(Collections.emptyList());
        }

        LambdaQueryWrapper<Department> wrapper = Wrappers.lambdaQuery(Department.class)
                .in(Department::getDepartmentid, privs)
                .eq(StringUtils.isNotBlank(upperDepartmentId), Department::getUpperdepartmentId, upperDepartmentId);
        List<Department> privsDepts = departmentService.selectAll(wrapper);
        return R.success(buildTree(privsDepts));
    }

    private List<TreeUtil.TreeNode<Department>> buildTree(List<Department> departments) {
        return TreeUtil.buildTreeOptimized(departments, Department::getDepartmentid, Department::getUpperdepartmentId);
    }

    @PostMapping("/pull")
    @Operation(summary = "同步部门", description = "同步部门")
    @ApiResponse(responseCode = "200", description = "同步成功")
    @ApiResponse(responseCode = "500", description = "同步失败")
    public R<Void> doPull() {
        asyncMessageTaskExecutor.execute(departmentService::pull);
        return R.success();
    }

    @GetMapping("/tree/select")
    @Operation(summary = "查询部门树", description = "查询部门树结构")
    @Parameters({@Parameter(name = "rootDepartmentId", description = "根部门ID", example = "1001"),
            @Parameter(name = "upperDepartmentId", description = "上级部门ID", example = "2001")})
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(schema = @Schema(implementation = R.class)))
    public R<List<TreeUtil.TreeNode<Department>>> selectAllTree() {
        List<Department> departmentList = SyncUtil.getDepartmentList();
        var tree = TreeUtil.buildTreeOptimized(
            departmentList, 
            Department::getDepartmentid,
            Department::getUpperdepartmentId
        );
        return R.success(tree);
    }

}
