package com.tdtech.cloudcmd.admin.resource.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.admin.exception.AdminErrorEnum;
import com.tdtech.cloudcmd.admin.exception.AdminException;
import com.tdtech.cloudcmd.admin.resource.entity.Menu;
import com.tdtech.cloudcmd.admin.resource.entity.PermissionDiff;
import com.tdtech.cloudcmd.admin.resource.entity.Role;
import com.tdtech.cloudcmd.admin.resource.entity.TrUserRole;
import com.tdtech.cloudcmd.admin.resource.entity.vo.AdminRoleVO;
import com.tdtech.cloudcmd.admin.resource.entity.co.RoleCreateCO;
import com.tdtech.cloudcmd.admin.resource.entity.co.RoleUpdateCO;
import com.tdtech.cloudcmd.admin.resource.entity.qo.RoleQO;
import com.tdtech.cloudcmd.admin.resource.mapper.RoleMapper;
import com.tdtech.cloudcmd.admin.resource.mapper.TrUserRoleMapper;
import com.tdtech.cloudcmd.admin.resource.service.IRoleService;
import com.tdtech.cloudcmd.admin.util.PermissionComparatorUtil;
import com.tdtech.cloudcmd.auth.controller.admin.vo.OrgPrivDto;
import com.tdtech.cloudcmd.auth.dto.ImUserDto;
import com.tdtech.cloudcmd.auth.service.IIMUserRPCService;
import com.tdtech.cloudcmd.auth.service.RoleRpcService;
import com.tdtech.cloudcmd.bean.PageResult;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.i18n.I18nUtil;
import com.tdtech.cloudcmd.msip.aop.LogReport;
import com.tdtech.cloudcmd.msip.aop.LogReportParam;
import com.tdtech.cloudcmd.msip.constant.MSIPConstant;
import com.tdtech.cloudcmd.msip.entity.OperationLog;
import com.tdtech.cloudcmd.msip.enums.OperationTypeEnum;
import com.tdtech.cloudcmd.msip.util.ReportUtil;
import com.tdtech.cloudcmd.encryptor.service.EncryptionService;
import com.tdtech.cloudcmd.redis.RedisUtil;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.IdWorker;
import com.tdtech.cloudcmd.util.json.JsonArray;
import com.tdtech.cloudcmd.util.json.JsonObject;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tdtech.cloudcmd.admin.exception.AdminErrorEnum.COMMON_ERROR_518;
import static com.tdtech.cloudcmd.constant.AuthConstants.ACCESSS_PERMISSION_UPDATE_MENU;

/**
 * <p>
 * 角色信息表 服务实现类
 * </p>
 *
 * @author mWX556161
 * @since 2020-06-23
 */
@Service
@Slf4j
public class RoleServiceImpl implements IRoleService {

    private static final String ROLE2_NAME_I18N = "ENG_ROLE_2";
    private static final String ROLE3_NAME_I18N = "ENG_ROLE_3";

    private final String PREFIX = "ENG_";

    private final String EMPTY = "空";

    @Resource
    private GlobalsServiceImpl globalsServiceImpl;

    @Resource
    private RoleMapper roleMapper;
    @Resource
    private TrUserRoleMapper userRoleMapper;
    @Resource
    private IdWorker idWorker;
    @Resource
    private MenuServiceImpl menuService;
    @Resource
    private RedisUtil redisUtil;

    @Resource
    private ReportUtil reportUtil;
    
    @DubboReference
    private IIMUserRPCService imUserRPCService;

    @DubboReference
    private RoleRpcService roleRpcService;

    @Autowired
    private EncryptionService encryptionService;

