package com.tdtech.cloudcmd.auth.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tdtech.cloudcmd.auth.dto.ImUserDeptQO;
import com.tdtech.cloudcmd.auth.dto.ImUserQO;
import com.tdtech.cloudcmd.auth.dto.ImUserVO;
import com.tdtech.cloudcmd.auth.dto.UserImUserRelDTO;
import com.tdtech.cloudcmd.auth.entity.ImUserDO;
import com.tdtech.cloudcmd.auth.entity.UserAdmin;

import java.security.NoSuchAlgorithmException;
import java.util.List;

public interface ImUserService {
    ImUserDO getById(Long id);

    List<ImUserDO> getByIdList(List<Long> idList);

    IPage<ImUserDO> queryPaged(IPage<ImUserDO> page, ImUserQO username);

    IPage<ImUserDO> queryPagedWithChildren(IPage<ImUserDO> page, ImUserDeptQO qo);

    int deleteImUserById(List<Long> ids);

    int deleteImUser(List<ImUserDO> userList);

    void addImUsers(Long roleId, List<ImUserDO> imUsers) throws NoSuchAlgorithmException;

    void changeStatus(Long userId, Integer status);

    void enable(ImUserDO imUserDO);
    void disable(ImUserDO imUserDO);

    void changePwd(ImUserDO imUserDO) throws NoSuchAlgorithmException;

    void updateGmtUpdateForRole(List<Long> userIds);

    void saveAdmin(ImUserVO imUserVO) throws NoSuchAlgorithmException;

    IPage<ImUserDO> queryAdmin(IPage<ImUserDO> page, ImUserQO qo);

    ImUserVO queryAdminById(Long id);

    void updateAdminUser(ImUserVO imUserVO);

    void bindAdminImUser(UserImUserRelDTO userImUserRelDTO);

    void deleteAdminImUserRelation(UserImUserRelDTO id);

    UserAdmin getBindAdminImUser(Long userId);

    void deleteAdminByid(Long id);
}
