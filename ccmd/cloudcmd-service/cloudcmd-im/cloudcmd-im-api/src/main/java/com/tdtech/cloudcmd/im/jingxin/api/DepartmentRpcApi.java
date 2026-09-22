package com.tdtech.cloudcmd.im.jingxin.api;

import com.tdtech.cloudcmd.bean.PageResult;
import com.tdtech.cloudcmd.im.jingxin.api.entity.department.OrganizationVO;

import java.util.List;

public interface DepartmentRpcApi {
    List<OrganizationVO> findList(List<Long> idList);

    List<OrganizationVO> queryDepartmentForList(String departmentCode);

    PageResult<OrganizationVO> page(Long parentId, String keywords, Integer page, Integer pageSize);

    OrganizationVO findRoot();
}