    @Override
    public PageResult<AdminRoleVO> listRole(RoleQO roleVo) {
        var engRole2 = I18nUtil.get(ROLE2_NAME_I18N);
        var engRole3 = I18nUtil.get(ROLE3_NAME_I18N);
        var rolePage = new Page<Role>(roleVo.getPageNum(), roleVo.getPageSize());
        var page = roleMapper.selectPage(rolePage, Wrappers.<Role>lambdaQuery().ne(Role::getType, 0)
                .and(roleVo.getName() != null && !roleVo.getName().isBlank(),
                        w -> w.like(Role::getName, roleVo.getName()).or()
                                .eq(engRole2.contains(roleVo.getName()), Role::getName, ROLE2_NAME_I18N).or()
                                .eq(engRole3.contains(roleVo.getName()), Role::getName, ROLE3_NAME_I18N)));
        if (page.getRecords() != null) {
            for (var record : page.getRecords()) {
                record.setName(I18nUtil.get(record.getName()));
            }
        }

        List<AdminRoleVO> voList = new ArrayList<>();
        if (page.getRecords() != null) {
            for (var record : page.getRecords()) {
                AdminRoleVO vo = convertToVO(record);
                voList.add(vo);
            }
        }
        // 填充数据权限信息
        fixDataPermission(voList);

        var pageResult = new PageResult<AdminRoleVO>();
        pageResult.setPages(page.getPages());
        pageResult.setSize(page.getSize());
        pageResult.setCurrent(page.getCurrent());
        pageResult.setTotal(page.getTotal());
        pageResult.setRecords(voList);
        return pageResult;
    }

    private void fixDataPermission(List<AdminRoleVO> voList) {
        if (CollectionUtils.isEmpty(voList)){
            return;
        }
        Map<Long, List<OrgPrivDto>> orgPrivByRoleId = roleRpcService.getOrgPrivByRoleId(
                voList.stream().map(AdminRoleVO::getId).collect(Collectors.toList()));
        if (CollectionUtils.isNotEmpty(orgPrivByRoleId)){
            voList.forEach(vo -> vo.setOrgPrivList(orgPrivByRoleId.getOrDefault(vo.getId(), Collections.emptyList())));
        }
    }

    private AdminRoleVO convertToVO(Role role) {
        AdminRoleVO vo = new AdminRoleVO();
        vo.setId(role.getId());
        vo.setName(role.getName());
        vo.setType(role.getType());
        vo.setIccPrivJson(role.getIccPrivJson());
        vo.setCappPrivJson(role.getCappPrivJson());
        vo.setAdminPrivJson(role.getAdminPrivJson());
        vo.setStatus(role.getStatus());
        vo.setGmtCreated(role.getGmtCreated());
        vo.setGmtModified(role.getGmtModified());
        return vo;
    }

    @Override
    public Role createRole(@LogReportParam RoleCreateCO roleCreateCO) {
        OperationLog operationLog = new OperationLog(OperationTypeEnum.ROLE_INSERT);
        try {
            roleCreateCO.setType(3);
            var roles = roleMapper.selectList(
                    Wrappers.<Role>lambdaQuery().in(Role::getName, roleCreateCO.getName(), ROLE2_NAME_I18N, ROLE3_NAME_I18N));
            var any = Optional.ofNullable(roles).stream().flatMap(Collection::stream).map(Role::getName).map(I18nUtil::get)
                    .filter(roleCreateCO.getName()::equals).findAny();
            if (any.isPresent()) {
                throw new AdminException(AdminErrorEnum.COMMON_ERROR_537.getCode(),
                        AdminErrorEnum.COMMON_ERROR_537.getMsg());
            }
            var role = BeanCopyUtils.copyBean(roleCreateCO, Role::new);
            role.setId(idWorker.nextId());
            fixAdminPrivilege(role.getAdminPrivJson());
            log.info("data:{}", role);
            roleMapper.insert(role);
            return role;
        } catch (Exception e) {
            operationLog.setStatus(MSIPConstant.OPERATION_FAILURE);
            // 抛出去，不影响现有的业务
            throw e;
        } finally {
            UserInfo user = SecurityUtils.getUser();
            if (Objects.nonNull(user)) {
                operationLog.setOperator(user.getUserName());
            }
            String detail = formatRoleDetail(roleCreateCO.getIccPrivJson(), roleCreateCO.getCappPrivJson(), roleCreateCO.getAdminPrivJson(), roleCreateCO.getOrgPrivList());
            operationLog.setOperation(String.format(operationLog.getOperation(), roleCreateCO.getName(), detail));
            reportUtil.saveOperationLog(operationLog);
        }
    }

