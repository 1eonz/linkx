package com.tdtech.cloudcmd.auth.service.impl;

import com.tdtech.cloudcmd.auth.controller.admin.vo.OrgPrivDto;
import com.tdtech.cloudcmd.auth.dto.ImUserPageResult;
import com.tdtech.cloudcmd.auth.dto.ImUserVO;
import com.tdtech.cloudcmd.auth.dto.OrgUserScopeQO;
import com.tdtech.cloudcmd.auth.entity.Organization;
import com.tdtech.cloudcmd.auth.mapper.CollabsOrganizationMapper;
import com.tdtech.cloudcmd.auth.mapper.OrganizationUserMapper;
import com.tdtech.cloudcmd.auth.entity.Role;
import com.tdtech.cloudcmd.auth.entity.RoleDataPriv;
import com.tdtech.cloudcmd.auth.exception.OAuthException;
import com.tdtech.cloudcmd.auth.service.IRoleService;
import com.tdtech.cloudcmd.auth.service.OrganizationUserService;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.i18n.I18nUtil;
import com.tdtech.cloudcmd.im.jingxin.api.CollaborationRpcApi;
import com.tdtech.cloudcmd.im.jingxin.api.DepartmentRpcApi;
import com.tdtech.cloudcmd.im.jingxin.api.ImUserRpcApi;
import com.tdtech.cloudcmd.im.jingxin.api.entity.im.ImUserVo;
import com.tdtech.cloudcmd.im.jingxin.api.entity.virtualUser.UserDepartmentVo;
import com.tdtech.cloudcmd.im.jingxin.api.entity.department.OrganizationVO;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.StringUtils;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tdtech.cloudcmd.auth.enums.CommonErrorEnum.COMMON_ERROR_118;

/**
 * 管理员数据权限范围内组织/人员查询服务实现。
 * <p>
 * 权限来源按用户类型区分（通过 UserInfo.isAdmin）：
 * <ul>
 *   <li>超管(id=1)：全组织树</li>
 *   <li>管理员(isAdmin=true)：用户级授权 {@code tb_organization_user}</li>
 *   <li>普通用户(isAdmin=false)：角色级授权，直接取 {@code UserInfo.imOrgPrivIds}（登录时已从 tb_organization_role 加载）</li>
 * </ul>
 *
 * @author system
 * @since 2024-01-01
 */
@Service
@Slf4j
public class OrganizationUserServiceImpl implements OrganizationUserService {

    /**
     * 超级管理员用户ID（具有一切权限，不受数据权限约束）
     */
    private static final Long SUPER_ADMIN_USER_ID = 1L;

    @Resource
    private CollabsOrganizationMapper collabsOrganizationMapper;

    @Resource
    private OrganizationUserMapper organizationUserMapper;

    @DubboReference
    private CollaborationRpcApi collaborationRpcApi;

    @DubboReference
    private DepartmentRpcApi departmentRpcApi;

    @DubboReference
    private ImUserRpcApi imUserRpcApi;

    @Resource
    private IRoleService roleService;

