package com.tdtech.cloudcmd.im.openapi.controller;

import cloudcmd.service.rpc.AppInfoRpcService;
import cloudcmd.vo.AppInfoResp4RpcVO;
import cloudcmd.vo.UserCommonAppResp4RpcVo;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.im.jingxin.api.ImUserRpcApi;
import com.tdtech.cloudcmd.im.openapi.aop.OpenApiOauth;
import com.tdtech.cloudcmd.im.openapi.controller.constant.AppListQueryTypeEnum;
import com.tdtech.cloudcmd.im.openapi.controller.constant.BusinessScopeEnum;
import com.tdtech.cloudcmd.im.openapi.utils.RequestHeaderUtil;
import com.tdtech.cloudcmd.linkx.third.api.Vo.AppUsedRankingVo;
import com.tdtech.cloudcmd.linkx.third.api.rpc.AppGroupRpcService;
import com.tdtech.cloudcmd.msip.aop.Licensed;
import com.tdtech.cloudcmd.msip.aop.RequestLimit;
import com.tdtech.cloudcmd.msip.enums.LicenseEnum;
import com.tdtech.cloudcmd.util.CollectionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Tag(name = "应用相关接口", description = "应用相关接口")
@RestController
@RequestMapping("/openapi/v1/apps")
@RequiredArgsConstructor
@Validated
@OpenApiOauth(BusinessScopeEnum.APPLICATION)
@RequestLimit(business = "应用相关接口")
@Licensed(module = LicenseEnum.NORTHBOUND_DATA)
public class AppController {

    @DubboReference
    private AppInfoRpcService appInfoRpcService;

    @DubboReference
    private ImUserRpcApi imUserRpcApi;

    @DubboReference
    private AppGroupRpcService appGroupRpcService;

    @Operation(summary = "获取应用列表", description = "type=0：获取全部应用；type=1：获取当前用户的常用应用；page/pageSize 可选，同时传入时对结果分页返回，不传返回全量")
    @Parameter(name = "Authorization", description = "认证令牌", required = true, in = ParameterIn.HEADER)
    @Parameter(name = "type", description = "0-全部应用，1-我的常用应用", required = true, in = ParameterIn.QUERY)
    @Parameter(name = "page", description = "页码，从1开始，需与pageSize同时传入", in = ParameterIn.QUERY)
    @Parameter(name = "pageSize", description = "每页条数，需与page同时传入", in = ParameterIn.QUERY)
    @GetMapping
    public R<List<UserCommonAppResp4RpcVo>> getAppList(@RequestParam(value = "type") Integer type,
                                                       @RequestParam(value = "page", required = false) Integer page,
                                                       @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        String userId = RequestHeaderUtil.getClientUserId();
        log.info("getAppList, type: {}, page: {}, pageSize: {}, clientUserId: {}", type, page, pageSize, userId);

        AppListQueryTypeEnum queryType = AppListQueryTypeEnum.of(type);
        if (queryType == null) {
            return R.failure(900202, "type参数格式错误，仅支持0或1");
        }
        if ((page == null) != (pageSize == null)) {
            return R.failure(900202, "page和pageSize需同时传入");
        }
        if (page != null && page < 1) {
            return R.failure(900202, "page参数格式错误，需大于等于1");
        }
        if (pageSize != null && pageSize < 1) {
            return R.failure(900202, "pageSize参数格式错误，需大于等于1");
        }
        R<List<UserCommonAppResp4RpcVo>> result = queryType == AppListQueryTypeEnum.MY ? getMyApps(userId) : getAllApps();
        // page/pageSize 均未传时返回全量，保持原有行为兼容；同时传入时对结果内存分页
        if (page == null || result.getData() == null) {
            return result;
        }
        List<UserCommonAppResp4RpcVo> data = result.getData();
        int fromIndex = Math.min((page - 1) * pageSize, data.size());
        int toIndex = Math.min(fromIndex + pageSize, data.size());
        result.setData(new ArrayList<>(data.subList(fromIndex, toIndex)));
        return result;
    }

    private R<List<UserCommonAppResp4RpcVo>> getMyApps(String userId) {
        if (StringUtils.isBlank(userId)) {
            return R.failure(900201, "type=1时用户ID不能为空");
        }
        Long userIdLong;
        try {
            userIdLong = Long.valueOf(userId);
        } catch (NumberFormatException e) {
            return R.failure(900202, "用户ID格式错误");
        }

        Integer sortType = imUserRpcApi.getMySortedType(userIdLong);
        // sortType 为 1 时走按热度排序（与 UserProfileServiceImpl 默认逻辑一致）
        if (Integer.valueOf(1).equals(sortType)) {
            List<AppUsedRankingVo> appUsedRanking = appGroupRpcService.getAppUsedRanking(userIdLong, 1, 2);
            if (CollectionUtils.isEmpty(appUsedRanking)) {
                return R.success(Collections.emptyList());
            }
            List<UserCommonAppResp4RpcVo> result = new ArrayList<>(appUsedRanking.size());
            for (AppUsedRankingVo vo : appUsedRanking) {
                if (vo == null) {
                    continue;
                }
                UserCommonAppResp4RpcVo app = new UserCommonAppResp4RpcVo();
                app.setId(vo.getId());
                app.setUserId(userId);
                app.setSort(vo.getCount());
                app.setAppId(vo.getAppId());
                app.setApp(vo.getApp());
                result.add(app);
            }
            return R.success(result);
        }
        List<UserCommonAppResp4RpcVo> myApps = appInfoRpcService.getMy(userId, 1, 2);
        return R.success(myApps != null ? myApps : Collections.emptyList());
    }

    private R<List<UserCommonAppResp4RpcVo>> getAllApps() {
        List<AppInfoResp4RpcVO> allApps = appInfoRpcService.getAllAppInfoByIds(null);
        if (allApps == null || allApps.isEmpty()) {
            return R.success(Collections.emptyList());
        }
        List<UserCommonAppResp4RpcVo> result = new ArrayList<>(allApps.size());
        for (AppInfoResp4RpcVO app : allApps) {
            if (app == null) {
                continue;
            }
            UserCommonAppResp4RpcVo vo = new UserCommonAppResp4RpcVo();
            vo.setSort(app.getSort());
            vo.setAppId(app.getId());
            vo.setApp(app);
            result.add(vo);
        }
        return R.success(result);
    }
}