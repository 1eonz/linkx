package com.tdtech.cloudcmd.auth.controller;

import com.alibaba.fastjson.JSONObject;
import com.tdtech.cloudcmd.auth.constant.AuthBaseConstant;
import com.tdtech.cloudcmd.auth.dto.OAuthLoginDto;
import com.tdtech.cloudcmd.auth.dto.OAuthLoginInfo;
import com.tdtech.cloudcmd.auth.dto.OAuthPwdDto;
import com.tdtech.cloudcmd.auth.dto.OAuthRefreshDto;
import com.tdtech.cloudcmd.auth.dto.OAuthRefreshInfo;
import com.tdtech.cloudcmd.auth.dto.PermissionDto;
import com.tdtech.cloudcmd.auth.entity.Application;
import com.tdtech.cloudcmd.auth.entity.Role;
import com.tdtech.cloudcmd.auth.exception.OAuthException;
import com.tdtech.cloudcmd.auth.service.IApplicationService;
import com.tdtech.cloudcmd.auth.service.IOAuthLoginService;
import com.tdtech.cloudcmd.auth.service.IPermissionService;
import com.tdtech.cloudcmd.auth.service.IRoleService;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.constant.AuthConstants;
import com.tdtech.cloudcmd.i18n.I18nUtil;
import com.tdtech.cloudcmd.msip.aop.Licensed;
import com.tdtech.cloudcmd.msip.enums.LicenseEnum;
import com.tdtech.cloudcmd.redis.RedisUtil;
import com.tdtech.cloudcmd.service.rpc.StatisticLoginRpcService;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import com.tdtech.cloudcmd.web.utils.ServletRequestContext;
import dto.StatisticLoginDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tdtech.cloudcmd.auth.enums.CommonErrorEnum.COMMON_ERROR_118;
import static com.tdtech.cloudcmd.auth.enums.CommonErrorEnum.COMMON_ERROR_142;
import static com.tdtech.cloudcmd.constant.AuthConstants.ACCESSS_PERMISSION_UPDATE_MENU;
import static com.tdtech.cloudcmd.constant.AuthConstants.ACCESSS_ROLE_CHANGE_STATE;

/**
 * @author mWX556161
 * @date 2020/11/18 14:43
 */
@Slf4j
@RestController
@RequestMapping("/auth/v1/oauth")
@Tag(name = "OAuth认证接口", description = "提供OAuth2.0认证相关操作接口")
public class OAuthLoginController {

    @Resource
    private IOAuthLoginService oAuthLoginService;
    @Resource
    private IPermissionService permissionService;
    @Resource
    private IApplicationService applicationService;
    @Resource
    private RedisUtil redisUtil;
    @Resource
    private IRoleService roleService;

    @DubboReference(async = true, retries = 0, cluster = "failfast", timeout = 3000, sent = true)
    private StatisticLoginRpcService statisticLoginRpcService;

    /**
     * 登录入口
     *
     * @return
     */
    @Operation(summary = "用户登录", description = "通过用户名和密码进行OAuth登录认证")
    @PostMapping("/v2/login")
    @Licensed(module = LicenseEnum.ALL)
    public R OauthLogin(@RequestBody OAuthLoginInfo loginInfo, HttpServletRequest request) {
        Date loginTime = new Date();
        OAuthLoginDto authLoginDto = null;
        StatisticLoginDTO statisticLoginDTO;
        try {
            authLoginDto = getOAuthLoginDto(loginInfo);
            statisticLoginDTO = StatisticLoginDTO.getSuccessInstance(request, loginTime);
            statisticLoginDTO.setUserId(authLoginDto.getUserId());
            statisticLogin(statisticLoginDTO);
        } catch (Exception exception) {
            statisticLoginDTO = StatisticLoginDTO.getFailureInstance(request, loginTime);
            statisticLoginDTO.setUserId(Objects.isNull(authLoginDto) ? null : authLoginDto.getUserId());
            statisticLogin(statisticLoginDTO);
            throw exception;
        }

        return R.success(authLoginDto);
    }

    private void statisticLogin(StatisticLoginDTO dto) {
        try {
            statisticLoginRpcService.statisticLogin(dto);
        } catch (Exception e) {
            log.error("Statistic login error.", e);
        }
    }

