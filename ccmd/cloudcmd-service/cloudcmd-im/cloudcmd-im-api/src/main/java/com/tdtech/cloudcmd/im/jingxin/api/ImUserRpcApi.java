package com.tdtech.cloudcmd.im.jingxin.api;

import com.tdtech.cloudcmd.im.jingxin.api.entity.im.ImUserPageResult;
import com.tdtech.cloudcmd.im.jingxin.api.entity.im.UserGetResultVo;

public interface ImUserRpcApi {

    UserGetResultVo getUsersInfo(String userIds, String idCards, String xUserId);

    Integer getMySortedType(Long userId);

    /**
     * 按部门分页查询人员（实时查警信）。
     *
     * @param pageNum        页码
     * @param pageSize       每页数量
     * @param deptId         部门ID；为空时查全量
     * @param includeChildren 是否包含子部门：0-否，1-是
     * @param name           人员姓名模糊查询
     * @return 分页结果
     */
    ImUserPageResult pageByDepartment(Integer pageNum, Integer pageSize, String deptId, Integer includeChildren, String name);
}