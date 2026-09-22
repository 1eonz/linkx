package com.tdtech.cloudcmd.icp.proxy.repo;

import com.tdtech.cloudcmd.icp.proxy.entity.IcpImUser;
import com.tdtech.cloudcmd.mysql.enhance.ExMPJBaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IcpImUserMapper extends ExMPJBaseMapper<IcpImUser> {

    @Insert({"<script>",
        "INSERT INTO tb_im_user_icp (id, code, name, avatar, gender, mobile, email, isdn, id_card, district, direct_leader_id, direct_leader_name, is_binding) VALUES ",
        "<foreach collection='list' item='item' separator=','>",
        "(#{item.id}, #{item.code}, #{item.name}, #{item.avatar}, #{item.gender}, #{item.mobile}, #{item.email}, #{item.isdn}, #{item.idCard}, #{item.district}, #{item.directLeaderId}, #{item.directLeaderName}, #{item.isBinding})",
        "</foreach>",//
        "</script>"})
    int insertBatch(@Param("list") List<IcpImUser> icpImUserList);

}
