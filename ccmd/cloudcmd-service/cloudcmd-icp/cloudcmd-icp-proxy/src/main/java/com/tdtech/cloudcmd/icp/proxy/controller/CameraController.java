package com.tdtech.cloudcmd.icp.proxy.controller;

import com.github.yulichang.toolkit.MPJWrappers;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.icp.proxy.controller.vo.*;
import com.tdtech.cloudcmd.icp.proxy.entity.Camera;
import com.tdtech.cloudcmd.icp.proxy.entity.CameraLevel;
import com.tdtech.cloudcmd.icp.proxy.entity.IsdnType;
import com.tdtech.cloudcmd.icp.proxy.entity.OnlineStatus;
import com.tdtech.cloudcmd.icp.proxy.service.CameraService;
import com.tdtech.cloudcmd.icp.proxy.service.IcpPrivService;
import com.tdtech.cloudcmd.icp.proxy.util.SyncUtil;
import com.tdtech.cloudcmd.icp.proxy.service.IsdnTypeService;
import com.tdtech.cloudcmd.mysql.entity.CcmdPage;
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
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/proxy/icp/v1/camera")
@RequiredArgsConstructor
@Tag(name = "摄像头管理", description = "摄像头信息查询和同步接口")
public class CameraController {

    private final CameraService cameraService;
    private final IcpPrivService icpPrivService;
    private final ThreadPoolTaskExecutor asyncMessageTaskExecutor;
    private final IsdnTypeService isdnTypeService;

    @GetMapping
    @Operation(summary = "分页查询摄像头", description = "根据条件分页查询摄像头信息，包括在线状态")
    @Parameters({@Parameter(name = "pageNo", description = "页码", example = "1"),
        @Parameter(name = "pageSize", description = "每页大小", example = "10"),
        @Parameter(name = "isOnline", description = "是否在线(true/false)"),
        @Parameter(name = "cameraLevel", description = "摄像头层级"),
        @Parameter(name = "isdnTypeId", description = "设备类型id"),
        @Parameter(name = "lp", description = "左下角坐标[lat,lon]"),
        @Parameter(name = "rp", description = "右上角坐标[lat,lon]")})
    @ApiResponse(responseCode = "200", description = "查询成功",
        content = @Content(schema = @Schema(implementation = R.class)))
    public R<CcmdPage<CameraVO>> selectPage(CameraQO qo) {
        if (SyncUtil.getConnect() != null && !SyncUtil.getConnect().isEmpty() && !SyncUtil.getConnectStatus(SyncUtil.ERROR_CODE).isEmpty()) {
            return R.failure(SyncUtil.getConnectStatus(SyncUtil.ERROR_CODE));
        }

        var user = Objects.requireNonNull(SecurityUtils.getUser());
        var privs = icpPrivService.getCameraPrivByUser(user.getUserId());
        if (privs == null || privs.isEmpty()) {
            var userPrivByUser = icpPrivService.getUserPrivByUser(user.getUserId());
            if (userPrivByUser == null || userPrivByUser.isEmpty()) {
                return R.success(1, "当前登录用户无任何设备调度权限！", CcmdPage.empty(qo));
            }
            return R.success(CcmdPage.empty(qo));
        }
        if ("-1".equals(qo.getCameraLevel())) {
            return R.success(CcmdPage.empty(qo));
        }
        IsdnType isdnType = qo.getIsdnTypeId() != null ? isdnTypeService.selectById(qo.getIsdnTypeId()) : null;
        var wrapper = MPJWrappers.lambdaJoin(Camera.class).selectAll(Camera.class)
            .selectAs("IF(os.status_value is null, 4012 ,os.status_value)", CameraVO::getStatusValue)
            .selectAs("(SELECT GROUP_CONCAT(cl2.node_name ORDER BY FIND_IN_SET(cl2.level_number, REPLACE(t.level_number_list, ';', ',')) SEPARATOR ';') FROM tb_camera_level cl2 WHERE FIND_IN_SET(cl2.level_number, REPLACE(t.level_number_list, ';', ',')) > 0)", CameraVO::getLevelName)
            .leftJoin(OnlineStatus.class, "os", OnlineStatus::getIsdn, Camera::getIsdn)
            .and(qo.getSearch() != null && !qo.getSearch().isBlank(),//
                w -> w.like(Camera::getIsdn, qo.getSearch())//
                    .or()//
                    .like(Camera::getAlias, qo.getSearch())//
                    .or()//
                    .like(Camera::getName, qo.getSearch()))//
            .eq(qo.getIsOnline() != null && qo.getIsOnline(), OnlineStatus::getStatusValue, "4011")
            .ne(qo.getIsOnline() != null && !qo.getIsOnline(), OnlineStatus::getStatusValue, "4011")
            .and(qo.getCameraLevel() != null && !qo.getCameraLevel().isBlank() && !qo.getCameraLevel().equals("-1"),
                        w -> w.apply("LOCATE(CONCAT(';', {0}, ';'), CONCAT(';', level_number_list, ';')) > 0", qo.getCameraLevel()))//
            .orderByAsc(qo.getOnlineFirst() != null && qo.getOnlineFirst(), "statusValue")
            .orderByDesc(qo.getOnlineFirst() != null && !qo.getOnlineFirst(), "statusValue")
            .orderByAsc(Camera::getId)//
            .in(Camera::getLevelNumber, privs);
        if (isdnType != null) {
            wrapper = wrapper
                .eq(Camera::getCategory, isdnType.getCategory())
                .eq(Camera::getSubusercategory, isdnType.getSubusercategory())
                .eq(Camera::getApptype, isdnType.getApptype());
        }
        if (qo.getHasLocation() != null && qo.getHasLocation() == 1) {
            wrapper = wrapper.isNotNull(Camera::getLon)//
                .isNotNull(Camera::getLat);
        }
        if (qo.validLpRp()) {
            var rpArray = qo.getRpArray();
            var lpArray = qo.getLpArray();
            wrapper = wrapper.le(Camera::getLat, rpArray[0])//
                .ge(Camera::getLat, lpArray[0])//
                .le(Camera::getLon, rpArray[1])//
                .ge(Camera::getLon, lpArray[1]);
        }
        var cameraVOCcmdPage = cameraService.selectPage(qo, CameraVO.class, wrapper);
        // 获取 IsdnType 映射
        Map<String, IsdnType> isdnTypeMap = isdnTypeService.getIsdnTypeMap();

        // 转换为 UserVO 并填充 icon 和 iconUri
        List<CameraVO> voList = cameraVOCcmdPage.getRecords().stream().map(cameraVO -> {
            CameraVO vo = new CameraVO();
            BeanUtils.copyProperties(cameraVO, vo);
            String key = isdnTypeService.buildIsdnTypeKey(cameraVO.getCategory(), cameraVO.getSubusercategory(), cameraVO.getApptype());
            IsdnType type = isdnTypeMap.get(key);
            if (type != null) {
                vo.setIcon(type.getIcon());
                vo.setIconUri(type.getIconUri());
                vo.setIsShow(type.getIsShow());
                vo.setIsdnTypeId(type.getId());
            }
            return vo;
        }).collect(Collectors.toList());

        cameraVOCcmdPage.setRecords(voList);
        return R.success(cameraVOCcmdPage);
    }

