package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import com.tdtech.cloudcmd.im.jingxin.client.ImHttpClient;
import com.tdtech.cloudcmd.im.jingxin.client.entity.*;
import com.tdtech.cloudcmd.util.ListUtils;
import com.tdtech.cloudcmd.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImService {

    private final ImHttpClient imHttpClient;

    /**
     * 不递归查询下级组织，都不传查顶层组织
     */
    public List<ImDepartment> queryDepartment(String parentCode, String parentId) {
        // 狗屎 查询顶层组织要单独处理，当虚拟账号配置了组织的时候要从token拿组织单独查
        if (StringUtils.isAllBlank(parentCode, parentId)) {
            var token = imHttpClient.getToken();
            if (token == null) {
                log.warn("login error");
                return Collections.emptyList();
            } else if (token.getDepartment() != null && token.getDepartment()
                    .getDepartmentCode() != null && !token.getDepartment().getDepartmentCode().isBlank()) {
                return imHttpClient.queryDepartment(token.getDepartment().getDepartmentCode());
            }
        }

        int dpn = 1, dps = 50;
        Long lastQueryTime = null;
        List<ImDepartment> list = new ArrayList<>();
        while (true) {
            var dPage = imHttpClient.departmentPage(dpn, dps, parentCode, parentId, null,lastQueryTime);
            if (dPage == null || dPage.getRecords() == null || dPage.getTotal() == null) {
                log.warn("data error:{}", dPage);
                break;
            }
            list.addAll(dPage.getRecords());
            if (dPage.getTotal() <= dpn * dps) {
                break;
            } else {
                dpn++;
            }
        }
        return list;
    }

    public List<ImDepartment> queryDepartmentByCode(List<String> codes){
        return imHttpClient.queryDepartment(String.join(",", codes));
    }


    /**
     *
     * 返回一个根据list集合版的部门结构，不需要构造树形，递归查询下级
     * 
     */
    public List<ImDepartment> queryDepartmentForList(String parentCode) {
        int dpn = 1, dps = 50;
        Long lastQueryTime = null;
        List<ImDepartment> list = new ArrayList<>();
        while (true) {
            var dPage = imHttpClient.departmentPage(dpn, dps, parentCode, null, 1,lastQueryTime);
            if (dPage == null || dPage.getRecords() == null || dPage.getTotal() == null) {
                log.warn("data error:{}", dPage);
                break;
            }
            for (var record : dPage.getRecords()) {
                list.add(record);
            }
            if (dPage.getTotal() <= dpn * dps) {
                break;
            } else {
                dpn++;
            }
        }
        // 顶层单独处理
        if (parentCode != null && !parentCode.isBlank()) {
            List<ImDepartment> imDepartments = imHttpClient.queryDepartment(parentCode);
            if (ListUtils.isNotBlankList(imDepartments)) {
                list.addAll(imDepartments);
            }
        } else {
            var token = imHttpClient.getToken();
            if (token.getDepartment() == null || token.getDepartment()
                    .getDepartmentCode() == null || token.getDepartment().getDepartmentCode().isBlank()) {
                // 顶层组织已经包含在上边查询，不需要单独处理
                return list;
            } else {
                List<ImDepartment> imDepartments =
                        imHttpClient.queryDepartment(token.getDepartment().getDepartmentCode());
                if (ListUtils.isNotBlankList(imDepartments)) {
                    list.addAll(imDepartments);
                }
            }
        }
        return list;
    }

    public List<ImDepartment> queryDepartmentForListById(Long parentId) {
        int dpn = 1, dps = 50;
        Long lastQueryTime = null;
        List<ImDepartment> list = new ArrayList<>();
        while (true) {
            var dPage =
                    imHttpClient.departmentPage(dpn, dps, null, Objects.nonNull(parentId) ? String.valueOf(parentId) : null,
                            1,lastQueryTime);
            if (dPage == null || dPage.getRecords() == null || dPage.getTotal() == null) {
                log.warn("data error:{}", dPage);
                break;
            }
            for (var record : dPage.getRecords()) {
                list.add(record);
            }
            if (dPage.getTotal() <= dpn * dps) {
                break;
            } else {
                dpn++;
            }
        }
        // 顶层单独处理
        if (parentId != null) {
            List<ImDepartment> imDepartments = imHttpClient.queryDepartmentByIds(String.valueOf(parentId));
            if (ListUtils.isNotBlankList(imDepartments)) {
                list.addAll(imDepartments);
            }
        } else {
            var token = imHttpClient.getToken();
            if (Objects.isNull(token.getDepartment()) || Objects.isNull(token.getDepartment().getDepartmentId())) {
                // 顶层组织已经包含在上边查询，不需要单独处理
                return list;
            } else {
                List<ImDepartment> imDepartments =
                    imHttpClient.queryDepartmentByIds(token.getDepartment().getDepartmentId() + "");
                if (ListUtils.isNotBlankList(imDepartments)) {
                    list.addAll(imDepartments);
                }
            }
        }
        return list;
    }

    public List<ImUser> queryUser(String code, Integer includeChildren, String keywords, String deptId, String name) {
        int dpn = 1, dps = 50;
        Long lastQueryTime = null;
        List<ImUser> list = new ArrayList<>();
        while (true) {
            var dPage = imHttpClient.userPageByDepartment(dpn, dps, code, includeChildren, keywords, deptId, name, lastQueryTime);
            if (dPage == null || dPage.getRecords() == null || dPage.getTotal() == null) {
                log.warn("data error:{}", dPage);
                break;
            }
            for (var record : dPage.getRecords()) {
                list.add(record);
            }
            if (dPage.getTotal() <= dpn * dps) {
                break;
            } else {
                dpn++;
            }
        }
        return list;
    }

    public ImPage<ImUser> queryUserByPage(String code, Integer pageNum, Integer pageSize, Integer includeChildren,
                                          String keywords, String name) {

        return imHttpClient.userPageByDepartment(pageNum, pageSize, code, includeChildren, keywords, null, name,null);
    }

    /**
     * 从im获取全量协同岗数据
     * 
     * @return
     */
    public List<UserListVo> queryCollaborationPost() {
        CooperationUserQueryRequest req = new CooperationUserQueryRequest();
        req.setIncludeChildren(1);
        int dpn = 1, dps = 1000;
        req.setPageSize(dps);
        List<UserListVo> list = new ArrayList<>();
        while (true) {
            req.setPageNo(dpn);
            var dPage = imHttpClient.queryCollborationUser(req);
            if (dPage == null || dPage.getRecords() == null) {
                log.warn("data error:{}", dPage);
                break;
            }
            for (var record : dPage.getRecords()) {
                list.add(record);
            }
            if (dPage.getTotalCount() <= dpn * dps) {
                break;
            } else {
                dpn++;
            }
        }
        return list;
    }

    /**
     * 根据身份证号查询用户信息
     * 
     * @param idCard
     * @return
     */
    public ImUser userPageByIdCard(String idCard) {
        UserGetVo userGetVo = imHttpClient.userPage(idCard,null);
        if (userGetVo.getResults() != null && !userGetVo.getResults().isEmpty()) {
            return userGetVo.getResults().get(0);
        }
        return null;
    }

    public List<GroupMembers> queryUserByGroupId(Long groupId){
        GroupVo groupVo = imHttpClient.queryGroupDetail(groupId);

        return groupVo.getGroupMembers();
    }

    public ImUser findUserInfo(String userId) {
        var userGetVo = imHttpClient.userPage(null, userId);
        if (userGetVo == null || userGetVo.getResults() == null || userGetVo.getResults().isEmpty()) {
            log.warn("findUserInfo user not found:{} {}", userId, userGetVo);
            return null;
        }
        return userGetVo.getResults().get(0);
    }
    public List<ImUser> findUserInfo(List<Long> userIds) {
        var userGetVo =
                imHttpClient.userPage(null, userIds.stream().map(Object::toString).collect(Collectors.joining(",")));
        if (userGetVo == null || userGetVo.getResults() == null || userGetVo.getResults().isEmpty()) {
            log.warn("findUserInfo user not found:{} {}", userIds, userGetVo);
            return null;
        }
        return userGetVo.getResults();
    }

    /**
     * 按身份证号批量查警信用户，构建 idCard→userId 映射。
     * <p>
     * 供 tb_static_photo_check 统计同步使用：核查记录源表只存身份证，需转成警信 userId 才能关联协同岗。
     * 查不到的身份证不在返回 Map 中，调用方按"查不到则记日志不入库"处理。
     *
     * @param idCards 身份证号集合
     * @return idCard→userId 映射（查不到的不包含）
     */
    public Map<String, Long> findUserIdMapByIdCards(Collection<String> idCards) {
        if (idCards == null || idCards.isEmpty()) {
            return Collections.emptyMap();
        }
        String joined = idCards.stream().filter(c -> c != null && !c.isBlank())
                .collect(Collectors.joining(","));
        if (joined.isBlank()) {
            return Collections.emptyMap();
        }
        var userGetVo = imHttpClient.userPage(joined, null);
        if (userGetVo == null || userGetVo.getResults() == null || userGetVo.getResults().isEmpty()) {
            log.warn("findUserIdMapByIdCards no user found for idCards:{}", idCards);
            return Collections.emptyMap();
        }
        Map<String, Long> result = new HashMap<>();
        for (ImUser user : userGetVo.getResults()) {
            if (user.getIdCard() != null && user.getId() != null) {
                result.put(user.getIdCard(), user.getId());
            }
        }
        return result;
    }

    /**
     * 递归查询部门及其所有父部门
     * @param departmentId 起始部门ID
     * @return 包含本部门和所有父部门的列表
     */
    public List<ImDepartment> queryDepartmentWithParents(Long departmentId) {
        if (departmentId == null) {
            return Collections.emptyList();
        }

        Set<Long> processedIds = new HashSet<>();
        List<ImDepartment> result = new ArrayList<>();
        queryDepartmentWithParentsRecursive(departmentId, processedIds, result);
        return result;
    }

    /**
     * 递归查询父部门的辅助方法
     */
    private void queryDepartmentWithParentsRecursive(Long departmentId, Set<Long> processedIds, List<ImDepartment> result) {
        if (departmentId == null || processedIds.contains(departmentId) || departmentId == 0) {
            return;
        }

        processedIds.add(departmentId);

        // 查询当前部门
        List<ImDepartment> departments = imHttpClient.queryDepartmentByIds(String.valueOf(departmentId));
        if (departments != null && !departments.isEmpty()) {
            ImDepartment currentDepartment = departments.get(0);
            result.add(currentDepartment);

            // 递归查询父部门
            if (currentDepartment.getParentId() != null) {
                queryDepartmentWithParentsRecursive(currentDepartment.getParentId(), processedIds, result);
            }
        }
    }
}