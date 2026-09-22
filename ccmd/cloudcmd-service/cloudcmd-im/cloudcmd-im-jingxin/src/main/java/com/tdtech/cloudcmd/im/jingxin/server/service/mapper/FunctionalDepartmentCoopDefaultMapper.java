package com.tdtech.cloudcmd.im.jingxin.server.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CoopUser;
import com.tdtech.cloudcmd.im.jingxin.server.entity.FunctionalDepartmentCoopDefault;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 部门协同岗用户关联默认表 Mapper 接口
 */
@Mapper
public interface FunctionalDepartmentCoopDefaultMapper extends BaseMapper<FunctionalDepartmentCoopDefault> {
    Page<CoopUser> getCoopUsers(Page<?> objectPage, @Param("name") String name,@Param("departments") List<Long> departments);
}