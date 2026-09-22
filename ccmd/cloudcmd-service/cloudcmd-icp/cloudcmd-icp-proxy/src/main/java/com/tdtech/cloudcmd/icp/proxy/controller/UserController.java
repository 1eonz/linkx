package com.tdtech.cloudcmd.icp.proxy.controller;

import com.github.yulichang.toolkit.MPJWrappers;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.icp.proxy.controller.vo.UserQO;
import com.tdtech.cloudcmd.icp.proxy.controller.vo.UserVO;
import com.tdtech.cloudcmd.icp.proxy.entity.*;
import com.tdtech.cloudcmd.icp.proxy.service.IcpPrivService;
import com.tdtech.cloudcmd.icp.proxy.service.ImUserService;
import com.tdtech.cloudcmd.icp.proxy.service.IsdnTypeService;
import com.tdtech.cloudcmd.icp.proxy.service.UserService;
import com.tdtech.cloudcmd.icp.proxy.util.SyncUtil;
import com.tdtech.cloudcmd.mysql.entity.CcmdPage;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/proxy/icp/v1/user")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户信息查询和同步接口")
public class UserController {

    private final UserService userService;
    private final ThreadPoolTaskExecutor asyncMessageTaskExecutor;
    private final IcpPrivService icpPrivService;
    private final ImUserService imUserService;
    private final IsdnTypeService isdnTypeService;

    @GetMapping
    @Operation(summary = "分页查询用户", description = "根据条件分页查询用户信息，包括在线状态和位置信息")
    @ApiResponse(responseCode = "200", description = "查询成功",
        content = @Content(schema = @Schema(implementation = R.class)))
    public R<CcmdPage<UserVO>> selectPage(UserQO qo) {
        if (SyncUtil.getConnect() != null && !SyncUtil.getConnect().isEmpty() && !SyncUtil.getConnectStatus(SyncUtil.ERROR_CODE).isEmpty()) {
            return R.failure(SyncUtil.getConnectStatus(SyncUtil.ERROR_CODE));
        }

        var user = Objects.requireNonNull(SecurityUtils.getUser());
        var privs = icpPrivService.getUserPrivByUser(user.getUserId());
        if (privs == null || privs.isEmpty()) {
            var cameraPrivByUser = icpPrivService.getCameraPrivByUser(user.getUserId());
            if (cameraPrivByUser == null || cameraPrivByUser.isEmpty()) {
                return R.success(1, "当前登录用户无任何设备调度权限！", CcmdPage.empty(qo));
            }
            return R.success(CcmdPage.empty(qo));
        }
        var typeList = qo.getTypeList();
        var categorys = Optional.ofNullable(qo.getCategorys()).map(a -> a.split(",")).map(Arrays::asList)
            .orElse(Collections.emptyList());
        IsdnType isdnType = qo.getIsdnTypeId() != null ? isdnTypeService.selectById(qo.getIsdnTypeId()) : null;
        var wrapper = MPJWrappers.lambdaJoin(User.class).selectAll(User.class)
            .selectAs("IF(os.status_value is null, 4012 ,os.status_value)", UserVO::getStatusValue)
            .selectAs(Gis::getLat, UserVO::getLat)//
            .selectAs(Gis::getLon, UserVO::getLon)//
            .selectAs(Department::getDepartmentname, UserVO::getDepartmntName)
            .leftJoin(OnlineStatus.class, "os", OnlineStatus::getIsdn, User::getIsdn)
            .leftJoin(Gis.class, "gs", Gis::getIsdn, User::getIsdn)
            .leftJoin(Department.class, "de", Department::getDepartmentid, User::getDepartmentid)
            .eq(qo.getIsOnline() != null && qo.getIsOnline(), OnlineStatus::getStatusValue, "4011")
            .ne(qo.getIsOnline() != null && !qo.getIsOnline(), OnlineStatus::getStatusValue, "4011")
            .eq(qo.getDepartmentId() != null && !qo.getDepartmentId().isBlank(), User::getDepartmentid,
                qo.getDepartmentId())//
            .ne(User::getCategory, 10)//
            .in(User::getDepartmentid, privs)//
            .in(!categorys.isEmpty(), User::getCategory, categorys);//
        if (isdnType != null) {
            wrapper = wrapper
                .eq(User::getCategory, isdnType.getCategory())
                .eq(User::getSubusercategory, isdnType.getSubusercategory())
                .eq(User::getApptype, isdnType.getApptype());
        }
        wrapper = wrapper.orderByAsc(qo.getOnlineFirst() != null && qo.getOnlineFirst(), "statusValue")
            .orderByDesc(qo.getOnlineFirst() != null && !qo.getOnlineFirst(), "statusValue")
            .orderByAsc(User::getId)//
            .and(qo.getSearch() != null && !qo.getSearch().isBlank(),//
                w -> w.like(User::getIsdn, qo.getSearch())//
                    .or()//
                    .like(User::getAlias, qo.getSearch())//
                    .or()//
                    .like(User::getName, qo.getSearch()))//
            .and(isdnType == null && (typeList.contains(0) || typeList.contains(1)), w -> {
                w.or(typeList.contains(0), // 记录仪
                        w1 -> w1.or(w2 -> w2.eq(User::getApptype, 109).eq(User::getCategory, 9)//
                            )//
                            .or(//
                                w2 -> w2.eq(User::getApptype, 0).eq(User::getCategory, 9).eq(User::getSubusercategory, 1)//
                            )) //
                    .or(typeList.contains(1),// //布控球
                        w1 -> w1.eq(User::getCategory, 9).eq(User::getSubusercategory, 3)//
                    );
            });//

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
        var cameraVOCcmdPage = userService.selectPage(qo, UserVO.class, wrapper);
        // 获取 IsdnType 映射
        Map<String, IsdnType> isdnTypeMap = isdnTypeService.getIsdnTypeMap();

        // 转换为 UserVO 并填充 icon 和 iconUri
        List<UserVO> voList = cameraVOCcmdPage.getRecords().stream().map(userVO -> {
            UserVO vo = new UserVO();
            BeanUtils.copyProperties(userVO, vo);
            String key = isdnTypeService.buildIsdnTypeKey(userVO.getCategory(), userVO.getSubusercategory(), userVO.getApptype());
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

    @PostMapping("/pull")
    @Operation(summary = "同步用户数据", description = "从源系统拉取并更新所有用户数据")
    @ApiResponse(responseCode = "200", description = "同步成功")
    @ApiResponse(responseCode = "500", description = "同步失败")
    public R<Void> doPull() {
        asyncMessageTaskExecutor.execute(userService::pull);
        return R.success();
    }
}