    @Override
    public ImUserPageResult queryUserInScopePaged(Integer pageNum, Integer pageSize, OrgUserScopeQO qo) {
        // 获取权限范围内的部门ID集合
        List<Long> scopeDeptIds = resolveScopeDeptIds(qo);
        Set<Long> scopeDeptIdSet = scopeDeptIds != null ? new java.util.HashSet<>(scopeDeptIds) : null;

        // 构建查询参数：orgId 指定时传 deptId + includeChildren，否则查全量
        String deptId = qo.getOrgId() != null ? String.valueOf(qo.getOrgId()) : null;
        Integer includeChildren = qo.getOrgId() != null ? (resolveIsChildren(qo) ? 1 : 0) : 1;

        // 调警信 RPC 实时查人员
        com.tdtech.cloudcmd.im.jingxin.api.entity.im.ImUserPageResult rpcResult =
                imUserRpcApi.pageByDepartment(pageNum, pageSize, deptId, includeChildren, qo.getName());

        // 转换为 auth 端分页结果
        ImUserPageResult result = new ImUserPageResult();
        if (rpcResult == null) {
            result.setPageNo(pageNum);
            result.setPageSize(pageSize);
            result.setTotal(0);
            result.setRecords(Collections.emptyList());
            return result;
        }
        result.setPageNo(rpcResult.getPageNo());
        result.setPageSize(rpcResult.getPageSize());
        result.setTotal(rpcResult.getTotal());

        // ImUserVo -> ImUserVO，同时标记 hasPermission 和绑定关系
        if (CollectionUtils.isNotEmpty(rpcResult.getRecords())) {
            Set<Long> boundUserIds = resolveBoundUserIds(qo.getType());
            List<ImUserVO> voList = rpcResult.getRecords().stream()
                    .map(vo -> {
                        ImUserVO userVO = toImUserVO(vo);
                        if (scopeDeptIdSet != null) {
                            userVO.setHasPermission(userVO.getDepartmentId() != null && scopeDeptIdSet.contains(userVO.getDepartmentId()));
                        }
                        if (boundUserIds != null && userVO.getId() != null && boundUserIds.contains(userVO.getId())) {
                            userVO.setIsBinding(1);
                        }
                        return userVO;
                    })
                    .collect(Collectors.toList());
            result.setRecords(voList);
        } else {
            result.setRecords(Collections.emptyList());
        }
        return result;
    }

    @Override
    public List<ImUserVO> queryUserInScopeList(OrgUserScopeQO qo) {
        // 获取权限范围内的部门ID集合
        List<Long> scopeDeptIds = resolveScopeDeptIds(qo);
        Set<Long> scopeDeptIdSet = scopeDeptIds != null ? new java.util.HashSet<>(scopeDeptIds) : null;

        // 构建查询参数
        String deptId = qo.getOrgId() != null ? String.valueOf(qo.getOrgId()) : null;
        Integer includeChildren = qo.getOrgId() != null ? (resolveIsChildren(qo) ? 1 : 0) : 1;

        // 不分页：逐页拉全量
        List<ImUserVo> allUsers = new ArrayList<>();
        int pageNo = 1;
        int batchSize = 500;
        while (true) {
            com.tdtech.cloudcmd.im.jingxin.api.entity.im.ImUserPageResult batch = imUserRpcApi.pageByDepartment(pageNo, batchSize, deptId, includeChildren, qo.getName());
            if (batch == null || CollectionUtils.isEmpty(batch.getRecords())) {
                break;
            }
            allUsers.addAll(batch.getRecords());
            if (batch.getTotal() != null && allUsers.size() >= batch.getTotal()) {
                break;
            }
            pageNo++;
        }

        // ImUserVo -> ImUserVO，同时标记 hasPermission 和绑定关系
        Set<Long> boundUserIds = resolveBoundUserIds(qo.getType());
        return allUsers.stream()
                .map(vo -> {
                    ImUserVO userVO = toImUserVO(vo);
                    if (scopeDeptIdSet != null) {
                        userVO.setHasPermission(userVO.getDepartmentId() != null && scopeDeptIdSet.contains(userVO.getDepartmentId()));
                    }
                    if (boundUserIds != null && userVO.getId() != null && boundUserIds.contains(userVO.getId())) {
                        userVO.setIsBinding(1);
                    }
                    return userVO;
                })
                .collect(Collectors.toList());
    }

    /**
     * ImUserVo (RPC) -> ImUserVO (auth)
     * 从 userDepartments 取主部门填充 departmentId/departmentName。
     */
    private ImUserVO toImUserVO(ImUserVo vo) {
        ImUserVO result = BeanCopyUtils.copyBean(vo, ImUserVO::new);
        // 从 userDepartments 取 isPrimary=true 的作为主部门
        List<UserDepartmentVo> depts = vo.getUserDepartments();
        if (CollectionUtils.isNotEmpty(depts)) {
            UserDepartmentVo primary = depts.stream()
                    .filter(d -> Boolean.TRUE.equals(d.getIsPrimary()))
                    .findFirst().orElse(depts.get(0));
            result.setDepartmentId(primary.getId());
            result.setDepartmentName(primary.getName());
        }
        return result;
    }