    private OAuthLoginDto getOAuthLoginDto(OAuthLoginInfo loginInfo) {
        var appKey = Objects.requireNonNull(ServletRequestContext.getAppKey());

        if (loginInfo.getUsername().trim().length() > 32 || loginInfo.getPassword().length() > 64) {
            throw new OAuthException(COMMON_ERROR_142.getCode(), I18nUtil.get(COMMON_ERROR_142.getMsg()));
        }

        Application application = applicationService.getApplicationById(appKey);
        loginInfo.setLoginIp(ServletRequestContext.getIp());
        loginInfo.setGrantType(AuthBaseConstant.PWD_AUTH_TYPE);
        return oAuthLoginService.oAuthLogin(loginInfo, application);
    }

    @Operation(summary = "身份证号登录")
    @PostMapping("/v2/h5login")
    public R h5login(@RequestBody OAuthLoginInfo loginInfo) {
        var appKey = ServletRequestContext.getAppKey();
        appKey = appKey == null || appKey.isBlank() ? "CAPP-1000" : appKey;
        var app = applicationService.getApplicationById(appKey);
        loginInfo.setLoginIp(ServletRequestContext.getIp());
        loginInfo.setGrantType(AuthBaseConstant.H5_TYPE);
        loginInfo.setClientId(appKey);
        loginInfo.setApplicationId(app.getId());
        return R.success(oAuthLoginService.h5Login(loginInfo, app));
    }

    @Operation(summary = "token登录")
    @PostMapping("/v2/tokenLogin")
    public R tokenLogin(@RequestBody OAuthLoginInfo loginInfo, HttpServletRequest request) {
        Date loginTime = new Date();
        OAuthLoginDto authLoginDto = null;
        StatisticLoginDTO statisticLoginDTO;
        try {
            authLoginDto = getTokenLogin(loginInfo);
            statisticLoginDTO = StatisticLoginDTO.getSuccessInstance(request, loginTime);
            statisticLoginDTO.setUserId(authLoginDto.getUserId());
            statisticLogin(statisticLoginDTO);
        } catch (Exception exception) {
            statisticLoginDTO = StatisticLoginDTO.getFailureInstance(request, loginTime);
            statisticLoginDTO.setUserId(Objects.isNull(authLoginDto) ? null : authLoginDto.getUserId());
            statisticLogin(statisticLoginDTO);
            throw exception;
        }

        return R.success(authLoginDto);
    }

    private OAuthLoginDto getTokenLogin(OAuthLoginInfo loginInfo) {
        var appKey = ServletRequestContext.getAppKey();
        appKey = appKey == null || appKey.isBlank() ? "CAPP-1000" : appKey;
        var app = applicationService.getApplicationById(appKey);
        loginInfo.setLoginIp(ServletRequestContext.getIp());
        loginInfo.setGrantType(AuthBaseConstant.IM_TOKEN_TYPE);
        loginInfo.setClientId(appKey);
        loginInfo.setApplicationId(app.getId());
        return oAuthLoginService.tokenLogin(loginInfo, app);
    }

    /**
     * 登出入口
     *
     * @return
     */
    @PostMapping("/v2/logout")
    public R OauthLogout() {
        String token = SecurityUtils.getToken();
        if (token == null || token.isBlank()) {
            return R.failure();
        }
        UserInfo user = SecurityUtils.getUser();
        log.info("OauthLogout user: {}", user);
        oAuthLoginService.oAuthLogout(user, token, ServletRequestContext.getIp());
        return R.success();
    }

    /**
     * token刷新
     */
    @PostMapping("/refresh")
    public R OauthRefresh(@RequestBody OAuthRefreshInfo oAuthRefreshInfo) {
        String token = Optional.ofNullable(SecurityUtils.getToken())
            .orElseThrow(() -> new OAuthException(COMMON_ERROR_118.getCode(), I18nUtil.get(COMMON_ERROR_118.getMsg())));
        oAuthRefreshInfo.setAuthorization(token);
        OAuthRefreshDto oAuthRefreshDto = oAuthLoginService.oAuthRefreshToken(oAuthRefreshInfo);
        return R.success(oAuthRefreshDto);
    }

