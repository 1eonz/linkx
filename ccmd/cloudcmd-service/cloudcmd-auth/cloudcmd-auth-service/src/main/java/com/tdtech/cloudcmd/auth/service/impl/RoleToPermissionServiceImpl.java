package com.tdtech.cloudcmd.auth.service.impl;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.auth.entity.RoleToPermission;
import com.tdtech.cloudcmd.auth.mapper.RoleToPermissionMapper;
import com.tdtech.cloudcmd.auth.service.IRoleToPermissionService;

/**
 * <p>
 * 角色-权限关联表 服务实现类
 * </p>
 *
 * @author zhuangzl
 * @since 2020-06-01
 */
@Service
public class RoleToPermissionServiceImpl extends ServiceImpl<RoleToPermissionMapper, RoleToPermission>
    implements IRoleToPermissionService {

}