    /**
     * 获取协同岗绑定的用户ID集合，type 为 null 时查询所有类型。
     */
    private Set<Long> resolveBoundUserIds(Integer type) {
        return collaborationRpcApi.listBoundUserIds(type);
    }

    @Override
    public List<RoleDataPriv> buildOrgPrivTreeForCurrentUser(String parentCode, Long parentId, Integer includeChildren) {
        UserInfo user = Optional.ofNullable(SecurityUtils.getUser())
                .orElseThrow(() -> new OAuthException(COMMON_ERROR_118.getCode(),
                        I18nUtil.get(COMMON_ERROR_118.getMsg())));
        Long userId = user.getUserId();
        boolean isAdmin = user.isAdmin();
        boolean includeSub = resolveIncludeChildren(includeChildren);

        // 先构建完整权限树，按用户类型区分权限来源：
        // 超管 → 全树；管理员 → tb_organization_user；普通用户 → UserInfo.imOrgPrivIds
        List<RoleDataPriv> fullTree;
        if (isSuperAdmin(userId)) {
            fullTree = buildFullOrgTree();
        } else if (isAdmin) {
            fullTree = buildGrantedOrgTree(userId);
        } else {
            fullTree = buildRolePrivTree(user);
        }

        // 无范围参数：includeSub=1 返回全树，=0 返回第一级节点
        boolean hasParent = StringUtils.isNotBlank(parentCode) || parentId != null;
        if (!hasParent) {
            return includeSub ? fullTree : extractFirstLevel(fullTree);
        }

        // 有范围参数：在权限树中定位 parentCode/parentId 对应节点，返回其子树
        return sliceSubTree(fullTree, parentCode, parentId, includeSub);
    }

    /**
     * 管理员授权树：基于 tb_organization_user 用户级授权记录构建。
     */
    private List<RoleDataPriv> buildGrantedOrgTree(Long userId) {
        List<OrgPrivDto> orgPrivDtos = organizationUserMapper.selectOrgPrivByUserId(userId);
        if (CollectionUtils.isEmpty(orgPrivDtos)) {
            return Collections.emptyList();
        }
        return roleService.buildOrgPrivTree(orgPrivDtos);
    }

    /**
     * 普通用户授权树：实时查库构建（tb_organization_role），修改权限后无需重新登录即可生效。
     * <p>
     * 无任何角色授权时，退化为当前用户所在部门作为唯一权限节点。
     */
    private List<RoleDataPriv> buildRolePrivTree(UserInfo user) {
        Long userId = user.getUserId();
        // 1. 获取用户角色列表
        List<Role> roles = roleService.getRoleListByUserId(userId);
        if (CollectionUtils.isEmpty(roles)) {
            return buildOwnDeptTree(user);
        }
        // 2. 获取角色关联的组织权限
        List<Long> roleIds = roles.stream().map(Role::getId).collect(Collectors.toList());
        Map<Long, List<OrgPrivDto>> orgPrivMap = roleService.getOrgPrivByRoleId(roleIds);
        if (orgPrivMap == null || orgPrivMap.isEmpty()) {
            return buildOwnDeptTree(user);
        }
        // 3. 合并所有角色的组织权限（按组织ID去重）
        Map<Long, OrgPrivDto> merged = new LinkedHashMap<>();
        for (List<OrgPrivDto> privs : orgPrivMap.values()) {
            if (CollectionUtils.isNotEmpty(privs)) {
                for (OrgPrivDto dto : privs) {
                    if (dto.getId() != null) {
                        merged.putIfAbsent(dto.getId(), dto);
                    }
                }
            }
        }
        if (merged.isEmpty()) {
            return buildOwnDeptTree(user);
        }
        // 4. 构建权限树
        return roleService.buildOrgPrivTree(new ArrayList<>(merged.values()));
    }

