
package com.tdtech.cloudcmd.im.jingxin.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImDepartment;
import com.tdtech.cloudcmd.im.jingxin.server.entity.Organization;
import com.tdtech.cloudcmd.im.jingxin.api.entity.department.OrganizationVO;

import java.util.List;

public interface IOrganizationService extends IService<Organization> {

    boolean syncFromIM();

    boolean reSync();

    void deleteCache();

    OrganizationVO tree(Long parentId);

    OrganizationVO tree(String parentCode);

    ImDepartment findOneById(Long id);

    List<Organization> findList(List<Long> idList);

    List<Organization> findListByCode(String code);

    List<ImDepartment> queryDepartmentForList(String parentCode);

    List<ImDepartment> queryDepartmentForListById(Long parentId);
}