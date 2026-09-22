package com.tdtech.cloudcmd.admin.resource.service;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.tdtech.cloudcmd.admin.resource.entity.Menu;
import com.tdtech.cloudcmd.admin.resource.entity.Role;
import com.tdtech.cloudcmd.admin.resource.entity.vo.AdminRoleVO;
import com.tdtech.cloudcmd.admin.resource.entity.co.RoleCreateCO;
import com.tdtech.cloudcmd.admin.resource.entity.co.RoleUpdateCO;
import com.tdtech.cloudcmd.admin.resource.entity.qo.RoleQO;
import com.tdtech.cloudcmd.bean.PageResult;

/**
 * <p>
 * 角色信息表 服务类
 * </p>
 *
 * @author zhuangzl
 * @since 2020-06-23
 */
public interface IRoleService {

    /**
     * 查询角色（返回包含组织权限详情的VO）
     *
     * @return
     */
    PageResult<AdminRoleVO> listRole(RoleQO roleVo);

    Role createRole(RoleCreateCO roleCreateCO);

    void updateRole(RoleUpdateCO roleUpdateCO, Role old);

    void enableRole(Role role);

    void disableRole(Role role);

    void deleteRole(List<Long> roleIds);

    List<Role> findList(List<Long> roleIds);

    Role findById(Long id);

    void deleteRoleBatch(List<Role> roleList);

    List<Menu> h5Permissions(@NotNull String idCardNum);
}