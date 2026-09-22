package com.tdtech.cloudcmd.im.openapi.controller;

import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.enums.ResponseCodeEnum;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.i18n.I18nUtil;
import com.tdtech.cloudcmd.im.openapi.controller.constant.GrantTypeEnum;
import com.tdtech.cloudcmd.im.openapi.controller.entity.LoginReq;
import com.tdtech.cloudcmd.im.openapi.controller.entity.Token;
import com.tdtech.cloudcmd.im.openapi.repo.OpenApplicationGrant;
import com.tdtech.cloudcmd.im.openapi.service.OpenApiOAuthService;
import com.tdtech.cloudcmd.msip.aop.RequestLimit;
import com.tdtech.cloudcmd.msip.enums.AlarmTemplateZhEnum;
import com.tdtech.cloudcmd.msip.util.ReportUtil;
import com.tdtech.cloudcmd.service.rpc.StatisticLoginRpcService;
import dto.StatisticLoginDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/openapi/v1/oauth")
@RequiredArgsConstructor
@Tag(name = "OAuth认证接口", description = "提供开放API的认证授权功能")
@RequestLimit(business = "OAuth认证")
public class OAuthController {

    private final OpenApiOAuthService openApiOAuthService;

    @Resource
    private ReportUtil reportUtil;

    @DubboReference(async = true, retries = 0, cluster = "failfast", timeout = 3000, sent = true)
    private StatisticLoginRpcService statisticLoginRpcService;

    /**
     * 登录
     *
     * @param loginReq 登录请求参数
     * @return 访问令牌
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "通过客户端凭证获取访问令牌")
    @ApiResponse(responseCode = "200", description = "成功返回访问令牌",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = R.class)))
    public R<Token> login(@RequestBody LoginReq loginReq, HttpServletRequest request) throws MethodArgumentNotValidException {
        Date loginTime = new Date();
        R<Token> result = null;
        StatisticLoginDTO statisticLoginDTO;
        try {
            result = getTokenResult(loginReq);
            Token token = getToken(result);
            statisticLoginDTO = Objects.isNull(token) ? StatisticLoginDTO.getFailureInstance(request, loginTime)
                    : StatisticLoginDTO.getSuccessInstance(request, loginTime);
            statisticLoginDTO.setClientTypeByUserId(statisticLoginDTO.getUserId());
            statisticLoginDTO.setAppId(getAppId(result));
            statisticLogin(statisticLoginDTO);
        } catch (Exception exception) {
            statisticLoginDTO = StatisticLoginDTO.getFailureInstance(request, loginTime);
            statisticLoginDTO.setClientTypeByUserId(statisticLoginDTO.getUserId());
            statisticLoginDTO.setAppId(getAppId(result));
            statisticLogin(statisticLoginDTO);
            throw exception;
        }

        return result;
    }

    private void statisticLogin(StatisticLoginDTO dto) {
        try {
            statisticLoginRpcService.statisticLogin(dto);
        } catch (Exception e) {
            log.error("Statistic login error.", e);
        }
    }

    private String getAppId(R<Token> result) {
        Token token = getToken(result);
        if (Objects.isNull(token)) {
            return null;
        }

        String appId = token.getClientId();
        return StringUtils.isBlank(appId) ? null : appId;
    }

    private Token getToken(R<Token> result) {
        if (Objects.isNull(result)) {
            return null;
        }

        Token token = result.getData();
        if (Objects.isNull(token)) {
            return null;
        }
        return token;
    }

    private R<Token> getTokenResult(LoginReq loginReq) throws MethodArgumentNotValidException {
        if (Objects.isNull(loginReq.getGrantType())) {
            return R.failure(ResponseCodeEnum.COMMON_ERROR_106.getCode(),
                    I18nUtil.get(ResponseCodeEnum.COMMON_ERROR_106.getMsg()) + "参数：" + "grantType不能为空");
        }
        if (StringUtils.isBlank(loginReq.getClientId())) {
            return R.failure(ResponseCodeEnum.COMMON_ERROR_106.getCode(),
                    I18nUtil.get(ResponseCodeEnum.COMMON_ERROR_106.getMsg()) + "参数：" + "clientId不能为空");
        }
        if (StringUtils.isBlank(loginReq.getClientSecret())) {
            return R.failure(ResponseCodeEnum.COMMON_ERROR_106.getCode(),
                    I18nUtil.get(ResponseCodeEnum.COMMON_ERROR_106.getMsg()) + "参数：" + "clientSecret不能为空");
        }

        Integer grantType = loginReq.getGrantType();
        if (!GrantTypeEnum.CLIENT_CREDENTIALS.getCode().equals(grantType)) {
            return R.failure("grant type: " + grantType + " not supported yet.");
        }
        OpenApplicationGrant client;
        try {
            client = openApiOAuthService.findConfig(loginReq.getClientId(), loginReq.getClientSecret());
        } catch (BusinessException e) {
            log.warn("oauth login client is expired, clientId: {}, message: {}", loginReq.getClientId(),
                    e.getMessage());
            // 发送告警
            reportUtil.saveAlarm2MSIP(AlarmTemplateZhEnum.THIRD_PARTY_SYSTEM_DISCONNECTION);
            return R.failure(e.getMessage());
        }
        if (Objects.isNull(client)) {
            log.info("oauth login clientInfo is null, clientId: {}, clientSecret: {}", loginReq.getClientId(),
                    loginReq.getClientSecret());
            // 发送告警
            reportUtil.saveAlarm2MSIP(AlarmTemplateZhEnum.THIRD_PARTY_SYSTEM_DISCONNECTION);

            return R.failure("clientId or clientSecret mismatch ");
        }
        var token = openApiOAuthService.create(client);
        // 擦除告警
        reportUtil.clearAlarm2MSIP(AlarmTemplateZhEnum.THIRD_PARTY_SYSTEM_DISCONNECTION);
        return R.success(token);
    }

    /**
     * 退出
     *
     * @param authorization 认证头信息
     * @return 操作结果
     */
    @PostMapping("/logout")
    @Operation(summary = "用户退出", description = "通过访问令牌和客户端凭证退出登录")
    @ApiResponse(responseCode = "200", description = "成功退出",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = R.class)))
    public R<Void> logout(@Parameter(description = "认证令牌", in = ParameterIn.HEADER, required = true)
                    @RequestHeader("Authorization") String authorization) {
        openApiOAuthService.remove(authorization);
        return R.success();
    }

}