    /**
     * 心跳检测
     *
     * @param authorization
     * @return
     */
    @PostMapping("/v2/keepalive")
    public R OauthKeepalive(@RequestHeader("Authorization") String authorization) {
        String token = Optional.ofNullable(SecurityUtils.getToken())
            .orElseThrow(() -> new OAuthException(COMMON_ERROR_118.getCode(), I18nUtil.get(COMMON_ERROR_118.getMsg())));
        return oAuthLoginService.oauthKeepalive(token);
    }

    @PostMapping("/v2/permissions")
    public R getPermissions() {

        var loginCacheDto = Optional.ofNullable(SecurityUtils.getUser())
            .orElseThrow(() -> new OAuthException(COMMON_ERROR_118.getCode(), I18nUtil.get(COMMON_ERROR_118.getMsg())));
        PermissionDto permissionDto =
            redisUtil.get(AuthConstants.ACCESSS_PERMISSION + loginCacheDto.getUserId(), PermissionDto.class);
        log.debug("permissions userId:{},permissionDto:{}", loginCacheDto.getUserId(),
            JSONObject.toJSONString(permissionDto));

        Boolean status;
        List<Long> roleIds = new ArrayList<>();
        if (permissionDto != null) {
            String deleteMark = redisUtil.get(ACCESSS_PERMISSION_UPDATE_MENU, String.class);
            List<Long> deletes = JSONObject.parseArray(deleteMark, Long.class);

            roleIds = permissionDto.getRoleIds();
            status = permissionService.getDeleteStatus(deletes, roleIds);
        } else {
            status = true;
        }
        String value = redisUtil.getString(ACCESSS_ROLE_CHANGE_STATE + loginCacheDto.getUserId());
        if (value != null && value.equals("1")) {
            status = true;
            redisUtil.set(ACCESSS_ROLE_CHANGE_STATE + loginCacheDto.getUserId(), "0");
        }
        if (status) {
            // 被删除以后需要重新取mysql里的值
            var roles = roleService.getRoleInfoListByUserId(loginCacheDto.getUserId(), false);
            var menuPermissions = roles.stream().flatMap(
                a -> Stream.of(a.getAdminPrivJson(), a.getIccPrivJson(), a.getCappPrivJson()).filter(Objects::nonNull)
                    .flatMap(Collection::stream)).distinct().collect(Collectors.toList());
            log.info("mysql menu:{}", menuPermissions);
            permissionDto.setMenus(menuPermissions);
            List<Long> newRoleIds = roles.stream().map(Role::getId).collect(Collectors.toList());
            permissionDto.setRoleIds(newRoleIds);
            redisUtil.set(AuthConstants.ACCESSS_PERMISSION + loginCacheDto.getUserId(), permissionDto);
        }
        if (permissionDto != null) {
            // 菜单权限从缓存里重新取（最新数据）
            List<String> menuPermissions = roleService.getMenuCacheByRoles(roleIds);
            log.debug("permissions 缓存数据 :{}", JSONObject.toJSONString(menuPermissions));
            permissionDto.setMenus(menuPermissions);
            permissionDto.setRole(I18nUtil.get(permissionDto.getRole()));
            return R.success(permissionDto);
        }
        var applicationById =
            applicationService.getApplicationById(Objects.requireNonNull(ServletRequestContext.getAppKey()));
        String token = Optional.ofNullable(SecurityUtils.getToken())
            .orElseThrow(() -> new OAuthException(COMMON_ERROR_118.getCode(), I18nUtil.get(COMMON_ERROR_118.getMsg())));
        permissionDto = permissionService.getPermissions(token, applicationById);
        log.debug("----------------------get permission is {}", permissionDto.getRole());
        permissionDto.setRole(I18nUtil.get(permissionDto.getRole()));
        log.debug("----------------------get i18n permission is {}", permissionDto.getRole());
        return R.success(permissionDto);
    }

    /**
     *
     */
    @PostMapping("v2/loginMessage")
    public R getLoginMessage() {
        String result = oAuthLoginService.getLoginMessage();
        return R.success(result);
    }

    @PostMapping("v2/changePwd")
    public R<Void> changePwd(@RequestBody OAuthPwdDto pwdDto) {
        try {
            oAuthLoginService.changePassword(pwdDto, ServletRequestContext.getIp());
        } catch (RuntimeException e) {
            return R.failure(1, I18nUtil.get(e.getMessage()));
        }
        return R.success();
    }

}
