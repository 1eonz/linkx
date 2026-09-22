package com.tdtech.cloudcmd.im.jingxin.server.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.im.jingxin.server.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    @Select("SELECT DISTINCT module_name FROM linkx_open.tb_notification WHERE collaborative_msg = 1 AND module_name IN " +
            "(SELECT DISTINCT n.module_name FROM linkx_open.tb_notification n " +
            "INNER JOIN linkx_open.tb_notification_targets nt ON n.id = nt.notification_id " +
            "WHERE nt.user_id = #{userId})")
    List<String> selectModuleNamesByUserId(@Param("userId") Long userId);
}
