package com.tdtech.cloudcmd.auth.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.auth.entity.UserAdmin;

/**
 * 用户管理员表 Mapper 接口
 *
 * @author system
 * @since 2024-01-01
 */
@Mapper
public interface UserAdminMapper extends BaseMapper<UserAdmin> {

    /**
     * 新增用户
     *
     * @param userAdmin 用户信息
     * @return 插入记录数
     */
    @Insert("INSERT INTO linkx_auth.tb_user_admin (id, application_id, name, account, password, last_login_time, status, first_login, error_pwd_count, error_pwd_last_time, gmt_created, gmt_modified,create_user_id) " +
            "VALUES (#{id}, #{applicationId}, #{name}, #{account}, #{password}, #{lastLoginTime}, #{status}, #{firstLogin}, #{errorPwdCount}, #{errorPwdLastTime}, now(), now(),#{createUserId})")
    int insertUser(UserAdmin userAdmin);

    @Update("UPDATE linkx_auth.tb_user_admin SET " +
            "im_user_id = COALESCE(#{imUserId}, 0), " +
            "im_user_dept_id = COALESCE(#{imUserDeptId}, 0), " +
            "gmt_modified = now() " +
            "WHERE id = #{id}")
    int updateImUserRelation(UserAdmin userAdmin);

    @Update("UPDATE linkx_auth.tb_user_admin SET " +
            "im_user_id = 0, " +
            "im_user_dept_id = 0, " +
            "gmt_modified = now() " +
            "WHERE id = #{id}")
    int deleteImUserRelation(@Param(value = "id") Long id);

    /**
     * 根据ID修改用户信息
     *
     * @param userAdmin 用户信息
     * @return 更新记录数
     */
    @Update("UPDATE linkx_auth.tb_user_admin SET " +
            "password = #{password}, " +
            "gmt_modified = now() " +
            "WHERE id = #{id}")
    int updatePwdById(UserAdmin userAdmin);

    @Update("UPDATE linkx_auth.tb_user_admin SET " +
            "status = #{status}, " +
            "gmt_modified = now() " +
            "WHERE id = #{id}")
    int updateStatusById(@Param(value = "id") Long id, @Param(value = "status") Integer status);

    /**
     * 根据ID删除用户
     *
     * @param id 用户ID
     * @return 删除记录数
     */
    @Delete("DELETE FROM linkx_auth.tb_user_admin WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

}