    @PutMapping("/priv/{userId}")
    @Operation(summary = "设置摄像头权限", description = "为指定用户分配权限列表")
    @ApiResponse(responseCode = "200", description = "权限设置成功",
        content = @Content(schema = @Schema(implementation = R.class)))
    public R<Void> setPriv(@Parameter(name = "userId", description = "用户ID", required = true,
        example = "12345") @PathVariable Long userId, @RequestBody List<String> privs) {
        icpPrivService.upsertCameraPriv(userId, privs);
        return R.success();
    }

    @GetMapping("/priv/{userId}")
    @Operation(summary = "获取摄像头权限", description = "获取用户权限")
    @ApiResponse(responseCode = "200", description = "权限设置成功",
        content = @Content(schema = @Schema(implementation = R.class)))
    public R<Collection<String>> getPriv(@Parameter(name = "userId", description = "用户ID", required = true,
        example = "12345") @PathVariable Long userId) {
        return R.success(icpPrivService.getCameraPrivByUser(userId));
    }

    @PostMapping("/pull")
    @Operation(summary = "同步摄像头数据", description = "从源系统拉取并更新所有摄像头数据")
    @ApiResponse(responseCode = "200", description = "同步成功")
    @ApiResponse(responseCode = "500", description = "同步失败")
    public R<Void> doPull() {
        asyncMessageTaskExecutor.execute(cameraService::pull);
        return R.success();
    }

    @PutMapping("/priv/dept")
    @Operation(summary = "批量设置部门摄像头权限", description = "为指定部门下的所有用户批量设置摄像头权限")
    @ApiResponse(responseCode = "200", description = "权限设置成功")
    public R<Void> setDeptCameraPriv(@RequestBody DeptPrivQO deptCameraPrivQO) {
        icpPrivService.upsertDeptCameraPriv(deptCameraPrivQO);
        return R.success();
    }

    @GetMapping("/priv/dept/{deptCode}")
    @Operation(summary = "获取部门摄像头权限", description = "获取部门本身的摄像头权限配置")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public R<List<String>> getDeptCameraPriv(
            @Parameter(name = "deptCode", description = "部门编码", required = true)
            @PathVariable String deptCode) {
        return R.success(icpPrivService.getDeptCameraPriv(deptCode));
    }

    @PutMapping("/priv/batch")
    @Operation(summary = "批量设置用户摄像头权限", description = "为多个用户批量设置摄像头权限")
    @ApiResponse(responseCode = "200", description = "权限设置成功")
    public R<Void> setUserCameraPrivBatch(@RequestBody BatchUserPrivQO userCameraPrivQO) {
        icpPrivService.upsertUserCameraPrivBatch(userCameraPrivQO);
        return R.success();
    }

}