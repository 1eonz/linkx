package com.tdtech.cloudcmd.icp.proxy.repo;

import com.tdtech.cloudcmd.icp.proxy.entity.OnlineStatus;
import com.tdtech.cloudcmd.mysql.enhance.ExBaseMapper;
import com.tdtech.cloudcmd.util.CollectionUtils;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OnlineStatusMapper extends ExBaseMapper<OnlineStatus> {

    default void insertBatch(@Param("list") List<OnlineStatus> onlineStatusList) {
        if (onlineStatusList == null || onlineStatusList.isEmpty()) {
            return;
        }
        var group = CollectionUtils.group(onlineStatusList, 200);
        for (var value : group.values()) {
            _insertBatch(value);
        }
    }

    @Insert(//
        "<script>" + //
            "INSERT INTO tb_online_status (id, status_value, status_type, attaching, isdn, seq) VALUES " + //
            "<foreach collection='list' item='item' separator=','>" + "(#{item.id}, #{item.statusValue}, #{item.statusType}, #{item.attaching}, #{item.isdn}, #{item.seq})" +//
            "</foreach>" + //
            "</script>")
    void _insertBatch(@Param("list") List<OnlineStatus> onlineStatusList);

    default void insertOrUpdateBatch(@Param("list") List<OnlineStatus> onlineStatusList) {
        if (onlineStatusList == null || onlineStatusList.isEmpty()) {
            return;
        }
        var group = CollectionUtils.group(onlineStatusList, 200);
        for (var value : group.values()) {
            _insertOrUpdateBatch(value);
        }
    }

    @Insert(//
        "<script>" + //
            "INSERT INTO tb_online_status (id, status_value, status_type, attaching, isdn, seq) VALUES " + //
            "<foreach collection='list' item='item' separator=','>" + "(#{item.id}, #{item.statusValue}, #{item.statusType}, #{item.attaching}, #{item.isdn}, #{item.seq})" +//
            "</foreach>" + //
            " ON DUPLICATE KEY UPDATE " +
            "status_value = VALUES(status_value), " +
            "status_type = VALUES(status_type), " +
            "attaching = VALUES(attaching), " +
            "seq = VALUES(seq)" +
            "</script>")
    void _insertOrUpdateBatch(@Param("list") List<OnlineStatus> onlineStatusList);
}
