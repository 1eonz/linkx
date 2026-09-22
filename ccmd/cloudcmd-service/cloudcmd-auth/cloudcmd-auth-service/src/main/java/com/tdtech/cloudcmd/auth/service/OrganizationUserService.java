package com.tdtech.cloudcmd.auth.service;

import com.tdtech.cloudcmd.auth.dto.ImUserPageResult;
import com.tdtech.cloudcmd.auth.dto.ImUserVO;
import com.tdtech.cloudcmd.auth.dto.OrgUserScopeQO;
import com.tdtech.cloudcmd.auth.entity.RoleDataPriv;

import java.util.List;

/**
 * 管理员数据权限范围内的组织/人员查询服务。
 * <p>
 * 权限来源：当前登录管理员的 imOrgPrivIds（tb_organization_user 授权的组织ID，扁平）。
 *
 * @author system
 * @since 2024-01-01
 */
public interface OrganizationUserService {

    /**
     * 构建当前登录管理员的组织权限树。
     * <p>
     * 超级管理员（userId=1）返回全组织树（全部 hasPermission=true）；
     * 普通管理员按 tb_organization_user 授权记录构建，授权节点 hasPermission=true，过渡祖先=false。
     * <p>
     * 范围控制：
     * <ul>
     *   <li>传了 parentCode 或 parentId：只返回该父节点下的组织树</li>
     *   <li>都没传 + includeChildren=1：返回全部组织树</li>
     *   <li>都没传 + includeChildren=0：返回第一级节点（根的直接子节点）</li>
     * </ul>
     * includeChildren=1 时含所有子孙节点；=0 时仅直接子节点。
     *
     * @param parentCode      父级组织代码（与 parentId 二选一，优先 parentCode）
     * @param parentId        父级组织ID
     * @param includeChildren 是否包含所有子孙节点：0-仅直接子节点，1-含子孙（默认1）
     * @return 权限树根节点列表，无数据时返回空列表
     */
    List<RoleDataPriv> buildOrgPrivTreeForCurrentUser(String parentCode, Long parentId, Integer includeChildren);

    /**
     * 构建当前登录管理员的组织权限树（全树，兼容旧调用）。
     */
    default List<RoleDataPriv> buildOrgPrivTreeForCurrentUser() {
        return buildOrgPrivTreeForCurrentUser(null, null, 1);
    }

    /**
     * 在管理员数据权限范围内分页搜索人员（实时查警信）。
     * <p>
     * 通过 RPC 调警信 userPageByDepartment 接口拉取人员，转为 ImUserVO 后按权限范围标记 hasPermission。
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @param qo       搜索条件
     * @return 分页结果（含 hasPermission 标记）
     */
    ImUserPageResult queryUserInScopePaged(Integer pageNum, Integer pageSize, OrgUserScopeQO qo);

    /**
     * 在管理员数据权限范围内搜索人员（不分页，实时查警信）。
     *
     * @param qo 搜索条件
     * @return 人员列表（含 hasPermission 标记）
     */
    List<ImUserVO> queryUserInScopeList(OrgUserScopeQO qo);
}