    /**
     * 构建当前用户所在部门的权限树（仅含自身部门一个节点）。
     */
    private List<RoleDataPriv> buildOwnDeptTree(UserInfo user) {
        Long orgId = user.getOrganizationId();
        if (orgId == null) {
            return Collections.emptyList();
        }
        List<Organization> orgs = collabsOrganizationMapper.selectByIds(Collections.singletonList(orgId));
        if (CollectionUtils.isEmpty(orgs)) {
            return Collections.emptyList();
        }
        Organization org = orgs.get(0);
        RoleDataPriv node = new RoleDataPriv();
        node.setId(org.getId());
        node.setCode(org.getCode());
        node.setName(org.getName());
        node.setSort(org.getSort());
        node.setParentId(org.getParentId());
        node.setShortName(org.getShortName());
        node.setFullPath(org.getFullPath());
        node.setFullPathName(org.getFullPathName());
        // 填充父节点编码和名称（与 RoleServiceImpl.buildDataPermissionTree 口径一致）
        if (org.getParentId() != null) {
            List<Organization> parentOrgs = collabsOrganizationMapper.selectByIds(
                    Collections.singletonList(org.getParentId()));
            if (CollectionUtils.isNotEmpty(parentOrgs)) {
                node.setParentCode(parentOrgs.get(0).getCode());
                node.setParentName(parentOrgs.get(0).getName());
            }
        }
        node.setHasPermission(true);
        node.setChildren(new ArrayList<>());
        List<RoleDataPriv> result = new ArrayList<>(1);
        result.add(node);
        return result;
    }

    /**
     * 从权限树中提取所有节点ID（递归遍历 children）。
     */
    private List<Long> extractAllIds(List<RoleDataPriv> tree) {
        if (CollectionUtils.isEmpty(tree)) {
            return Collections.emptyList();
        }
        List<Long> ids = new ArrayList<>();
        for (RoleDataPriv node : tree) {
            if (node.getId() != null) {
                ids.add(node.getId());
            }
            if (CollectionUtils.isNotEmpty(node.getChildren())) {
                ids.addAll(extractAllIds(node.getChildren()));
            }
        }
        return ids;
    }

    /**
     * 只取第一级节点（去掉其 children），用于"仅直接子节点"场景。
     */
    private List<RoleDataPriv> extractFirstLevel(List<RoleDataPriv> tree) {
        if (CollectionUtils.isEmpty(tree)) {
            return Collections.emptyList();
        }
        List<RoleDataPriv> result = new ArrayList<>(tree.size());
        for (RoleDataPriv node : tree) {
            RoleDataPriv shallow = shallowCopy(node);
            shallow.setChildren(new ArrayList<>());
            result.add(shallow);
        }
        return result;
    }

    /**
     * 在已构建的权限树中按 parentCode/parentId 定位父节点：
     * - includeSub=true：返回该父节点及其所有子孙（父节点作为根）
     * - includeSub=false：仅返回该父节点的直接子节点（不带 children）
     */
    private List<RoleDataPriv> sliceSubTree(List<RoleDataPriv> tree, String parentCode, Long parentId,
                                            boolean includeSub) {
        RoleDataPriv parent = findNode(tree, parentCode, parentId);
        if (parent == null) {
            return Collections.emptyList();
        }
        if (includeSub) {
            // 父节点作为根返回（含其整棵子树）
            RoleDataPriv root = shallowCopy(parent);
            List<RoleDataPriv> result = new ArrayList<>(1);
            result.add(root);
            return result;
        }
        // 仅直接子节点，不带 children
        return extractFirstLevel(parent.getChildren());
    }