    private String buildDiff(Role oldRole, Role newRole) {
        List<String> changeList = new ArrayList<>();
        try {
            String oldData = JSONObject.toJSONString(oldRole);
            String newData = JSONObject.toJSONString(newRole);
            List<PermissionDiff> diffs = PermissionComparatorUtil.compare(
                    oldData,
                    newData
            );
            if (CollectionUtils.isEmpty(diffs)) {
                return "没有变更";
            }
            List<String> includeMenuList = List.of("iccPrivJson", "cappPrivJson", "adminPrivJson");
            List<String> allMenuIds = diffs.stream()
                    .filter(diff -> includeMenuList.contains(diff.getFieldName()))
                    .flatMap(diff -> Stream.of(diff.getOldValue(), diff.getNewValue())) // 合并 old 和 new
                    .filter(Objects::nonNull)
                    .flatMap(obj -> obj instanceof Collection
                            ? ((Collection<?>) obj).stream()  // 如果是集合，展开流
                            : Stream.of(obj))                  // 如果是单值，转为单元素流
                    .map(Object::toString)
                    .distinct()
                    .collect(Collectors.toList());

            Map<Long, String> menuMap = new HashMap<>();
            if (CollectionUtils.isNotEmpty(allMenuIds)) {
                List<Menu> menuList = menuService.list(Wrappers.<Menu>lambdaQuery().in(Menu::getId, allMenuIds));
                menuList.forEach(menu -> {
                    String name = menu.getName();
                    if (StringUtils.isNotBlank(name) && name.startsWith(PREFIX)) {
                        menu.setName(I18nUtil.get(name));
                    }
                });
                menuMap.putAll(menuList.stream().collect(Collectors.toMap(Menu::getId, Menu::getName)));
            }

            Map<String, String> oldDataMap = diffs.stream().filter(diff -> includeMenuList.contains(diff.getFieldName()))
                    .collect(Collectors.toMap(PermissionDiff::getFieldName, diff -> {
                        Object oldValue = diff.getOldValue();
                        if (Objects.nonNull(oldValue) && oldValue instanceof Collection) {
                            // 原来的老数据
                            return ((Collection<String>) oldValue).stream().map(Long::parseLong).map(menuMap::get).collect(Collectors.joining(","));
                        }
                        return EMPTY;
                    }));
            Map<String, String> newDataMap = diffs.stream().filter(diff -> includeMenuList.contains(diff.getFieldName()))
                    .collect(Collectors.toMap(PermissionDiff::getFieldName, diff -> {
                        Object newValue = diff.getNewValue();
                        if (Objects.nonNull(newValue) && newValue instanceof Collection) {
                            // 原来的老数据
                            return ((Collection<String>) newValue).stream().map(Long::parseLong).map(menuMap::get).collect(Collectors.joining(","));
                        }
                        return EMPTY;
                    }));

            String iccPrivJsonOldChanges = oldDataMap.get("iccPrivJson");
            String iccPrivJsonNewChanges = newDataMap.get("iccPrivJson");
            boolean iccPrivJsonHasChanged = StringUtils.isNotBlank(iccPrivJsonOldChanges) || StringUtils.isNotBlank(iccPrivJsonNewChanges);
            if (iccPrivJsonHasChanged) {
                changeList.add(String.format(getTitle()+"客户端：%s-->%s", iccPrivJsonOldChanges, iccPrivJsonNewChanges));
            }
            String cappPrivJsonOldChanges = oldDataMap.get("cappPrivJson");
            String cappPrivJsonNewChanges = newDataMap.get("cappPrivJson");
            boolean cappPrivJsonHasChanged = StringUtils.isNotBlank(cappPrivJsonOldChanges) || StringUtils.isNotBlank(cappPrivJsonNewChanges);
            if (cappPrivJsonHasChanged) {
                changeList.add(String.format(getTitle()+"H5：%s-->%s", cappPrivJsonOldChanges, cappPrivJsonNewChanges));
            }
            String adminPrivJsonOldChanges = oldDataMap.get("adminPrivJson");
            String adminPrivJsonNewChanges = newDataMap.get("adminPrivJson");
            boolean adminPrivJsonHasChanged = StringUtils.isNotBlank(adminPrivJsonOldChanges) || StringUtils.isNotBlank(adminPrivJsonNewChanges);
            if (adminPrivJsonHasChanged) {
                changeList.add(String.format(getTitle()+"后台管理系统：%s-->%s", adminPrivJsonOldChanges, adminPrivJsonNewChanges));
            }

            String permissionChange = diffs.stream().filter(diff -> "imOrgPrivJson".equals(diff.getFieldName()))
                    .map(diff -> {
                        String oldChecked = buildDeptNames(diff.getOldValue());
                        String newChecked = buildDeptNames(diff.getNewValue());
                        return String.format("%s-->%s", oldChecked, newChecked);
                    }).collect(Collectors.joining(","));
            if (StringUtils.isNotBlank(permissionChange)) {
                changeList.add(String.format("数据权限：%s",permissionChange));
            }
        } catch (Exception e) {
            log.error("buildDiff error: {}", e.getMessage());
        }
        return changeList.stream().collect(Collectors.joining(","));
    }

