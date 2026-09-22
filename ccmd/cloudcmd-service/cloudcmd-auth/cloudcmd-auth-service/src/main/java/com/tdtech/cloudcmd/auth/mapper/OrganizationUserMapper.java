package com.tdtech.cloudcmd.auth.mapper;

import java.util.List;

import com.tdtech.cloudcmd.auth.controller.admin.vo.OrgPrivDto;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.auth.entity.OrganizationUser;

/**
 * 组织用户关联表 Mapper 接口
 *
 * @author system
 * @since 2024-01-01
 */
@Mapper
public interface OrganizationUserMapper extends BaseMapper<OrganizationUser> {

    /**
     * 批量插入组织用户关联
     *
     * @param list 组织用户关联列表
     * @return 插入记录数
     */
    @Insert("<script>" +
            "INSERT INTO linkx_auth.tb_organization_user (id, application_id, user_id, im_org_id, grant_user_id, grant_time, gmt_created, gmt_modified) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.id}, #{item.applicationId}, #{item.userId}, #{item.imOrgId}, #{item.grantUserId}, #{item.grantTime}, now(), now())" +
            "</foreach>" +
            "</script>")
    int insertBatch(@Param("list") List<OrganizationUser> list);

    /**
     * 根据用户ID列表批量删除
     *
     * @param userIds 用户ID列表
     * @return 删除记录数
     */
    @Delete("<script>" +
            "DELETE FROM linkx_auth.tb_organization_user WHERE user_id IN " +
            "<foreach collection='userIds' item='userId' open='(' separator=',' close=')'>" +
            "#{userId}" +
            "</foreach>" +
            "</script>")
    int deleteByUserIds(@Param("userIds") List<Long> userIds);

    /**
     * 根据用户ID查询组织用户关联列表
     *
     * @param userId 用户ID
     * @return 组织用户关联列表
     */
    @Select("SELECT id, application_id, user_id, im_org_id, grant_user_id, grant_time, gmt_created, gmt_modified "
            + "FROM linkx_auth.tb_organization_user WHERE user_id = #{userId}")
    List<OrganizationUser> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID查询有权限的组织
     *
     * @param userId 用户ID
     * @return 组织用户关联列表
     */
    @Select("SELECT distinct tog.id,tog.code,tog.name " +
            "FROM linkx_auth.tb_organization_user  tou " +
            "join icp_collabs.tb_organization tog on tog.id = tou.im_org_id " +
            "WHERE tou.user_id = #{userId} " +
            "order by tog.sort,tog.id ")
    List<OrgPrivDto> selectOrgPrivByUserId(@Param("userId") Long userId);

}