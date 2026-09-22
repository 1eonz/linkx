package com.tdtech.cloudcmd.im.jingxin.server.service.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.im.jingxin.server.entity.TrPoliceTicketTypePost;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TrPoliceTicketTypePostMapper extends BaseMapper<TrPoliceTicketTypePost> {

    @Insert("<script>" + //
        "INSERT INTO tr_police_ticket_type_post (id, type_id, post_id) VALUES " + //
        "   <foreach collection='list' item='item' separator=','>" + //
        "   (#{item.id}, #{item.typeId}, #{item.postId})" + //
        "   </foreach>" + //
        "</script>")
    int insertBatch(@Param("list") List<TrPoliceTicketTypePost> list);


    @Select("<script>" +//
        "select distinct tpttp.post_id " +//
        "from tr_police_ticket_type_post tpttp, " +//
        "     tb_collaboration_post tcp " +//
        "where tcp.id = tpttp.post_id " +//
        "  and tcp.type = 0 " +//
        "  and tpttp.type_id in " +//
        "  <foreach collection='tagIds' item='item' separator=',' open='(' close=')'>" +//
        "  #{item}" +//
        "  </foreach>" +//
        "</script>")
    List<Long> selectPostIdsByTag(@Param("tagIds") List<Long> tagIds);
}