    private String buildDeptNames(Object value) {
        if (Objects.nonNull(value) && value instanceof Collection && CollectionUtils.isNotEmpty((Collection<Map<String, String>>) value)) {
            return ((Collection<Map<String, String>>) value).stream().map(data -> data.get("name")).collect(Collectors.joining(","));
        }
        return EMPTY;
    }

    private String formatRoleDetail(List<String> iccPrivJson, List<String> cappPrivJson, List<String> adminPrivJson, List<OrgPrivDto> dataPermission) {
        List<String> menuIdList = new ArrayList<>();
        Map<Long, String> menuMap = new HashMap<>();
        List<String> detailList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(iccPrivJson)) {
            menuIdList.addAll(iccPrivJson);
        }
        if (CollectionUtils.isNotEmpty(cappPrivJson)) {
            menuIdList.addAll(cappPrivJson);
        }
        if (CollectionUtils.isNotEmpty(adminPrivJson)) {
            menuIdList.addAll(adminPrivJson);
        }
        if (CollectionUtils.isNotEmpty(menuIdList)) {
            List<Menu> menuList = menuService.list(Wrappers.<Menu>lambdaQuery().in(Menu::getId, menuIdList));
            menuList.forEach(menu -> {
                String name = menu.getName();
                if (StringUtils.isNotBlank(name) && name.startsWith(PREFIX)) {
                    menu.setName(I18nUtil.get(name));
                }
            });
            menuMap.putAll(menuList.stream().collect(Collectors.toMap(Menu::getId, Menu::getName)));
        }

