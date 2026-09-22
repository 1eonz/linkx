package com.tdtech.cloudcmd.icp.proxy.repo;

import com.tdtech.cloudcmd.icp.proxy.entity.ImUserIcpUserPriv;
import com.tdtech.cloudcmd.mysql.enhance.ExBaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ImUserIcpUserPrivMapper extends ExBaseMapper<ImUserIcpUserPriv> {

    @Insert({"<script>",//
        "INSERT INTO tr_im_user_icp_priv (id,im_user_id, departmentid) VALUES ",//
        "<foreach collection='list' item='item' separator=','>",//
        "(#{item.id}, #{item.imUserId}, #{item.departmentid})",//
        "</foreach>",//
        "</script>"//
    })
    int insertBatch(@Param("list") List<ImUserIcpUserPriv> imUserIcpUserPrivList);

    @Select("select tiu.id as im_user_id, tu.departmentid as departmentid"//
        + "     from tb_im_user_icp tiu"//
        + "     left join tr_im_user_icp_priv triuip on triuip.im_user_id = tiu.id"//
        + "     join tb_isdn tu on tiu.isdn = tu.isdn"//
        + "     where tu.departmentid is not null"//
        + "       and tu.status = 0"//
        + "       and triuip.im_user_id is null")
    List<ImUserIcpUserPriv> getDefaults();

    @Select("select tiu.id as im_user_id, tu.departmentid as departmentid"//
        + "     from tb_im_user_icp tiu"//
        + "     left join tr_im_user_icp_priv triuip on triuip.im_user_id = tiu.id"//
        + "     join tb_isdn tu on tiu.isdn = tu.isdn"//
        + "     where tu.departmentid is not null"//
        + "       and tu.status = 0"//
        + "       and tiu.id = #{userId}"//
        + "       and triuip.im_user_id is null")
    List<ImUserIcpUserPriv> getDefaultsByUser(@Param("userId") Long userId);

    @Delete("delete from tr_im_user_icp_priv where im_user_id not in (select id from tb_im_user_icp)")
    void clear();
}