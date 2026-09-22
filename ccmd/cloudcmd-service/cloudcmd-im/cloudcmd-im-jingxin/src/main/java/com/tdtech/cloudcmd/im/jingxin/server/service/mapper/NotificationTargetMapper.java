package com.tdtech.cloudcmd.im.jingxin.server.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.im.jingxin.server.entity.NotificationTarget;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface NotificationTargetMapper extends BaseMapper<NotificationTarget> {

    @Select("SELECT COUNT(*) FROM linkx_open.tb_notification_targets nt " +
            "INNER JOIN linkx_open.tb_notification n ON nt.notification_id = n.id " +
            "WHERE nt.user_id = #{userId} AND n.module_name = #{moduleName} AND n.collaborative_msg = 1 AND nt.read = 0")
    int countUnreadByUserIdAndModuleName(@Param("userId") Long userId, @Param("moduleName") String moduleName);

    @Select("SELECT COUNT(*) FROM linkx_open.tb_notification_targets nt " +
            "INNER JOIN linkx_open.tb_notification n ON nt.notification_id = n.id " +
            "WHERE nt.user_id = #{userId} AND n.module_name = #{moduleName} AND n.collaborative_msg = 1 AND nt.read = 1")
    int countReadByUserIdAndModuleName(@Param("userId") Long userId, @Param("moduleName") String moduleName);

    @Update("UPDATE linkx_open.tb_notification_targets nt " +
            "INNER JOIN linkx_open.tb_notification n ON nt.notification_id = n.id " +
            "SET nt.read = 1, nt.read_time = NOW() " +
            "WHERE nt.user_id = #{userId} AND n.module_name = #{moduleName} AND n.collaborative_msg = 1 AND nt.read = 0")
    int markAllReadByUserIdAndModuleName(@Param("userId") Long userId, @Param("moduleName") String moduleName);

    @Update("UPDATE linkx_open.tb_notification_targets " +
            "SET `read` = 1, read_time = NOW() " +
            "WHERE user_id = #{userId} AND notification_id = #{notificationId} AND `read` = 0")
    int markReadByUserIdAndNotificationId(@Param("userId") Long userId, @Param("notificationId") Long notificationId);
}
