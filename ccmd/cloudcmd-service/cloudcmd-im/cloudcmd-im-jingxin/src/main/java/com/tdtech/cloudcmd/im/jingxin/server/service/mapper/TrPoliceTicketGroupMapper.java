package com.tdtech.cloudcmd.im.jingxin.server.service.mapper;

import java.util.List;
import java.util.Map;

import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.im.jingxin.server.entity.TrPoliceTicketGroup;

@Mapper
public interface TrPoliceTicketGroupMapper extends MPJBaseMapper<TrPoliceTicketGroup> {

    @Insert("<script>" + //
        "INSERT INTO tr_police_ticket_group (id, ticket_id, group_id) VALUES" + //
        "    <foreach collection='list' item='item' separator=','>" + //
        "        (#{item.id}, #{item.ticketId}, #{item.groupId})" + //
        "    </foreach>" + //
        "</script>")
    int insertBatch(@Param("list") List<TrPoliceTicketGroup> trPoliceTicketGroups);

}
