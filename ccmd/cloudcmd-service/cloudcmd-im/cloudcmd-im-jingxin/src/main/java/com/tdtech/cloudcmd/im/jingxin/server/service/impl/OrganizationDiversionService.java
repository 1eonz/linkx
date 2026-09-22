package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tdtech.cloudcmd.im.jingxin.api.entity.department.OrganizationVO;
import com.tdtech.cloudcmd.im.jingxin.client.CachedImConfig;
import com.tdtech.cloudcmd.im.jingxin.client.ImHttpClient;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImDepartment;
import com.tdtech.cloudcmd.im.jingxin.server.entity.Organization;
import com.tdtech.cloudcmd.im.jingxin.server.service.IOrganizationService;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 根据配置，决定查询部门是从IM查询还是从我们自己业务库查询分流service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationDiversionService {

    private final ImHttpClient imHttpClient;

    @Resource
    private CachedImConfig cachedImConfig;

    @Resource
    private IOrganizationService organizationService;

    @Resource
    private ImService imService;

    private boolean isSearchFromLocal() {
        String enableFlag = cachedImConfig.getConfig("DEPARTMENT_SYNC_SIGN");
        return StringUtils.isNotBlank(enableFlag) && "true".equals(enableFlag);
    }

    public ImDepartment findOne(String departmentCode) {
        List<ImDepartment> imDepartments = queryDepartment(departmentCode);
        if (CollectionUtils.isNotEmpty(imDepartments)) {
            return imDepartments.get(0);
        }
        return null;
    }

    public List<OrganizationVO> findListVO(List<Long> idList) {
        if (idList == null || idList.isEmpty()) {
            return Collections.emptyList();
        }
        if (isSearchFromLocal()) {
            var list = organizationService.findList(idList);
            return BeanCopyUtils.copyList(list, OrganizationVO::new);
        } else {
            var list = CollectionUtils.group(idList, 15)//
                    .values()//
                    .stream()//
                    .map(a -> imHttpClient.queryDepartmentByIds(
                            a.stream().map(String::valueOf).collect(Collectors.joining(","))))//
                    .flatMap(Collection::stream)//
                    .collect(Collectors.toList());
            return BeanCopyUtils.copyList(list, OrganizationVO::new);
        }
    }

    public List<ImDepartment> findList(List<Long> idList) {
        if (idList == null || idList.isEmpty()) {
            return Collections.emptyList();
        }
        if (isSearchFromLocal()) {
            var list = organizationService.findList(idList);
            return BeanCopyUtils.copyList(list, ImDepartment::new);
        }
        return CollectionUtils.group(idList, 15)//
                .values()//
                .stream()//
                .map(a -> imHttpClient.queryDepartmentByIds(
                        a.stream().map(String::valueOf).collect(Collectors.joining(","))))//
                .flatMap(Collection::stream)//
                .collect(Collectors.toList());
    }

    public List<ImDepartment> findDepartmentAndChildrenByName(String name) {
        if (StringUtils.isBlank(name)) {
            return Collections.emptyList();
        }
        List<ImDepartment> matchedDepartments;
        if (isSearchFromLocal()) {
            LambdaQueryWrapper<Organization> queryWrapper =
                    new LambdaQueryWrapper<Organization>().eq(Organization::getName, name);
            var list = organizationService.list(queryWrapper);
            matchedDepartments = BeanCopyUtils.copyList(list, ImDepartment::new);
        } else {
            matchedDepartments = imService.queryDepartmentForList(null).stream()
                    .filter(department -> StringUtils.isNotBlank(department.getName())
                            && department.getName().equals(name))
                    .collect(Collectors.toList());
        }
        if (CollectionUtils.isEmpty(matchedDepartments) || matchedDepartments.size() != 1) {
            return Collections.emptyList();
        }
        List<ImDepartment> departments = queryDepartmentForListById(matchedDepartments.get(0).getId());
        return CollectionUtils.isNotEmpty(departments) ? departments : matchedDepartments;
    }

    public List<ImDepartment> queryDepartment(String departmentCode) {
        if (isSearchFromLocal()) {
            var list = organizationService.findListByCode(departmentCode);
            return BeanCopyUtils.copyList(list, ImDepartment::new);
        }
        return imHttpClient.queryDepartment(departmentCode);
    }

    /**
     * 包含子孙节点
     *
     * @param parentCode
     * @return
     */
    public List<ImDepartment> queryDepartmentForList(String parentCode) {
        if (isSearchFromLocal()) {
            return organizationService.queryDepartmentForList(parentCode);
        }
        return imService.queryDepartmentForList(parentCode);
    }

    /**
     * 包含子孙节点
     *
     * @param parentId
     * @return
     */
    public List<ImDepartment> queryDepartmentForListById(Long parentId) {
        if (isSearchFromLocal()) {
            return organizationService.queryDepartmentForListById(parentId);
        }
        return imService.queryDepartmentForListById(parentId);
    }

}
