package com.tdtech.cloudcmd.im.jingxin.server.rpc;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.bean.PageResult;
import com.tdtech.cloudcmd.im.jingxin.api.DepartmentRpcApi;
import com.tdtech.cloudcmd.im.jingxin.api.entity.department.OrganizationVO;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImDepartment;
import com.tdtech.cloudcmd.im.jingxin.server.entity.Organization;
import com.tdtech.cloudcmd.im.jingxin.server.service.IOrganizationService;
import com.tdtech.cloudcmd.im.jingxin.server.service.impl.OrganizationDiversionService;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboService;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@DubboService
public class DepartmentRpc implements DepartmentRpcApi {
    @Resource
    private OrganizationDiversionService organizationDiversionService;
    @Resource
    private IOrganizationService organizationService;

    @Override
    public List<OrganizationVO> findList(List<Long> idList) {
        return organizationDiversionService.findListVO(idList);
    }
    @Override
    public List<OrganizationVO> queryDepartmentForList(String departmentCode) {
        List<ImDepartment> imDepartments = organizationDiversionService.queryDepartmentForList(departmentCode);
        return BeanCopyUtils.copyList(imDepartments, OrganizationVO::new);
    }

    @Override
    public PageResult<OrganizationVO> page(Long parentId, String keywords, Integer page, Integer pageSize) {
        LambdaQueryWrapper<Organization> queryWrapper = new LambdaQueryWrapper<Organization>()
            .select(
                Organization::getId, Organization::getCode, Organization::getName, Organization::getShortName,
                Organization::getParentId, Organization::getParentCode, Organization::getParentName,
                Organization::getSort, Organization::getFullPath, Organization::getFullPathCode,
                Organization::getFullPathName, Organization::getGmtCreated, Organization::getGmtModified
            )
            .eq(parentId != null, Organization::getParentId, parentId)
            .like(StringUtils.isNotBlank(keywords), Organization::getName, StringUtils.trimToNull(keywords))
            .orderByAsc(Organization::getSort).orderByAsc(Organization::getId);
        Page<Organization> dataPage = organizationService.page(new Page<>(page, pageSize), queryWrapper);
        return PageResult.fromIPage(dataPage, dataPage.getRecords().stream()
            .map(this::toOrganizationVO)
            .collect(Collectors.toList()));
    }

    @Override
    public OrganizationVO findRoot() {
        List<Organization> roots = organizationService.list(new LambdaQueryWrapper<Organization>()
            .select(
                Organization::getId, Organization::getCode, Organization::getName, Organization::getShortName,
                Organization::getParentId, Organization::getParentCode, Organization::getParentName,
                Organization::getSort, Organization::getFullPath, Organization::getFullPathCode,
                Organization::getFullPathName, Organization::getGmtCreated, Organization::getGmtModified
            )
            .eq(Organization::getParentId, 0L)
            .orderByAsc(Organization::getSort).orderByAsc(Organization::getId));
        if (roots == null || roots.isEmpty()) {
            return null;
        }
        return toOrganizationVO(roots.get(0));
    }

    private OrganizationVO toOrganizationVO(Organization organization) {
        OrganizationVO vo = new OrganizationVO();
        vo.setId(organization.getId());
        vo.setCode(organization.getCode());
        vo.setName(organization.getName());
        vo.setShortName(organization.getShortName());
        vo.setParentId(organization.getParentId());
        vo.setParentCode(organization.getParentCode());
        vo.setParentName(organization.getParentName());
        vo.setSort(organization.getSort());
        vo.setFullPath(organization.getFullPath());
        vo.setFullPathCode(organization.getFullPathCode());
        vo.setFullPathName(organization.getFullPathName());
        vo.setGmtCreated(organization.getGmtCreated());
        vo.setGmtModified(organization.getGmtModified());
        return vo;
    }
}