    /**
     * 深度优先在权限树中查找 code 或 id 匹配的节点。
     * <p>
     * parentCode 与 parentId 只生效一个：parentCode 非空时仅按 code 匹配，忽略 parentId；
     * parentCode 为空时才按 parentId 匹配。
     */
    private RoleDataPriv findNode(List<RoleDataPriv> tree, String parentCode, Long parentId) {
        if (CollectionUtils.isEmpty(tree)) {
            return null;
        }
        boolean useCode = StringUtils.isNotBlank(parentCode);
        for (RoleDataPriv node : tree) {
            boolean match = useCode
                    ? parentCode.equals(node.getCode())
                    : parentId != null && parentId.equals(node.getId());
            if (match) {
                return node;
            }
            RoleDataPriv found = findNode(node.getChildren(), parentCode, parentId);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * 浅拷贝节点（保留所有字段，children 引用原列表），用于把树中某节点提升为根。
     */
    private RoleDataPriv shallowCopy(RoleDataPriv src) {
        RoleDataPriv dst = new RoleDataPriv();
        dst.setId(src.getId());
        dst.setCode(src.getCode());
        dst.setName(src.getName());
        dst.setSort(src.getSort());
        dst.setHasPermission(src.isHasPermission());
        dst.setFullPath(src.getFullPath());
        dst.setParentId(src.getParentId());
        dst.setShortName(src.getShortName());
        dst.setGmtCreated(src.getGmtCreated());
        dst.setParentCode(src.getParentCode());
        dst.setParentName(src.getParentName());
        dst.setGmtModified(src.getGmtModified());
        dst.setFullPathCode(src.getFullPathCode());
        dst.setFullPathName(src.getFullPathName());
        dst.setChildren(src.getChildren());
        return dst;
    }

    private boolean resolveIncludeChildren(Integer includeChildren) {
        return includeChildren != null && includeChildren == 1;
    }

    /**
     * 构建全组织树（超管用）。
     * <p>
     * 通过 DepartmentRpcApi.findRoot 拿根组织，再 queryDepartmentForList(rootCode) 拉全量组织，
     * 转为 RoleDataPriv 树，全部节点 hasPermission=true。
     * 不做授权裁剪，整棵树可见且全部有权限。
     */
    private List<RoleDataPriv> buildFullOrgTree() {
        OrganizationVO root = departmentRpcApi.findRoot();
        if (root == null || StringUtils.isBlank(root.getCode())) {
            return Collections.emptyList();
        }
        List<OrganizationVO> allOrgs = departmentRpcApi.queryDepartmentForList(root.getCode());
        if (CollectionUtils.isEmpty(allOrgs)) {
            // RPC 无结果时退化为仅根节点
            return Collections.singletonList(toRoleDataPriv(root));
        }
        // flat 列表 -> 树
        Map<Long, RoleDataPriv> nodeMap = new LinkedHashMap<>();
        for (OrganizationVO org : allOrgs) {
            nodeMap.put(org.getId(), toRoleDataPriv(org));
        }
        List<RoleDataPriv> roots = new ArrayList<>();
        for (RoleDataPriv node : nodeMap.values()) {
            Long parentId = node.getParentId();
            if (parentId == null || !nodeMap.containsKey(parentId)) {
                roots.add(node);
            } else {
                nodeMap.get(parentId).getChildren().add(node);
            }
        }
        sortTree(roots);
        return roots;
    }

    /**
     * OrganizationVO -> RoleDataPriv，全部 hasPermission=true（超管全权）。
     */
    private RoleDataPriv toRoleDataPriv(OrganizationVO org) {
        RoleDataPriv node = new RoleDataPriv();
        node.setId(org.getId());
        node.setCode(org.getCode());
        node.setName(org.getName());
        node.setSort(org.getSort());
        node.setParentId(org.getParentId());
        node.setShortName(org.getShortName());
        node.setFullPath(org.getFullPath());
        node.setFullPathName(org.getFullPathName());
        node.setParentCode(org.getParentCode());
        node.setParentName(org.getParentName());
        node.setHasPermission(true);
        node.setChildren(new ArrayList<>());
        return node;
    }

    private void sortTree(List<RoleDataPriv> nodes) {
        if (CollectionUtils.isEmpty(nodes)) {
            return;
        }
        nodes.sort(Comparator.comparingInt(n -> n.getSort() != null ? n.getSort() : Integer.MAX_VALUE));
        for (RoleDataPriv node : nodes) {
            sortTree(node.getChildren());
        }
    }

    /**
     * 解析当前查询的有效部门ID范围。
     * <p>
     * 权限边界按用户类型区分：
     * <ul>
     *   <li>超管：不受数据权限约束</li>
     *   <li>管理员(isAdmin=true)：查 tb_organization_user 取用户级授权组织</li>
     *   <li>普通用户(isAdmin=false)：直接取 UserInfo.imOrgPrivIds（角色级授权）</li>
     * </ul>
     * 返回 null 表示不限部门；返回空列表表示无可见部门。
     *
     * @param qo 查询条件
     * @return 部门ID范围
     */
    private List<Long> resolveScopeDeptIds(OrgUserScopeQO qo) {
        UserInfo user = Optional.ofNullable(SecurityUtils.getUser())
                .orElseThrow(() -> new OAuthException(COMMON_ERROR_118.getCode(),
                        I18nUtil.get(COMMON_ERROR_118.getMsg())));
        Long userId = user.getUserId();
        boolean isAdmin = user.isAdmin();

        // 超管：不受数据权限约束
        if (isSuperAdmin(userId)) {
            if (qo.getOrgId() == null) {
                return null;
            }
            return expandOrgSubTree(qo.getOrgId(), resolveIsChildren(qo));
        }

        // 按用户类型取授权组织ID列表
        List<Long> privIds;
        if (isAdmin) {
            privIds = loadGrantedOrgIds(userId);
        } else {
            // 从 imOrgPrivs 树中提取所有组织ID（兼容冀中分支无 imOrgPrivIds 字段）
            List<RoleDataPriv> privTree = buildRolePrivTree(user);
            privIds = extractAllIds(privTree);
            // 普通用户无任何授权时，退化为当前用户所在部门
            if (CollectionUtils.isEmpty(privIds) && user.getOrganizationId() != null) {
                privIds = Collections.singletonList(user.getOrganizationId());
            }
        }
        if (CollectionUtils.isEmpty(privIds)) {
            return Collections.emptyList();
        }
        if (qo.getOrgId() == null) {
            return privIds;
        }
        // 指定 orgId：展开子树后与授权范围取交集
        List<Long> orgSubTreeIds = expandOrgSubTree(qo.getOrgId(), resolveIsChildren(qo));
        if (orgSubTreeIds.isEmpty()) {
            return Collections.emptyList();
        }
        return orgSubTreeIds.stream()
                .filter(privIds::contains)
                .collect(Collectors.toList());
    }

    /**
     * 超管判定：约定 id=1 为超级管理员。
     */
    private boolean isSuperAdmin(Long userId) {
        return SUPER_ADMIN_USER_ID.equals(userId);
    }

    /**
     * 主动查询用户在 tb_organization_user 的授权组织ID列表（扁平，不含子树）。
     */
    private List<Long> loadGrantedOrgIds(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        List<OrgPrivDto> orgPrivDtos = organizationUserMapper.selectOrgPrivByUserId(userId);
        if (CollectionUtils.isEmpty(orgPrivDtos)) {
            return Collections.emptyList();
        }
        return orgPrivDtos.stream()
                .map(OrgPrivDto::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 展开组织子树，返回该组织（含可选子部门）的ID列表。
     */
    private List<Long> expandOrgSubTree(Long orgId, boolean includeChildren) {
        if (orgId == null) {
            return Collections.emptyList();
        }
        if (!includeChildren) {
            return new ArrayList<>(List.of(orgId));
        }
        // orgId -> code（子树展开 RPC 入参为 code）
        List<Organization> orgs = collabsOrganizationMapper.selectByIds(Collections.singletonList(orgId));
        if (CollectionUtils.isEmpty(orgs)) {
            return new ArrayList<>(List.of(orgId));
        }
        String code = orgs.get(0).getCode();
        if (StringUtils.isBlank(code)) {
            return new ArrayList<>(List.of(orgId));
        }
        List<OrganizationVO> orgList = departmentRpcApi.queryDepartmentForList(code);
        if (CollectionUtils.isEmpty(orgList)) {
            return new ArrayList<>(List.of(orgId));
        }
        List<Long> ids = orgList.stream()
                .map(OrganizationVO::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (!ids.contains(orgId)) {
            ids.add(orgId);
        }
        return ids;
    }

    private boolean resolveIsChildren(OrgUserScopeQO qo) {
        return qo.getIsChildren() == null || qo.getIsChildren() == 1;
    }
}