        if (CollectionUtils.isNotEmpty(iccPrivJson)) {
            String iccNames = iccPrivJson.stream().map(Long::parseLong).map(menuMap::get).collect(Collectors.joining(","));
            detailList.add(String.format(getTitle()+"客户端: %s", iccNames));
        }
        if (CollectionUtils.isNotEmpty(cappPrivJson)) {
            String cappNames = cappPrivJson.stream().map(Long::parseLong).map(menuMap::get).collect(Collectors.joining(","));
            detailList.add(String.format(getTitle()+"H5: %s", cappNames));
        }
        if (CollectionUtils.isNotEmpty(adminPrivJson)) {
            String adminNames = adminPrivJson.stream().map(Long::parseLong).map(menuMap::get).collect(Collectors.joining(","));
            detailList.add(String.format(getTitle()+"后台管理系统: %s", adminNames));
        }
        List<String> deptNameList = formatDeptName(dataPermission);
        if (CollectionUtils.isNotEmpty(deptNameList)) {
            String deptNames = deptNameList.stream().collect(Collectors.joining(","));
            detailList.add(String.format("数据权限: %s", deptNames));
        }
        return detailList.stream().collect(Collectors.joining(","));
    }

    private List<String> formatDeptName(List<OrgPrivDto> dataPermission) {
        if (CollectionUtils.isEmpty(dataPermission)) {
            return Collections.emptyList();
        }
        List<String> resultList = new ArrayList<>();
        for (OrgPrivDto orgPrivDto : dataPermission) {
            resultList.add(orgPrivDto.getName());
        }
        return resultList;
    }

    @Override
    public void updateRole(@LogReportParam RoleUpdateCO roleUpdateCO, Role old) {
        OperationLog operationLog = new OperationLog(OperationTypeEnum.ROLE_UPDATE);
        var role = BeanCopyUtils.copyBean(roleUpdateCO, Role::new);
        try {
            fixAdminPrivilege(role.getAdminPrivJson());

            roleMapper.updateById(role);
        } catch (Exception e) {
            operationLog.setStatus(MSIPConstant.OPERATION_FAILURE);
            // 抛出去，不影响现有的业务
            throw e;
        } finally {
            UserInfo user = SecurityUtils.getUser();
            if (Objects.nonNull(user)) {
                operationLog.setOperator(user.getUserName());
            }
//            String detail = formatRoleDetail(roleUpdateCO.getIccPrivJson(), roleUpdateCO.getCappPrivJson(), roleUpdateCO.getAdminPrivJson(), roleUpdateCO.getImOrgPrivJson());
            String detail = buildDiff(old, role);
            operationLog.setOperation(String.format(operationLog.getOperation(), roleUpdateCO.getName(), detail));
            reportUtil.saveOperationLog(operationLog);
        }
    }

    @Override
    @LogReport(type = OperationTypeEnum.ROLE_ENABLE)
    public void enableRole(@LogReportParam Role role) {
        roleMapper.updateById(role);
    }

    @Override
    @LogReport(type = OperationTypeEnum.ROLE_DISABLE)
    public void disableRole(@LogReportParam Role role) {
        int count = userRoleMapper.count(role.getId());
        if (count > 0) {
            throw new BusinessException("该角色已绑定人员");
        }
        roleMapper.updateById(role);
    }

    private void fixAdminPrivilege(List<String> adminPriv) {
        if (adminPriv == null || adminPriv.isEmpty()) {
            return;
        }
        var list = menuService.list(Wrappers.<Menu>lambdaQuery().in(Menu::getId, adminPriv));
        list.forEach(a -> {
            if (a.getParentId() != null && a.getParentId() != -1L && !adminPriv.contains(a.getParentId() + "")) {
                adminPriv.add(a.getParentId() + "");
            }
        });
    }

    @Override
    public List<Role> findList(List<Long> roleIds) {
        return roleMapper.selectBatchIds(roleIds);
    }

    @Override
    public Role findById(Long id) {
        return roleMapper.selectById(id);
    }

    @Override
    @LogReport(type = OperationTypeEnum.ROLE_DELETE)
    public void deleteRoleBatch(@LogReportParam List<Role> roleList) {
        List<Long> roleIds = roleList.stream().map(Role::getId).collect(Collectors.toList());
        deleteRole(roleIds);
    }

    @Override
    public void deleteRole(List<Long> roleIds) {
        var trUserRoles =
                userRoleMapper.selectList(Wrappers.<TrUserRole>lambdaQuery().in(TrUserRole::getRoleId, roleIds));
        if (trUserRoles != null && !trUserRoles.isEmpty()) {
            throw new AdminException(COMMON_ERROR_518.getCode(), COMMON_ERROR_518.getMsg());
        }
        //删除role表
        roleMapper.deleteBatchIds(roleIds);
        //删除userRole表
        userRoleMapper.delete(Wrappers.<TrUserRole>lambdaQuery().in(TrUserRole::getRoleId, roleIds));
        //增加删除后的id缓存，用于判断是否重新拉取菜单权限
        String oldMark = redisUtil.get(ACCESSS_PERMISSION_UPDATE_MENU, String.class);
        List<Long> deletes = JSONObject.parseArray(oldMark, Long.class);
        if (deletes == null) {
            deletes = new ArrayList<>();
        }
        deletes.addAll(roleIds);
        //去重
        deletes = new ArrayList<>(new TreeSet<>(deletes));
        redisUtil.set(ACCESSS_PERMISSION_UPDATE_MENU, JSONObject.toJSONString(deletes));

    }

    @Override
    public List<Menu> h5Permissions(@NotNull String idCardNum) {
        List<Role> roleByExecutorId;
        if (encryptionService.encryptEnabled()) {
            roleByExecutorId = getRoleByIdCardNumEs(idCardNum);
        } else {
            roleByExecutorId  = roleMapper.roleByImIdCardNum(idCardNum);
        }

        var collect = Optional.ofNullable(roleByExecutorId).stream().flatMap(Collection::stream)//
                .map(Role::getCappPrivJson).filter(Objects::nonNull).flatMap(Collection::stream)//
                .collect(Collectors.toList());
        if (collect.isEmpty()) {
            return Collections.emptyList();
        }
        return menuService.list(Wrappers.lambdaQuery(Menu.class).in(Menu::getId, collect));
    }

    private List<Role> getRoleByIdCardNumEs(String idCardNum) {
        // 通过RPC调用查询用户（使用ES查询idCard）
        ImUserDto user = imUserRPCService.getByIdCard(idCardNum);
        if (user == null) {
            return Collections.emptyList();
        }
        return roleMapper.roleByImId(user.getId());
    }

    private String getTitle(){
        String title = globalsServiceImpl.getValueByName("title");
        return StringUtils.isNotBlank(title) ? title : "警务协同";
    }
}