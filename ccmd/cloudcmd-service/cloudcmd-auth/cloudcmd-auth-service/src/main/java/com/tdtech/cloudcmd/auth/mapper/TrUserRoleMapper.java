package com.tdtech.cloudcmd.auth.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.auth.entity.TrUserRole;

import java.util.List;

/**
 * <p>
 * 用户-角色信息表（无勤务时，后台设定；有勤务时，根据排班设定）--角色与执行者关联还是与用户关联后续再看，现在先跟用户关联。 Mapper 接口
 * </p>
 *
 * @author mWX556161
 * @since 2020-11-24
 */
@Mapper
public interface TrUserRoleMapper extends BaseMapper<TrUserRole> {

    /**
     * 批量插入用户角色关联
     * @param list 用户角色关联列表
     * @return 插入记录数
     */
    @Insert("<script>" +
            "INSERT INTO tr_user_role (id, user_id, role_id, gmt_created, gmt_modified) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.id}, #{item.userId}, #{item.roleId}, now(), now())" +
            "</foreach>" +
            "</script>")
    int insertBatch(@Param("list") List<TrUserRole> list);

}