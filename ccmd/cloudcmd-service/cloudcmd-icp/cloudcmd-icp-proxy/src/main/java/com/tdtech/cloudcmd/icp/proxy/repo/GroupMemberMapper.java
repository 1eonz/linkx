package com.tdtech.cloudcmd.icp.proxy.repo;

import com.tdtech.cloudcmd.icp.proxy.entity.GroupMember;
import com.tdtech.cloudcmd.mysql.enhance.ExBaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GroupMemberMapper extends ExBaseMapper<GroupMember> {

    /**
     * 根据群组ID和多个ISDN批量删除群组成员
     */
    @Delete("<script>" +
            "DELETE FROM tb_group_member WHERE group = #{groupId} AND isdn IN " +
            "<foreach collection='isdns' item='isdn' open='(' separator=',' close=')'>" +
            "#{isdn}" +
            "</foreach>" +
            "</script>")
    int deleteByGroupIdAndIsdns(@Param("groupId") Long groupId, @Param("isdns") List<String> isdns);
}
