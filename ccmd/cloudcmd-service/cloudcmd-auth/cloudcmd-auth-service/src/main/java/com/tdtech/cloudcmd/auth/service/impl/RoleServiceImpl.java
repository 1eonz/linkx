package com.tdtech.cloudcmd.auth.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.tdtech.cloudcmd.auth.entity.*;
import com.tdtech.cloudcmd.auth.controller.admin.vo.OrgPrivDto;
import com.tdtech.cloudcmd.auth.mapper.*;
import com.tdtech.cloudcmd.auth.service.IRoleService;
import com.tdtech.cloudcmd.auth.service.RoleRpcService;
import com.tdtech.cloudcmd.constant.AuthConstants;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.im.jingxin.api.DepartmentRpcApi;
import com.tdtech.cloudcmd.im.jingxin.api.entity.department.OrganizationVO;
import com.tdtech.cloudcmd.msip.aop.LogReport;
import com.tdtech.cloudcmd.msip.aop.LogReportParam;
import com.tdtech.cloudcmd.msip.enums.OperationTypeEnum;
import com.tdtech.cloudcmd.redis.RedisUtil;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.IdWorker;
import com.tdtech.cloudcmd.util.json.JsonArray;
import com.tdtech.cloudcmd.util.json.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * 角色信息表 服务实现类
 * </p>
 *
 * @author zhuangzl
 * @since 2020-06-01
 */
