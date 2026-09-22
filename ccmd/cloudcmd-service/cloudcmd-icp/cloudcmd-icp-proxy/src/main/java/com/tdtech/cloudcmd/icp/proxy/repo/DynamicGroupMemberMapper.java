package com.tdtech.cloudcmd.icp.proxy.repo;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.icp.proxy.controller.vo.DynamicGroupQO;
import com.tdtech.cloudcmd.icp.proxy.entity.DynamicGroupMember;
import com.tdtech.cloudcmd.mysql.enhance.ExBaseMapper;
import com.tdtech.cloudcmd.mysql.entity.CcmdPage;
import com.tdtech.cloudcmd.mysql.entity.CcmdPageParam;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DynamicGroupMemberMapper extends ExBaseMapper<DynamicGroupMember> {

    @Insert("<script>" +//
        "INSERT INTO tb_dynamic_group_member (id, group_id,group_name, user_id, user_name, isdn, tumb_avatar, owner_id) " +//
        "VALUES " +//
        "<foreach collection='list' item='item' separator=','>" +//
        "(#{item.id}, #{item.groupId}, #{item.groupName}, #{item.userId}, #{item.userName}, #{item.isdn}, #{item.tumbAvatar}, #{item.ownerId})" +//
        "</foreach>" +//
        "</script>")
    void insertBatch(@Param("list") List<DynamicGroupMember> dynamicGroupMembers);

    /**
     * 分页查询不同的 groupId，支持条件查询
     */
    @Select("<script>" +//
        "SELECT DISTINCT group_id FROM tb_dynamic_group_member WHERE 1=1 " +//
        "<if test='qo.userId != null and qo.userId != \"\"'> AND user_id = #{qo.userId}</if> " +//
        "<if test='qo.userName != null and qo.userName != \"\"'> AND user_name LIKE CONCAT('%', #{qo.userName}, '%')</if> " +//
        "<if test='qo.isdn != null and qo.isdn != \"\"'> AND isdn = #{qo.isdn}</if> " +//
        "</script>")
    Page<String> __selectDistinctGroupIdsByCondition(Page<String> page, @Param("qo") DynamicGroupQO qo);

    default CcmdPage<String> selectDistinctGroupIdsByCondition(CcmdPageParam pageParam,
        @Param("qo") DynamicGroupQO qo) {
        var page = new Page<String>(pageParam.getPageNum(), pageParam.getPageSize());
        page = __selectDistinctGroupIdsByCondition(page, qo);
        // 转换返回
        return new CcmdPage<>(pageParam.getPageNum(), pageParam.getPageSize(), page.getTotal(), page.getRecords());
    }
}
