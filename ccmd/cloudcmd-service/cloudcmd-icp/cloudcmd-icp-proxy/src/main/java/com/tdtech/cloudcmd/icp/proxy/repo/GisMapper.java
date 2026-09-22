package com.tdtech.cloudcmd.icp.proxy.repo;

import com.tdtech.cloudcmd.icp.proxy.entity.Gis;
import com.tdtech.cloudcmd.mysql.enhance.ExBaseMapper;
import com.tdtech.cloudcmd.util.CollectionUtils;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface GisMapper extends ExBaseMapper<Gis> {

    @Insert(
        "<script>" + "INSERT INTO tb_gis_status (id, isdn, plmnid, devicetype, speed, direction, location, gpsstar, locateMode, cellid, cellname, lat, lon, alt, create_time) VALUES " + "<foreach collection='list' item='item' separator=','>" + "(#{item.id}, #{item.isdn}, #{item.plmnid}, #{item.devicetype}, #{item.speed}, #{item.direction}, #{item.location}, #{item.gpsstar}, #{item.locateMode}, #{item.cellid}, #{item.cellname}, #{item.lat}, #{item.lon}, #{item.alt}, #{item.createTime})" + "</foreach>" + "</script>")
    int _insertBatch(@Param("list") List<Gis> gisList);

    default void insertBatch(@Param("list") Collection<Gis> gisList) {
        if (gisList == null || gisList.isEmpty()) {
            return;
        }
        var group = CollectionUtils.group(gisList, 200);
        for (var value : group.values()) {
            _insertBatch(value);
        }
    }

    @Insert(
        "<script>" + "INSERT INTO tb_gis_status (id, isdn, plmnid, devicetype, speed, direction, location, gpsstar, locateMode, cellid, cellname, lat, lon, alt, create_time) VALUES " + "<foreach collection='list' item='item' separator=','>" + "(#{item.id}, #{item.isdn}, #{item.plmnid}, #{item.devicetype}, #{item.speed}, #{item.direction}, #{item.location}, #{item.gpsstar}, #{item.locateMode}, #{item.cellid}, #{item.cellname}, #{item.lat}, #{item.lon}, #{item.alt}, #{item.createTime})" + "</foreach>" + " ON DUPLICATE KEY UPDATE " + "plmnid = VALUES(plmnid), " + "devicetype = VALUES(devicetype), " + "speed = VALUES(speed), " + "direction = VALUES(direction), " + "location = VALUES(location), " + "gpsstar = VALUES(gpsstar), " + "locateMode = VALUES(locateMode), " + "cellid = VALUES(cellid), " + "cellname = VALUES(cellname), " + "lat = VALUES(lat), " + "lon = VALUES(lon), " + "alt = VALUES(alt), " + "create_time = VALUES(create_time)" + "</script>")
    int _insertOrUpdateBatch(@Param("list") List<Gis> gisList);

    default void insertOrUpdateBatch(@Param("list") Collection<Gis> gisList) {
        if (gisList == null || gisList.isEmpty()) {
            return;
        }
        var group = CollectionUtils.group(gisList, 200);
        for (var value : group.values()) {
            _insertOrUpdateBatch(value);
        }
    }
}
