package com.tdtech.cloudcmd.im.openapi.controller;

import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.i18n.I18nUtil;
import com.tdtech.cloudcmd.im.jingxin.api.ImUserRpcApi;
import com.tdtech.cloudcmd.im.jingxin.api.entity.im.UserGetResultVo;
import com.tdtech.cloudcmd.im.openapi.aop.OpenApiOauth;
import com.tdtech.cloudcmd.im.openapi.controller.constant.BusinessScopeEnum;
import com.tdtech.cloudcmd.im.openapi.utils.RequestHeaderUtil;
import com.tdtech.cloudcmd.msip.aop.Licensed;
import com.tdtech.cloudcmd.msip.aop.RequestLimit;
import com.tdtech.cloudcmd.msip.enums.LicenseEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@Slf4j
@Tag(name = "IM用户相关接口", description = "IM用户相关接口")
@RestController
@RequestMapping("/openapi/v1/users")
@RequiredArgsConstructor
@OpenApiOauth(BusinessScopeEnum.IM_USER)
@RequestLimit(business = "IM用户")
@Licensed(module = LicenseEnum.NORTHBOUND_DATA)
public class ImUserController {

    @DubboReference
    private ImUserRpcApi imUserRpcApi;

    @GetMapping
    @Operation(summary = "获取用户基本信息", description = "根据用户ID或身份证号获取用户基本信息列表")
    @Parameter(name = "Authorization", description = "用户认证Token", required = true, in = ParameterIn.HEADER)
    @Parameter(name = "Content-Type", description = "内容类型", required = true, in = ParameterIn.HEADER)
    @Parameter(name = "X-User-Id", description = "调用人的警信用户ID", in = ParameterIn.HEADER)
    public R<UserGetResultVo> getUsersInfo(
            @Parameter(description = "用户ID，多个用\",\"分隔，最大50个")
            String userIds,
            @Parameter(description = "用户身份证号，多个用\",\"分隔，最大50个")
            String idCards,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId) {

        if (!RequestHeaderUtil.validateXUserId(xUserId, imUserRpcApi)) {
            return R.failure(2, "X-User-Id无效");
        }

        if (StringUtils.isEmpty(userIds) && StringUtils.isEmpty(idCards)) {
            return R.failure(3, "查询用户信息时请携带用户ID或身份证号");
        }

        if (!isValidCommaSeparated(userIds) || !isValidCommaSeparated(idCards)) {
            return R.failure(4, "批量查询用户ID和身份证号时，请用\",\"分隔，最大50个");
        }

        log.info("openapi: '/openapi/v1/users', X-User-Id: {},  msg: success", xUserId);
        return R.success(imUserRpcApi.getUsersInfo(userIds, idCards, xUserId));
    }

    private boolean isValidCommaSeparated(String value) {
        if (StringUtils.isEmpty(value)) {
            return true;
        }
        String[] parts = value.split(",");
        if (parts.length > 50) {
            return false;
        }
        return Arrays.stream(parts).noneMatch(p -> StringUtils.isEmpty(p.trim()));
    }
    
}
