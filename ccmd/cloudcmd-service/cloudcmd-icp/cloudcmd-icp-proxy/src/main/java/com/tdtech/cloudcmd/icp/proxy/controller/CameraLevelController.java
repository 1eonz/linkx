package com.tdtech.cloudcmd.icp.proxy.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.icp.proxy.controller.vo.CameraLevelQO;
import com.tdtech.cloudcmd.icp.proxy.entity.CameraLevel;
import com.tdtech.cloudcmd.icp.proxy.service.CameraLevelService;
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/proxy/icp/v1/camera-level")
@RequiredArgsConstructor
@Tag(name = "摄像头层级管理", description = "摄像头层级信息查询接口")
public class CameraLevelController {

    private final CameraLevelService cameraLevelService;
    private final ThreadPoolTaskExecutor asyncMessageTaskExecutor;
    private final IcpPrivService icpPrivService;

    @GetMapping
    @Operation(summary = "分页查询摄像头层级", description = "根据条件分页查询摄像头层级信息")
    @Parameters({@Parameter(name = "pageNo", description = "页码", example = "1"),
            @Parameter(name = "pageSize", description = "每页大小", example = "10"),
            @Parameter(name = "higherLevelNumber", description = "上级编号"),
            @Parameter(name = "rootLevelNumber", description = "根级编号")})
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(schema = @Schema(implementation = R.class)))
    public R<CcmdPage<CameraLevel>> selectPage(CameraLevelQO qo) {
        CameraLevel root = null;
        if (qo.getRootLevelNumber() != null && !qo.getRootLevelNumber().isBlank()) {
            root = cameraLevelService.selectone(
                    Wrappers.lambdaQuery(CameraLevel.class).eq(CameraLevel::getLevelNumber, qo.getRootLevelNumber()));
            if (root == null) {
                return R.success();
            }
        }
        var page = cameraLevelService.selectPage(qo, Wrappers.lambdaQuery(CameraLevel.class)
                .eq(qo.getHigherLevelNumber() != null && !qo.getHigherLevelNumber().isBlank(),
                        CameraLevel::getHighLevelNumber, qo.getHigherLevelNumber())
                .likeRight(root != null, CameraLevel::getLevelNumberPath,
                        Optional.ofNullable(root).map(CameraLevel::getLevelNumberPath).orElse("")));
        return R.success(page);
    }

    @GetMapping("/tree")
    @Operation(summary = "查询摄像头层级树")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(schema = @Schema(implementation = R.class)))
    public R<List<TreeUtil.TreeNode<CameraLevel>>> selectTree(
            @Parameter(name = "higherLevelNumber", description = "上级编号") @RequestParam(value = "higherLevelNumber",
                    required = false) String higherLevelNumber,
            @Parameter(name = "rootLevelNumber", description = "根级编号") @RequestParam(value = "rootLevelNumber",
                    required = false) String rootLevelNumber) {
        CameraLevel root = null;
        if (rootLevelNumber != null && !rootLevelNumber.isBlank()) {
            root = cameraLevelService.selectone(
                    Wrappers.lambdaQuery(CameraLevel.class).eq(CameraLevel::getLevelNumber, rootLevelNumber));
            if (root == null) {
                return R.success();
            }
        }
        if (StringUtils.isBlank(higherLevelNumber)) {
            List<TreeUtil.TreeNode<CameraLevel>> all = cameraLevelService.getCameraLevelTree();
            return R.success(all);
        } else {
            var all = cameraLevelService.selectAll(Wrappers.lambdaQuery(CameraLevel.class)
                    .eq(!higherLevelNumber.isBlank(), CameraLevel::getHighLevelNumber,
                            higherLevelNumber).likeRight(root != null, CameraLevel::getLevelNumberPath,
                            Optional.ofNullable(root).map(CameraLevel::getLevelNumberPath).orElse("")));
            var tree = TreeUtil.buildTreeOptimized(
                all, 
                CameraLevel::getLevelNumber,
                CameraLevel::getHighLevelNumber
            );
            return R.success(tree);
        }

    }

    @GetMapping("/tree/privs")
    @Operation(summary = "查询带有权限的摄像头层级树")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(schema = @Schema(implementation = R.class)))
    public R<List<TreeUtil.TreeNode<CameraLevel>>> selectPrivsTree(
            @Parameter(name = "higherLevelNumber", description = "上级编号") @RequestParam(value = "higherLevelNumber",
                    required = false) String higherLevelNumber,
            @Parameter(name = "rootLevelNumber", description = "根级编号") @RequestParam(value = "rootLevelNumber",
                    required = false) String rootLevelNumber) {
        CameraLevel root = null;
        if (rootLevelNumber != null && !rootLevelNumber.isBlank()) {
            root = cameraLevelService.selectone(
                    Wrappers.lambdaQuery(CameraLevel.class).eq(CameraLevel::getLevelNumber, rootLevelNumber));
            if (root == null) {
                return R.success();
            }
        }
        if (StringUtils.isBlank(higherLevelNumber)) {
            var user = SecurityUtils.getUser();
            if (user == null) {
                log.warn("User not found in security context when selecting privs tree");
                return R.success(Collections.emptyList());
            }
            var privs = icpPrivService.getCameraPrivByUser(user.getUserId());
            if (privs == null || privs.isEmpty()) {
                return R.success(Collections.emptyList());
            }

            Set<String> privSet = new HashSet<>(privs);
            List<CameraLevel> privsCameraLevels = cameraLevelService.selectAll(
                    new LambdaQueryWrapper<CameraLevel>()
                            .in(CameraLevel::getLevelNumber, privSet)
            );
            var tree = TreeUtil.buildTreeOptimized(
                    privsCameraLevels,
                    CameraLevel::getLevelNumber,
                    CameraLevel::getHighLevelNumber
            );
            return R.success(tree);
        } else {
            log.info("higherLevelNumber:{}", higherLevelNumber);
            var all = cameraLevelService.selectAll(Wrappers.lambdaQuery(CameraLevel.class)
                    .eq(!higherLevelNumber.isBlank(), CameraLevel::getHighLevelNumber,
                            higherLevelNumber).likeRight(root != null, CameraLevel::getLevelNumberPath,
                            Optional.ofNullable(root).map(CameraLevel::getLevelNumberPath).orElse("")));
            var tree = TreeUtil.buildTreeOptimized(
                    all,
                    CameraLevel::getLevelNumber,
                    CameraLevel::getHighLevelNumber
            );
            return R.success(tree);
        }

    }

    @PostMapping("/pull")
    @Operation(summary = "同步层级", description = "同步层级")
    @ApiResponse(responseCode = "200", description = "同步成功")
    @ApiResponse(responseCode = "500", description = "同步失败")
    public R<Void> doPull() {
        asyncMessageTaskExecutor.execute(cameraLevelService::pull);
        return R.success();
    }

    @GetMapping("/tree/select")
    @Operation(summary = "查询摄像头层级树")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(schema = @Schema(implementation = R.class)))
    public R<List<TreeUtil.TreeNode<CameraLevel>>> selectAllTree() {
        List<CameraLevel> cameraLevelList = SyncUtil.getCameraLevelList();
        var tree = TreeUtil.buildTreeOptimized(
            cameraLevelList, 
            CameraLevel::getLevelNumber,
            CameraLevel::getHighLevelNumber
        );
        return R.success(tree);
    }
}
