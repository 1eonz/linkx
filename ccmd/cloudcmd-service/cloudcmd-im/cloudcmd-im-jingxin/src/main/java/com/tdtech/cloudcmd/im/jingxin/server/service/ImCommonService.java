package com.tdtech.cloudcmd.im.jingxin.server.service;

import com.tdtech.cloudcmd.im.jingxin.api.entity.department.OrganizationVO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.CreateGroupCO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.SendApproveCardCO;
import com.tdtech.cloudcmd.im.jingxin.client.entity.*;

import java.util.List;
import java.util.Set;

/**
 * @author: S063874
 * @date: 2026-01-14 15:42
 */
public interface ImCommonService {

    ImPage<ImUser> queryUser(String code, Integer includeChildren, String keywords, String deptId, String name, Integer pageNum, Integer pageSize);

    ImPage<GroupInfoVo> queryGroupByUserType(String type, String key, String keywords, Integer pageNum, Integer pageSize);

    ImPage<UserFollowVo> getFriendsOrFollows(String userId, Integer userType, Integer pageNo, Integer pageSize);

    Object getUserTree(String userId, String departmentId);

    ImPage<ImDepartment> getDepartmentPage(String userId, String departmentId, Integer pageNo, Integer pageSize);

    Long createGroup(CreateGroupCO createGroupVO);

    ImPage<ImUser> getUsersPage(String name, String keywords, String departmentId, Integer pageNo, Integer pageSize);

    Set<String> getUserDepartments();

    List<OrganizationVO> getUserDepartmentTree(String userId);

    Long createGroupByFunctionalDepartment(CreateGroupCO createGroupVO);

    Object pullHistoryGroup(String sync);

    Boolean sendApproveCard(SendApproveCardCO sendApproveCardCO);

    ImUser getUserByUserId(String userId);

    ImUser getUserByUserIdCard(String idCard);

    ImUserDeptNodeInfoVO userDeptNodeInfo(Long userId);

    String getPeerNodeGateWayPrefix();
}