package com.tdtech.cloudcmd.auth.service.impl;

import static com.tdtech.cloudcmd.auth.constant.OAuthConstant.ACCESS_TOKEN_USER_KEY;
import static com.tdtech.cloudcmd.auth.enums.CommonErrorEnum.COMMON_ERROR_133;
import static com.tdtech.cloudcmd.auth.exception.OAuthErrorEnum.COMMON_ERROR_524;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.auth.dto.PermissionDto;
import com.tdtech.cloudcmd.auth.entity.Application;
import com.tdtech.cloudcmd.auth.entity.ImUserDO;
import com.tdtech.cloudcmd.auth.entity.Permission;
import com.tdtech.cloudcmd.auth.entity.Role;
import com.tdtech.cloudcmd.auth.exception.OAuthException;
import com.tdtech.cloudcmd.auth.mapper.ImUserMapper;
import com.tdtech.cloudcmd.auth.mapper.PermissionMapper;
import com.tdtech.cloudcmd.auth.mapper.RoleToPermissionMapper;
import com.tdtech.cloudcmd.auth.service.IPermissionService;
import com.tdtech.cloudcmd.auth.service.IRoleService;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.constant.AuthConstants;
import com.tdtech.cloudcmd.i18n.I18nUtil;
import com.tdtech.cloudcmd.redis.RedisUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * 权限信息表(可以很方便的扩展) 服务实现类
 * </p>
 *
 * @author zhuangzl
 * @since 2020-06-01
 */
@Slf4j
@Service
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements IPermissionService {

    @Resource
    private RedisUtil redisUtil;
    @Resource
    private ImUserMapper imUserMapper;
    @Resource
    private IRoleService roleService;
    @Resource
    private RoleToPermissionMapper roleToPermissionMapper;

    public PermissionDto getPermissions(ImUserDO user, List<? extends Role> roles,
        Application application) {
        var permissionDto = new PermissionDto();
        // menu permissions
        List<Long> roleIds = new ArrayList<>();
        for (int i = 0; i < roles.size(); i++) {
            List<String> menuPri = new ArrayList<>();
            Role role = roles.get(i);
            if (role.getAdminPrivJson() != null) {
                menuPri.addAll(role.getAdminPrivJson());
            }
            if (role.getCappPrivJson() != null) {
                menuPri.addAll(role.getCappPrivJson());
            }
            if (role.getIccPrivJson() != null) {
                menuPri.addAll(role.getIccPrivJson());
            }
            String menuPermissions = JSONObject.toJSONString(menuPri);
            redisUtil.set(AuthConstants.ACCESSS_PERMISSION_MENU + role.getId(), menuPermissions);

            roleIds.add(role.getId());
        }
        var menuPermissions =
            roles.stream().flatMap(a -> Stream.of(a.getAdminPrivJson(), a.getIccPrivJson(), a.getCappPrivJson())
                .filter(Objects::nonNull).flatMap(Collection::stream)).distinct().collect(Collectors.toList());
        permissionDto.setMenus(menuPermissions);
        permissionDto.setRoleIds(roleIds);
        // data permissions
        permissionDto.setOrganization(user.getDepartmentId());
        permissionDto.setOrganizations(roles.get(0).getImOrgPrivJson());
        permissionDto.setHasChildOrgPriv(0);
        permissionDto.setApplicationId(application.getId());
        // DEPRECATED
        if (application.isAdmin()) {
            List<String> allactionPermission = roleToPermissionMapper.queryPermission(null, application.getId());
            redisUtil.set(AuthConstants.ACCESSS_PERMISSION_ACT_ALL + application.getId(), allactionPermission);
            redisUtil.set(AuthConstants.ACCESSS_PERMISSION_ACT + application.getId(), allactionPermission);
            permissionDto.setAllActions(allactionPermission);
            permissionDto.setActions(allactionPermission);
        }
        permissionDto.setType(user.getType());

        redisUtil.set(AuthConstants.ACCESSS_PERMISSION + user.getId(), permissionDto);
        return permissionDto;
    }

    @Override
    public PermissionDto getPermissions(String token, Application application) {
        log.warn("getPermissions token {}, application {}", token, application);
        UserInfo loginCacheDto = redisUtil.get(ACCESS_TOKEN_USER_KEY.replace("{access_token}", token), UserInfo.class);
        if (loginCacheDto == null) {
            log.warn("getPermissions loginCacheDtoStr is null ");
            throw new OAuthException(COMMON_ERROR_524.getCode(), I18nUtil.get(COMMON_ERROR_524.getMsg()));
        }
        Long userId = loginCacheDto.getUserId();
        log.debug("getPermissions userId {} ", userId);
        //
        // 根据userId找到角色id
        var roles = roleService.getRoleWithDataPriv(userId, null, true);
        if (roles == null || roles.size() == 0) {
            // 没有角色抛出异常提示
            throw new OAuthException(COMMON_ERROR_133.getCode(), I18nUtil.get(COMMON_ERROR_133.getMsg()),
                "未查询到该用户没有角色信息");
        }
        log.debug("getPermissions roles {} ", roles);
        var imUserDO = imUserMapper.selectById(userId);
        return getPermissions(imUserDO, roles, application);
    }

    @Override
    public Boolean getDeleteStatus(List<Long> deletes, List<Long> roleIds) {
        if (deletes == null || deletes.isEmpty()) {
            return false;
        }
        for (int i = 0; i < roleIds.size(); i++) {
            Long roleId = roleIds.get(i);
            boolean contains = deletes.contains(roleId);
            if (contains) {
                return true;
            }
        }
        return false;
    }

    // @Override
    // public void refreshPermission(Long orgId) {
    // var organization = roleToPermissionMapper.getOrganization(orgId);
    // var fullPath = organization.getFullPath();
    // var orgIds = Arrays.stream(fullPath.split(",")).filter(a -> !a.isBlank()).map(Long::parseLong)
    // .collect(Collectors.toList());
    // var userIdByOrgIds = executorMapper.getUserIdByOrgIds(orgIds);
    // if (userIdByOrgIds != null && !userIdByOrgIds.isEmpty()) {
    // userIdByOrgIds.forEach(uid -> {
    // var permissionDto = redisUtil.get(AuthConstants.ACCESSS_PERMISSION + uid, PermissionDto.class);
    // if (permissionDto == null) {
    // return;
    // }
    // var organizations = permissionDto.getOrganizations();
    // if (!organizations.contains(orgId)) {
    // organizations.add(orgId);
    // }
    // redisUtil.set(AuthConstants.ACCESSS_PERMISSION + uid, permissionDto);
    // });
    // }
    // }
}
