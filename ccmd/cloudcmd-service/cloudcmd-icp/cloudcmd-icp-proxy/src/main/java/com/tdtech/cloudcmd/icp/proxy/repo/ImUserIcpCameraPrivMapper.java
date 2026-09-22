package com.tdtech.cloudcmd.icp.proxy.repo;

import com.tdtech.cloudcmd.icp.proxy.entity.ImUserIcpCameraPriv;
import com.tdtech.cloudcmd.mysql.enhance.ExBaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ImUserIcpCameraPrivMapper extends ExBaseMapper<ImUserIcpCameraPriv> {

    @Insert({"<script>",//
        "INSERT INTO tr_im_user_icp_camera_priv (id,im_user_id, camera_level) VALUES ",//
        "<foreach collection='list' item='item' separator=','>",//
        "(#{item.id}, #{item.imUserId}, #{item.cameraLevel})",//
        "</foreach>",//
        "</script>"//
    })
    int insertBatch(@Param("list") List<ImUserIcpCameraPriv> imUserIcpUserPrivList);

    @Delete("delete from tr_im_user_icp_camera_priv where im_user_id not in (select id from tb_im_user_icp)")
    void clear();
}
