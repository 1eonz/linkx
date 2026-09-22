package com.tdtech.cloudcmd.auth.controller;

import com.tdtech.cloudcmd.auth.dto.ImUserPageResult;
import com.tdtech.cloudcmd.auth.dto.ImUserVO;
import com.tdtech.cloudcmd.auth.dto.OrgUserScopeQO;
import com.tdtech.cloudcmd.auth.entity.RoleDataPriv;
import com.tdtech.cloudcmd.auth.service.OrganizationUserService;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.util.json.JsonArray;
import com.tdtech.cloudcmd.util.json.JsonUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

/**
 * 管理员用户组织数据权限接口。
 * <p>
 * 权限来源按用户类型区分（UserInfo.isAdmin）：
 * <ul>
 *   <li>管理员(isAdmin=true)：用户级授权 {@code tb_organization_user}</li>
 *   <li>普通用户(isAdmin=false)：角色级授权，实时查 {@code tb_organization_role} 构建</li>
 * </ul>
 * 人员查询通过警信 RPC 实时拉取，返回全部人员并按权限范围标记 hasPermission。
 *
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/auth/v1/orgUser")
@RequiredArgsConstructor
@Tag(name = "管理员组织数据权限", description = "管理员用户的组织数据权限树及权限范围内人员查询")
public class OrganizationUserController {

    private final OrganizationUserService organizationUserService;

    /**
     * 构建当前登录管理员的组织权限树。
     * <p>
     * 用户ID从登录上下文获取，先从 {@code tb_organization_user} 查出该用户被授权的扁平组织列表，
     * 再复用角色权限树构建算法：解析 fullPath 反推祖先链 → 以最浅授权深度为裁剪线 →
     * 按 parent_id 组装树并按 sort 排序，授权节点 hasPermission=true，过渡祖先=false。
     *
     * @return 权限树 JSON，结构与登录响应 imOrgPrivs 一致；无授权时返回空数组
     */
    @GetMapping("/orgPrivTree")
    @Operation(summary = "构建当前管理员组织权限树", description = "根据登录用户构建数据权限树：超管返回全组织树，管理员(isAdmin=true)按 tb_organization_user 授权记录构建，普通用户(isAdmin=false)按角色级授权构建。传 parentCode/parentId 只返回该父节点下的组织树；都没传时 includeChildren=1 返回全部组织树，=0 返回第一级节点")
    public R<JsonArray> getOrgPrivTree(
            @Parameter(description = "父级组织代码") @RequestParam(required = false,
                    value = "parentCode") String parentCode,
            @Parameter(description = "父级ID") @RequestParam(required = false, value = "parentId") Long parentId,
            @Parameter(description = "是否包含所有子孙节点：0-仅直接子节点，1-含子孙（默认1）") @RequestParam(required = false,
                    defaultValue = "1", value = "includeChildren") Integer includeChildren) {
        List<RoleDataPriv> tree = organizationUserService.buildOrgPrivTreeForCurrentUser(parentCode, parentId, includeChildren);
        return R.success(JsonUtil.convert(tree, JsonArray.class));
    }

    @GetMapping("/user/page")
    @Operation(summary = "权限范围内分页搜索人员", description = "通过警信RPC实时查人员，按权限范围标记hasPermission：true-有权限，false-无权限")
    public R<ImUserPageResult> queryUserInScopePaged(
            @Parameter(name = "pageNum", description = "页码", example = "1") @RequestParam(defaultValue = "1",
                    value = "pageNum") Integer pageNum,
            @Parameter(name = "pageSize", description = "每页数量", example = "10") @RequestParam(defaultValue = "10",
                    value = "pageSize") Integer pageSize,
            OrgUserScopeQO qo) {
        ImUserPageResult result = organizationUserService.queryUserInScopePaged(pageNum, pageSize, qo);
        return R.success(result);
    }

    @GetMapping("/user/list")
    @Operation(summary = "权限范围内搜索人员（不分页）", description = "通过警信RPC实时查人员，按权限范围标记hasPermission")
    public R<List<ImUserVO>> queryUserInScopeList(OrgUserScopeQO qo) {
        List<ImUserVO> voList = organizationUserService.queryUserInScopeList(qo);
        return R.success(voList);
    }
}