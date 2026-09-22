package com.tdtech.cloudcmd.im.jingxin.server.util;

import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImDepartment;
import com.tdtech.cloudcmd.im.jingxin.server.service.impl.ImService;
import com.tdtech.cloudcmd.im.jingxin.server.service.impl.OrganizationDiversionService;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class DataPermissionUtil {

    @Resource
    private ImService imService;

    @Resource
    private OrganizationDiversionService organizationDiversionService;

    /**
     * 本级及其下级部门的编码
     * @return
     */
    public List<String> getDefaultOrgCodeList() {
        UserInfo user = SecurityUtils.getUser();
        if (user != null) {
            String organizationCode = user.getOrganizationCode();
            List<ImDepartment> imDepartments = organizationDiversionService.queryDepartmentForList(organizationCode);
            if (CollectionUtils.isNotEmpty(imDepartments)) {
                return imDepartments.stream().map(ImDepartment::getCode).collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }
}
