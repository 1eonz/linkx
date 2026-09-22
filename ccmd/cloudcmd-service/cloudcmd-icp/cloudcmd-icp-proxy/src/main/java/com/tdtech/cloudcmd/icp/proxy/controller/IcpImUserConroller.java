package com.tdtech.cloudcmd.icp.proxy.controller;

import com.github.yulichang.toolkit.MPJWrappers;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.icp.proxy.controller.vo.DeptPrivQO;
import com.tdtech.cloudcmd.icp.proxy.controller.vo.IcpImUserVO;
import com.tdtech.cloudcmd.icp.proxy.controller.vo.ImUserQO;
import com.tdtech.cloudcmd.icp.proxy.controller.vo.BatchUserPrivQO;
import com.tdtech.cloudcmd.icp.proxy.controller.vo.UserVO;
import com.tdtech.cloudcmd.icp.proxy.entity.Gis;
import com.tdtech.cloudcmd.icp.proxy.entity.IcpImUser;
import com.tdtech.cloudcmd.icp.proxy.entity.OnlineStatus;
import com.tdtech.cloudcmd.icp.proxy.entity.User;
import com.tdtech.cloudcmd.icp.proxy.service.IcpPrivService;
import com.tdtech.cloudcmd.icp.proxy.service.ImUserService;
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
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/proxy/icp/v1/imuser")
@RequiredArgsConstructor
@Tag(name = "IM用户管理")
public class IcpImUserConroller {

    private final ImUserService imUserService;
    private final IcpPrivService icpPrivService;
    private final ThreadPoolTaskExecutor asyncMessageTaskExecutor;

    @GetMapping
    @Operation(summary = "分页查询用户", description = "根据条件分页查询用户信息，包括在线状态和位置信息")
    @Parameters({@Parameter(name = "pageNo", description = "页码", example = "1"),
        @Parameter(name = "pageSize", description = "每页大小", example = "10"),
        @Parameter(name = "isOnline", description = "是否在线(true/false)"),
        @Parameter(name = "lp", description = "左下角坐标[lat,lon]"),
        @Parameter(name = "rp", description = "右上角坐标[lat,lon]")})
    @ApiResponse(responseCode = "200", description = "查询成功",
        content = @Content(schema = @Schema(implementation = R.class)))
    public R<CcmdPage<IcpImUserVO>> selectPage(ImUserQO qo) {
        var user = Objects.requireNonNull(SecurityUtils.getUser());
        var privs = icpPrivService.getUserPrivByUser(user.getUserId());
        if (privs == null || privs.isEmpty()) {
            return R.success(CcmdPage.empty(qo));
        }
        if (!privs.contains("-1")) {
            privs.add("-1");
        }
        var wrapper = MPJWrappers.lambdaJoin(IcpImUser.class).selectAll(IcpImUser.class)
            .selectAs("IF(os.status_value is null, 4012 ,os.status_value)", UserVO::getStatusValue)
            .selectAs(Gis::getLat, UserVO::getLat)//
            .selectAs(Gis::getLon, UserVO::getLon)//
            .leftJoin(OnlineStatus.class, "os", OnlineStatus::getIsdn, IcpImUser::getIsdn)
            .leftJoin(Gis.class, "gs", Gis::getIsdn, IcpImUser::getIsdn)
            .innerJoin(User.class, "us", User::getIsdn, IcpImUser::getIsdn)
            .eq(qo.getIsOnline() != null && qo.getIsOnline(), OnlineStatus::getStatusValue, "4011")
            .ne(qo.getIsOnline() != null && !qo.getIsOnline(), OnlineStatus::getStatusValue, "4011")
            .ne(User::getCategory, 10)//
            .in(User::getDepartmentid, privs);

        if (qo.getHasLocation() != null && qo.getHasLocation() == 1) {
            wrapper = wrapper.isNotNull(Gis::getLon)//
                .isNotNull(Gis::getLat);
        }
        if (qo.validLpRp()) {
            var rpArray = qo.getRpArray();
            var lpArray = qo.getLpArray();
            wrapper = wrapper.le("gs.lat", rpArray[0])//
                .ge("gs.lat", lpArray[0])//
                .le("gs.lon", rpArray[1])//
                .ge("gs.lon", lpArray[1]);
        }
        var cameraVOCcmdPage = imUserService.selectPage(qo, wrapper);
        return R.success(cameraVOCcmdPage);
    }

    @PutMapping("/priv/{userId}")
    @Operation(summary = "设置用户权限", description = "为指定用户分配权限列表")
    @ApiResponse(responseCode = "200", description = "权限设置成功",
        content = @Content(schema = @Schema(implementation = R.class)))
    public R<Void> setPriv(@Parameter(name = "userId", description = "用户ID", required = true,
        example = "12345") @PathVariable Long userId, @RequestBody List<String> privs) {
        icpPrivService.upsertUserPriv(userId, privs);
        return R.success();
    }

    @GetMapping("/priv/{userId}")
    @Operation(summary = "获取用户权限", description = "获取用户权限")
    @ApiResponse(responseCode = "200", description = "权限设置成功",
        content = @Content(schema = @Schema(implementation = R.class)))
    public R<Collection<String>> getPriv(@Parameter(name = "userId", description = "用户ID", required = true,
        example = "12345") @PathVariable Long userId) {
        return R.success( icpPrivService.getUserPrivByUser(userId));
    }

    @PostMapping("/pull")
    @Operation(summary = "同步", description = "同步")
    @ApiResponse(responseCode = "200", description = "同步成功")
    @ApiResponse(responseCode = "500", description = "同步失败")
    public R<Void> doPull() {
        asyncMessageTaskExecutor.execute(imUserService::refreshImUser);
        return R.success();
    }

    @PutMapping("/priv/dept")
    @Operation(summary = "设置部门权限", description = "批量设置部门权限")
    @ApiResponse(responseCode = "200", description = "推送成功")
    public R<Void> setDeptPriv(@RequestBody DeptPrivQO deptPrivQO){
        icpPrivService.upsertDeptUserPriv(deptPrivQO);
        return R.success();
    }

    @GetMapping("/priv/dept/{deptCode}")
    @Operation(summary = "获取部门设备权限", description = "获取部门本身的设备权限配置")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public R<List<String>> getDeptPriv(
            @Parameter(name = "deptCode", description = "部门编码", required = true)
            @PathVariable String deptCode) {
        return R.success(icpPrivService.getDeptUserPriv(deptCode));
    }

    @PutMapping("/priv/batch")
    @Operation(summary = "批量设置用户权限", description = "为多个用户批量设置设备权限")
    @ApiResponse(responseCode = "200", description = "权限设置成功")
    public R<Void> setUserPrivBatch(@RequestBody BatchUserPrivQO batchUserPrivQO) {
        icpPrivService.upsertUserPrivBatch(batchUserPrivQO);
        return R.success();
    }

}
