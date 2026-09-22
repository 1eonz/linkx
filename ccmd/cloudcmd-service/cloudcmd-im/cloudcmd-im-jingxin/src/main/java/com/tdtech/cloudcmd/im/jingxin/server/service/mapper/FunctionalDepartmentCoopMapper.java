package com.tdtech.cloudcmd.im.jingxin.server.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CoopUser;
import com.tdtech.cloudcmd.im.jingxin.server.entity.FunctionalDepartmentCoop;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 部门协同岗用户关联 Mapper 接口
 */
@Mapper
public interface FunctionalDepartmentCoopMapper extends BaseMapper<FunctionalDepartmentCoop> {
    Page<CoopUser> getCoopUsers(Page<?> objectPage, @Param("deptId")String deptI,
                                @Param("departments") List<Long> departments);

    Page<CoopUser> searchCoopUsers(Page<?> objectPage, @Param("deptId")Long deptId, @Param("name")String name,
                                   @Param("startTime")String startTime, @Param("endTime")String endTime);

    List<CoopUser> getCoopUsersList(@Param("deptId") Long deptId);

    int getMemberCountByLevelId(Long deptId);

    List<String> getBindMember(@Param("ids") List<Long> ids);

    void deleteCoopUsersByids(@Param("ids") List<Long> ids);
}