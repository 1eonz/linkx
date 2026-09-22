package com.tdtech.cloudcmd.auth.service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.cloudcmd.auth.controller.admin.vo.OrgPrivDto;
import com.tdtech.cloudcmd.auth.entity.ImUserDO;
import com.tdtech.cloudcmd.auth.entity.Role;
import com.tdtech.cloudcmd.auth.entity.RoleDataPriv;
import com.tdtech.cloudcmd.auth.entity.RoleDto;

/**
 * <p>
 * 角色信息表 服务类
 * </p>
 *
 * @author zhuangzl
 * @since 2020-06-01
 */
public interface IRoleService extends IService<Role> {

    /**
     * 根据类型查询所有角色
     * 
     * @param type
     * @return
     */
    List<Role> getRoleListByType(Integer type);

    /**
     * 根据用户id查询角色
     * 
     * @param userId
     * @return
     */
    List<Role> getRoleListByUserId(Long userId);

    /**
     * 根据用户id查询角色-包含角色的数据权限
     *
     * @param userId 用户id
     * @param includeOrgPrivTree 是否包含组织权限树
     * @return
     */
    List<RoleDto> getRoleInfoListByUserId(Long userId, boolean includeOrgPrivTree);


    /**
     * 查询用户的第一个角色
     *
     * @param userId 用户id
     * @return 角色信息
     */
    RoleDto getFirstRole(Long userId);

    /**
     * 根据用户id查询角色-包含角色的数据权限, 指定的组织权限-不指定组织则默认包含用户所在的组织权限
     *
     * @param userId 用户id
     * @param orgPrivDtos 组织列表
     * @param includeOrgPrivTree 是否包含组织权限树
     * @return
     */
    List<RoleDto> getRoleWithDataPriv(Long userId, List<OrgPrivDto> orgPrivDtos, boolean includeOrgPrivTree);

    /**
     * 创建菜单权限缓存
     */
    void createRoleMenuCache();

    /**
     * 获取菜单缓存
     * @param roleIds
     * @return
     */
    List<String> getMenuCacheByRoles(List<Long> roleIds);

    void setDefaultRole(Long userId);

    void setRole(Long userId, Long roleId);

    void updateUserRole(Long userId, Long roleId);

    void updateUserRole(ImUserDO userInfo, Long roleId);

    void setRole(List<Long> userId, Long roleId);

    void setUsersRole(List<ImUserDO> userList, Long roleId);

    void lockUser(ImUserDO imUserDO);

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
     *
     */
    void deleteOrgPriv(List<Long> roleIds);

    /**
     * 根据已授权的组织列表构建数据权限树。
     * <p>
     * 算法：提取授权ID → 解析 fullPath 反推祖先链 → 以最浅授权深度为裁剪线 →
     * 按 parent_id 组装树并按 sort 排序。授权节点 hasPermission=true，过渡祖先=false。
     *
     * @param orgPrivDtos 已授权的组织列表（扁平结构）
     * @return 权限树根节点列表，结构与登录响应 imOrgPrivs 一致
     */
    List<RoleDataPriv> buildOrgPrivTree(List<OrgPrivDto> orgPrivDtos);
}