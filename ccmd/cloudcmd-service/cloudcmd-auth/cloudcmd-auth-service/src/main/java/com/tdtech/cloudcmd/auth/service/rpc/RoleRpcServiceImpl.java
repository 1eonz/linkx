package com.tdtech.cloudcmd.auth.service.rpc;

import com.tdtech.cloudcmd.auth.controller.admin.vo.OrgPrivDto;
import com.tdtech.cloudcmd.auth.dto.ImUserDto;
import com.tdtech.cloudcmd.auth.dto.RoleDto;
import com.tdtech.cloudcmd.auth.entity.ImUserDO;
import com.tdtech.cloudcmd.auth.entity.Role;
import com.tdtech.cloudcmd.auth.mapper.ImUserMapper;
import com.tdtech.cloudcmd.auth.service.IRoleService;
import com.tdtech.cloudcmd.auth.service.RoleRpcService;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import com.tdtech.cloudcmd.util.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author mWX556161
 * @date 2020/9/27 14:12
 */
@DubboService
@Slf4j
public class RoleRpcServiceImpl implements RoleRpcService {

    @Autowired
    private IRoleService roleService;

    @Resource
    private ImUserMapper imUserMapper;

    @Override
    public List<RoleDto> getRoleListByType(Integer type) {
        List<Role> roleList = roleService.getRoleListByType(type);
        List<RoleDto> roleDtos = BeanCopyUtils.copyList(roleList, RoleDto::new);
        log.info("rpc getRoleListByType type:{}, result:{}", type, roleDtos);
        return roleDtos;
    }

    @Override
    public void createRoleMenuCache() {
        roleService.createRoleMenuCache();
    }

    @Override
    public RoleDto getRoleByUserId(String userId) {
        List<com.tdtech.cloudcmd.auth.entity.RoleDto> roles = roleService.getRoleWithDataPriv(Long.parseLong(userId), null, true);
        Role role = Optional.ofNullable(roles).stream().flatMap(Collection::stream).findAny().orElse(null);
        if (role == null) {
            return null;
        }
        RoleDto roleDto = BeanCopyUtils.copyBean(role, RoleDto::new);
        roleDto.convertOrgJson();
        roleDto.setImOrgPrivJson(null);
        return roleDto;
    }

    @Override
    public ImUserDto getUserInfoByUserId(String userId) {
        ImUserDO userDO = imUserMapper.selectById(Long.parseLong(userId));
        return BeanCopyUtils.copyBean(userDO, ImUserDto::new);
    }

    @Override
    public List<ImUserDto> getUserInfoByUserId(List<Long> userIds) {
        List<ImUserDO> imUserDOS = imUserMapper.selectBatchIds(userIds);
        return BeanCopyUtils.copyList(imUserDOS, ImUserDto::new);
    }

    @Override
    public void saveOrgPriv(Long roleId, Long grantUserId, List<OrgPrivDto> orgPrivList) {
        log.info("RPC调用：保存角色{}的组织权限，共{}条", roleId, orgPrivList != null ? orgPrivList.size() : 0);
        roleService.saveOrgPriv(roleId, grantUserId, orgPrivList);
    }

    @Override
    public Map<Long, List<OrgPrivDto>> getOrgPrivByRoleId(List<Long> roleIds) {
        log.info("RPC调用：获取角色{}的组织权限列表", roleIds);
        return roleService.getOrgPrivByRoleId(roleIds);
    }

    @Override
    public void deleteOrgPriv(List<Long> roleIds) {
        log.info("RPC调用：删除角色{}的组织权限", roleIds);
        if (CollectionUtils.isEmpty(roleIds)) {
            return;
        }
        roleService.deleteOrgPriv(roleIds);
    }
}