@Service
@Slf4j
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements IRoleService {

    @Resource
    private RoleMapper roleMapper;
    @Resource
    private TrUserRoleMapper trUserRoleMapper;
    @Resource
    private OrganizationRoleMapper organizationRoleMapper;
    @Resource
    private CollabsOrganizationMapper collabsOrganizationMapper;
    @Resource
    private RedisUtil redisUtil;
    @Resource
    private IdWorker idWorker;
    @Resource
    private OAuthLoginServiceImpl oAuthLoginService;
    @Resource
    private RoleDataPrivCacheService roleDataPrivCacheService;
    @DubboReference
    private DepartmentRpcApi departmentRpcApi;

    @DubboReference
    private RoleRpcService roleRpcService;

    @Resource
    private ImUserMapper imUserMapper;

    @Override
    public List<Role> getRoleListByType(Integer type) {
        QueryWrapper<Role> wrapper = new QueryWrapper<>();
        wrapper.eq(Role.TYPE, type);
        return roleMapper.selectList(wrapper);
    }

    @Override
    public List<Role> getRoleListByUserId(Long userId) {
        var roles = roleMapper.getRoleListByUserId(userId);
        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }
        fixRole(roles);
        return roles;
    }

    @Override
    public List<RoleDto> getRoleInfoListByUserId(Long userId, boolean includeOrgPrivTree) {
        var roles = roleMapper.getRoleListByUserId(userId);
        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }
        List<RoleDto> roleDtos = BeanCopyUtils.copyList(roles, RoleDto::new);
        fixDataPermission(roleDtos, includeOrgPrivTree);
        return roleDtos;
    }

    @Override
    public RoleDto getFirstRole(Long userId) {
        List<Role> roles = roleMapper.getRoleListByUserId(userId);
        if (CollectionUtils.isEmpty(roles)) {
            return null;
        }
        RoleDto roleDto = BeanCopyUtils.copyBean(roles.get(0), RoleDto::new);
        Map<Long, List<OrgPrivDto>> orgPrivByRoleId = roleRpcService.getOrgPrivByRoleId(Collections.singletonList(roleDto.getId()));
        if (CollectionUtils.isNotEmpty(orgPrivByRoleId)){
            roleDto.setOrgPrivList(orgPrivByRoleId.get(roleDto.getId()));
        }
        return roleDto;
    }

    @Override
    public List<RoleDto> getRoleWithDataPriv(Long userId, List<OrgPrivDto> orgPrivDtos, boolean includeOrgPrivTree) {
        var roles = roleMapper.getRoleListByUserId(userId);
        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }
        // 不指定组织则默认加上用户自身的组织
        if (CollectionUtils.isEmpty(orgPrivDtos)) {
            List<ImUserDto> imUsers = imUserMapper.listByIds(Collections.singletonList(userId));
            if (CollectionUtils.isEmpty(imUsers)) {
                throw new BusinessException("用户不存在");
            }
            orgPrivDtos = imUsers.stream().filter(u -> u.getDepartmentId() != null).map(a -> {
                OrgPrivDto orgPrivDto = new OrgPrivDto();
                orgPrivDto.setId(a.getDepartmentId());
                orgPrivDto.setCode(a.getDepartmentCode());
                orgPrivDto.setName(a.getDepartmentName());
                return orgPrivDto;
            }).collect(Collectors.toList());
        }
        List<RoleDto> roleDtos = BeanCopyUtils.copyList(roles, RoleDto::new);
        fixDataPermission(roleDtos, orgPrivDtos, includeOrgPrivTree);
        return roleDtos;
    }

    private void fixDataPermission(List<RoleDto> roles, List<OrgPrivDto> orgPrivDtos, boolean includeOrgPrivTree) {
        List<Long> roleIdList = roles.stream().map(RoleDto::getId).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(roleIdList)) {
            return;
        }
        Map<Long, List<OrgPrivDto>> orgPrivByRoleId = roleRpcService.getOrgPrivByRoleId(roleIdList);
        for (RoleDto role : roles) {
            orgPrivDtos = CollectionUtils.isEmpty(orgPrivDtos) ? new ArrayList<>() : orgPrivDtos;
            Set<OrgPrivDto> orgPrivDtosSet = new HashSet<>(orgPrivDtos);
            orgPrivDtosSet.addAll(orgPrivByRoleId.getOrDefault(role.getId(), Collections.emptyList()));
            List<OrgPrivDto> finalOrgPrivDtos = new ArrayList<>(orgPrivDtosSet);
            role.setOrgPrivList(finalOrgPrivDtos);
            if (includeOrgPrivTree) {
                // 混杂了用户权限，无法使用缓存
                List<RoleDataPriv> dataPermissionTree = buildDataPermissionTree(finalOrgPrivDtos);
                role.setImOrgPrivJson(JsonUtil.convert(dataPermissionTree, JsonArray.class));
            }
        }
    }

    private void fixDataPermission(List<RoleDto> roles, boolean includeOrgPrivTree) {
        List<Long> roleIdList = roles.stream().map(RoleDto::getId).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(roleIdList)) {
            return;
        }
        Map<Long, List<OrgPrivDto>> orgPrivByRoleId = roleRpcService.getOrgPrivByRoleId(roleIdList);
        for (RoleDto role : roles) {
            List<OrgPrivDto> orgPrivDtos = orgPrivByRoleId.getOrDefault(role.getId(), Collections.emptyList());
            role.setOrgPrivList(orgPrivDtos);
            if (includeOrgPrivTree) {
                List<RoleDataPriv> dataPermissionTree = roleDataPrivCacheService.getOrBuild(
                        role.getId(), () -> buildDataPermissionTree(orgPrivDtos));
                role.setImOrgPrivJson(JsonUtil.convert(dataPermissionTree, JsonArray.class));
            }
        }
    }

    @Override
    public List<RoleDataPriv> buildOrgPrivTree(List<OrgPrivDto> orgPrivDtos) {
        return buildDataPermissionTree(orgPrivDtos);
    }

    /**
     * 根据已授权的组织列表，构建数据权限树。
     *
     * 算法步骤：
     * 1. 从 orgPrivDtos 中提取所有已授权的组织ID（grantedIds）
     * 2. 预解析 fullPath，计算所有授权节点中最浅的深度（minDepth）
     * 3. 收集所有需要纳入树的节点ID（授权节点 + 它们的祖先节点）
     * 4. 过滤：只保留深度 >= minDepth 的节点，裁掉最浅授权节点之上的祖先
     * 5. 将 Organization 转换为 RoleDataPriv，用 parentId 组装树结构
     *
     * 裁剪策略说明：
     *   以"最浅授权节点的深度"为裁剪线，深度 < minDepth 的祖先全部裁掉，
     *   深度 >= minDepth 的祖先保留（即使它们本身未被授权），确保授权节点之间的路径完整。
     *
     * 示例1（授权节点在同一子树的不同深度）：
     *   组织树:  根(1) → 部门A(11) → 小组B(112) → 岗位C(132)
     *                       └→ 部门D(12)
     *   已授权: [132, 12]
     *   minDepth = 2（节点12的深度，fullPath="1,12"有2个ID）
     *   裁剪: 节点1（深度1 < 2）
     *   保留: 节点11(深度2), 112(深度3), 132(深度4), 12(深度2)
     *   最终树:
     *     部门A(11) [hasPermission=false]
     *     └── 小组B(112) [hasPermission=false]
     *         └── 岗位C(132) [hasPermission=true]
     *     部门D(12) [hasPermission=true]
     *
     * 示例2（授权节点都在深层）：
     *   组织树:  根(1) → 部门A(11) → 小组B(112) → 岗位C(132)
     *   已授权: [132]
     *   minDepth = 4（节点132的深度，fullPath="1,11,112,132"有4个ID）
     *   裁剪: 节点1(深度1), 11(深度2), 112(深度3)
     *   最终树:
     *     岗位C(132) [hasPermission=true]  （单节点，无祖先冗余）
     *
     * @param orgPrivDtos 已授权的组织列表
     * @return 数据权限树，根节点从最浅授权节点的深度开始
     */
    private List<RoleDataPriv> buildDataPermissionTree(List<OrgPrivDto> orgPrivDtos) {
        if (CollectionUtils.isEmpty(orgPrivDtos)) {
            return Collections.emptyList();
        }

        // ===== 第一步：提取所有已授权的组织ID =====
        Set<Long> grantedIds = orgPrivDtos.stream()
                .map(OrgPrivDto::getId)
                .collect(Collectors.toSet());

        // 查询这些已授权组织的详细信息（需要fullPath字段来判断祖先关系和深度）
        List<Organization> grantedOrgs = collabsOrganizationMapper.selectDetailByIds(new ArrayList<>(grantedIds));
        if (CollectionUtils.isEmpty(grantedOrgs)) {
            return Collections.emptyList();
        }

        // ===== 第二步：预解析fullPath + 计算最浅授权深度 =====
        // 将每个组织的 fullPath（如 "1,5,10"）一次性解析为 Set<Long>
        // 同时计算 minDepth：所有授权节点中最浅的深度（fullPath中ID的个数即为深度）
        Map<Long, Set<Long>> orgPathIdsMap = new HashMap<>();
        int minDepth = Integer.MAX_VALUE;
        for (Organization org : grantedOrgs) {
            Set<Long> pathIds = parseFullPathIds(org.getFullPath());
            orgPathIdsMap.put(org.getId(), pathIds);
            // fullPath中ID的个数 = 该节点在组织树中的深度
            // 例如 "1,12" → 2个ID → 深度2；"1,11,112,132" → 4个ID → 深度4
            if (!pathIds.isEmpty()) {
                minDepth = Math.min(minDepth, pathIds.size());
            }
        }

        // ===== 第三步：收集所有需要查询的组织ID =====
        // 包括：所有已授权节点 + 它们的祖先节点（从预解析的pathIds中直接获取）
        Set<Long> allIds = new HashSet<>(grantedIds);
        for (Set<Long> pathIds : orgPathIdsMap.values()) {
            allIds.addAll(pathIds);
        }

        // 批量查询所有相关组织的详细信息
        List<Organization> allOrgs = collabsOrganizationMapper.selectDetailByIds(new ArrayList<>(allIds));
        if (CollectionUtils.isEmpty(allOrgs)) {
            return Collections.emptyList();
        }

        // 为所有组织预解析fullPath（复用解析方法，一次解析多次使用）
        Map<Long, Set<Long>> allOrgPathIdsMap = new HashMap<>();
        for (Organization org : allOrgs) {
            allOrgPathIdsMap.put(org.getId(), parseFullPathIds(org.getFullPath()));
        }

        // 构建 id -> Organization 映射
        Map<Long, Organization> orgMap = allOrgs.stream()
                .collect(Collectors.toMap(Organization::getId, Function.identity(), (v1, v2) -> v1));

        // ===== 第四步：过滤——只保留深度 >= minDepth 的节点 =====
        // 深度 < minDepth 的节点是最浅授权节点之上的祖先，裁掉以避免前端展开大量无关层级
        // 深度 >= minDepth 的节点（包括未被授权的中间节点）保留，确保授权节点之间的路径完整
        Set<Long> keptIds = new HashSet<>();
        for (Organization org : allOrgs) {
            Set<Long> pathIds = allOrgPathIdsMap.get(org.getId());
            // pathIds.size() 即为该节点的深度，深度 >= minDepth 则保留
            if (pathIds != null && pathIds.size() >= minDepth) {
                keptIds.add(org.getId());
            }
        }

        // ===== 第五步：将 Organization 转换为 RoleDataPriv 并组装树结构 =====
        Map<Long, RoleDataPriv> nodeMap = new LinkedHashMap<>();
        for (Organization org : allOrgs) {
            // 只构建保留集合中的节点，裁掉最浅授权节点上方的祖先
            if (!keptIds.contains(org.getId())) {
                continue;
            }
            RoleDataPriv node = new RoleDataPriv();
            node.setId(org.getId());
            node.setCode(org.getCode());
            node.setName(org.getName());
            node.setSort(org.getSort());
            node.setParentId(org.getParentId());
            node.setShortName(org.getShortName());
            node.setFullPath(org.getFullPath());
            node.setFullPathName(org.getFullPathName());
            // hasPermission: 在grantedIds中→true，不在（中间过渡节点）→false
            node.setHasPermission(grantedIds.contains(org.getId()));
            node.setChildren(new ArrayList<>());

            // 填充父节点的编码和名称（用于前端展示）
            if (org.getParentId() != null) {
                Organization parentOrg = orgMap.get(org.getParentId());
                if (parentOrg != null) {
                    node.setParentCode(parentOrg.getCode());
                    node.setParentName(parentOrg.getName());
                }
            }

            nodeMap.put(org.getId(), node);
        }

        // ===== 第六步：用 parentId 将节点挂到父节点的 children 中，形成树结构 =====
        // 如果 parentId 为空 或 父节点不在nodeMap中（被裁掉了），则当前节点是根节点
        List<RoleDataPriv> roots = new ArrayList<>();
        for (RoleDataPriv node : nodeMap.values()) {
            Long parentId = node.getParentId();
            if (parentId == null || !nodeMap.containsKey(parentId)) {
                roots.add(node);
            } else {
                nodeMap.get(parentId).getChildren().add(node);
            }
        }

        // 按 sort 字段递归排序
        sortTree(roots);

        return roots;
    }

    /**
     * 将 fullPath 字符串（如 "1,5,10"）解析为 Long 类型的 Set。
     * 解析一次，后续直接用集合运算，避免循环内反复 split + parse。
     */
    private Set<Long> parseFullPathIds(String fullPath) {
        if (fullPath == null || fullPath.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> ids = new HashSet<>();
        for (String segment : fullPath.split(",")) {
            String trimmed = segment.trim();
            if (!trimmed.isEmpty()) {
                try {
                    ids.add(Long.parseLong(trimmed));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return ids;
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

    private void fixRole(List<Role> roles) {
        for (var role : roles) {
            if (role.getImOrgPrivJson() == null || role.getImOrgPrivJson().isEmpty()) {
                continue;
            }
            var privs = JsonUtil.convert(role.getImOrgPrivJson(), new TypeReference<List<RoleDataPriv>>() {
            });
            var unwraps = privs.stream().flatMap(this::unwrap).collect(Collectors.toList());
            var ids = unwraps.stream().map(RoleDataPriv::getId).collect(Collectors.toList());
            var list = departmentRpcApi.findList(ids);
            for (var unwrap : unwraps) {
                var find = CollectionUtils.find(list, OrganizationVO::getId, unwrap.getId());
                if (find != null) {
                    BeanCopyUtils.copyBean(find, unwrap);
                }
            }
            role.setImOrgPrivJson(JsonUtil.convert(privs, JsonArray.class));
        }
    }

    private Stream<RoleDataPriv> unwrap(RoleDataPriv root) {
        if (root.getChildren() == null || root.getChildren().isEmpty()) {
            return Stream.of(root);
        }
        var childStream = root.getChildren().stream().flatMap(this::unwrap);
        return Stream.concat(Stream.of(root), childStream);
    }

    @Override
    public void createRoleMenuCache() {
        QueryWrapper<Role> wrapper = new QueryWrapper<>();
        wrapper.eq(Role.STATUS, 0);
        List<Role> roles = roleMapper.selectList(wrapper);
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

        }
    }

    @Override
    public List<String> getMenuCacheByRoles(List<Long> roleIds) {
        List<String> menuCache = new ArrayList<>();
        for (int i = 0; i < roleIds.size(); i++) {
            Long roleId = roleIds.get(i);
            String menuData = redisUtil.get(AuthConstants.ACCESSS_PERMISSION_MENU + roleId, String.class);
            List<String> menus = JSONObject.parseArray(menuData, String.class);
            menuCache.addAll(menus);
        }
        return menuCache;
    }

    @Override
    public void setDefaultRole(Long userId) {
        setRole(userId, 6L);
    }



    @Override
    public void setRole(Long userId, Long roleId) {
        var update = trUserRoleMapper.update(null,
            Wrappers.lambdaUpdate(TrUserRole.class).eq(TrUserRole::getUserId, userId)
                .set(TrUserRole::getRoleId, roleId));
        if (update == 0) {
            var trUserRole = new TrUserRole();
            trUserRole.setId(idWorker.nextId());
            trUserRole.setRoleId(roleId);
            trUserRole.setUserId(userId);
            trUserRoleMapper.insert(trUserRole);
        }
        oAuthLoginService.kickOutUserForRPC(userId + "");
    }

    @Override
    public void updateUserRole(Long userId, Long roleId) {
        setRole(userId, roleId);
    }

    @Override
    @LogReport(type = OperationTypeEnum.PERMISSION_UPDATE)
    public void updateUserRole(@LogReportParam ImUserDO userInfo, Long roleId) {
        updateUserRole(userInfo.getId(), roleId);
    }

    @Override
    public void setRole(List<Long> userIds, Long roleId) {
        trUserRoleMapper.delete(Wrappers.lambdaUpdate(TrUserRole.class).in(TrUserRole::getUserId, userIds));
        var collect = userIds.stream().map(uid -> {
            var trUserRole = new TrUserRole();
            trUserRole.setId(idWorker.nextId());
            trUserRole.setRoleId(roleId);
            trUserRole.setUserId(uid);
            return trUserRole;
        }).collect(Collectors.toList());
        trUserRoleMapper.insertBatch(collect);
        for (var userId : userIds) {
            oAuthLoginService.kickOutUserForRPC(userId + "");
        }
    }

    @Override
    @LogReport(type = OperationTypeEnum.PERMISSION_UPDATE)
    public void setUsersRole(@LogReportParam List<ImUserDO> userList, Long roleId) {
        setRole(userList.stream().map(ImUserDO::getId).collect(Collectors.toList()), roleId);
    }

    /**
     * 这个方法目前仅用于切面上报日志
     *
     * @param imUserDO
     */
    @Override
    @LogReport(type = OperationTypeEnum.LOCKED)
    public void lockUser(@LogReportParam(field = "idCard") ImUserDO imUserDO) {
        log.info("用户被冻结了: {}", imUserDO);
    }

    @Override
    public void saveOrgPriv(Long roleId, Long grantUserId, List<OrgPrivDto> orgPrivList) {
        log.info("保存角色{}的组织权限，共{}条", roleId, orgPrivList != null ? orgPrivList.size() : 0);

        roleDataPrivCacheService.evict(roleId);

        Date now = new Date();

        organizationRoleMapper.deleteByRoleId(roleId);

        if (orgPrivList == null || orgPrivList.isEmpty()) {
            return;
        }

        List<OrganizationRole> orgRoles = orgPrivList.stream().map(dto -> {
            OrganizationRole orgRole = new OrganizationRole();
            orgRole.setId(idWorker.nextId());
            orgRole.setRoleId(roleId);
            orgRole.setImOrgId(dto.getId());
            orgRole.setGrantUserId(grantUserId == null ? getSuperAdminUserId() : grantUserId);
            orgRole.setGrantTime(now);
            orgRole.setGmtCreated(now);
            orgRole.setGmtModified(now);
            return orgRole;
        }).collect(Collectors.toList());

        organizationRoleMapper.insertBatch(orgRoles);
    }

    @Override
    public Map<Long, List<OrgPrivDto>> getOrgPrivByRoleId(List<Long> roleIds) {
        log.info("获取角色{}的组织权限列表", roleIds);

        // 1. 根据角色ID列表查询角色-组织关联记录
        List<OrganizationRole> orgRoles = organizationRoleMapper.selectByRoleIds(roleIds);
        if (CollectionUtils.isEmpty(orgRoles)) {
            return Collections.emptyMap();
        }

        // 2. 提取去重的组织ID，批量查询组织详情
        List<Long> orgIds = orgRoles.stream()
                .map(OrganizationRole::getImOrgId)
                .distinct()
                .collect(Collectors.toList());

        List<Organization> organizations = collabsOrganizationMapper.selectByIds(orgIds);
        if (CollectionUtils.isEmpty(organizations)) {
            return Collections.emptyMap();
        }

        // 3. 构建组织ID -> 组织实体的映射，便于后续填充DTO
        Map<Long, Organization> orgMap = organizations.stream().collect(Collectors.toMap(Organization::getId,
                Function.identity(), (v1,v2)->v1));

        // 4. 按角色ID分组，将关联记录转换为OrgPrivDto列表
        return orgRoles.stream().collect(Collectors.groupingBy(
                OrganizationRole::getRoleId,
                Collectors.mapping(orgRole -> {
                    OrgPrivDto dto = new OrgPrivDto();
                    dto.setId(orgRole.getImOrgId());

                    // 从组织映射中填充名称、编码、全路径等字段
                    Organization org = orgMap.get(orgRole.getImOrgId());
                    if (org != null) {
                        dto.setName(org.getName());
                        dto.setCode(org.getCode());
                        dto.setFullPath(org.getFullPathName());
                    }

                    return dto;
                }, Collectors.toList())
        ));
    }

    @Override
    public void deleteOrgPriv(List<Long> roleIds) {
        log.info("删除角色{}的组织权限", roleIds);
        roleDataPrivCacheService.evict(roleIds);
        organizationRoleMapper.deleteByRoleIds(roleIds);
    }

    private Long getSuperAdminUserId() {
        return 1L;
    }
}