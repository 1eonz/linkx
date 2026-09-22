package com.tdtech.cloudcmd.auth.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.cloudcmd.auth.dto.PermissionDto;
import com.tdtech.cloudcmd.auth.entity.Application;
import com.tdtech.cloudcmd.auth.entity.Permission;

/**
 * <p>
 * 权限信息表(可以很方便的扩展) 服务类
 * </p>
 *
 * @author zhuangzl
 * @since 2020-06-01
 */
public interface IPermissionService extends IService<Permission> {

    /**
     * 获取所有的权限
     * 
     * @param token
     * @param applicationId
     * @return
     */
    PermissionDto getPermissions(String token, Application application);

    Boolean getDeleteStatus(List<Long> deletes, List<Long> roleIds);

//    void refreshPermission(Long orgId);

}
