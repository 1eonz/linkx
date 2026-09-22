package com.tdtech.cloudcmd.auth.service;

import com.tdtech.cloudcmd.auth.controller.admin.vo.OrgPrivDto;
import com.tdtech.cloudcmd.auth.dto.ImUserDto;
import com.tdtech.cloudcmd.auth.dto.RoleDto;

import java.util.List;
import java.util.Map;

/**
 * @author mWX556161
 * @date 2020/9/27 14:14
 */
public interface RoleRpcService {

    /**
     * 根据类型查询角色列表
     *
     * @param type
     * @return
     */
    List<RoleDto> getRoleListByType(Integer type);

    void createRoleMenuCache();

    RoleDto getRoleByUserId(String userId);

    ImUserDto getUserInfoByUserId(String userId);

    List<ImUserDto> getUserInfoByUserId(List<Long> userIds);

    /**
     * 保存角色的组织权限（使用关联表）
     *
     * @param roleId 角色ID
     * @param grantUserId 授权用户ID
     * @param orgPrivList 组织权限列表
     */
    void saveOrgPriv(Long roleId, Long grantUserId, List<OrgPrivDto> orgPrivList);

    /**
     * 获取角色的组织权限列表
     *
     * @param roleIds 角色ID
     * @return 组织权限列表
     */
    Map<Long, List<OrgPrivDto>> getOrgPrivByRoleId(List<Long> roleIds);

     /**
     * 删除角色的组织权限（使用关联表）
     *
     * @param roleIds 角色ID
     */
    void deleteOrgPriv(List<Long> roleIds);
}