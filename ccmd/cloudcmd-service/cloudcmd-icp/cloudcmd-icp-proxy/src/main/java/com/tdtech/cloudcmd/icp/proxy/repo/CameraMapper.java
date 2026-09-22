package com.tdtech.cloudcmd.icp.proxy.repo;

import com.tdtech.cloudcmd.icp.proxy.entity.Camera;
import com.tdtech.cloudcmd.icp.proxy.entity.IsdnType;
import com.tdtech.cloudcmd.mysql.enhance.ExMPJBaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CameraMapper extends ExMPJBaseMapper<Camera> {

    /**
     * 批量插入摄像头信息
     *
     * @param cameraList 摄像头列表
     * @return 插入记录数
     */
    @Insert("<script>" +//
        "INSERT INTO tb_camera (id, ipc_privilege, alias, apptype, category, departmentid, ipctype, isdn, level_name, level_number, level_number_list, location, name, priority, shape, subusercategory, uetype, vpnid, vpnin, vpnout, create_time, lat, lon, alt) VALUES " + //
        "<foreach collection='list' item='item' separator=','>" + //
        "(#{item.id}, #{item.ipcPrivilege}, #{item.alias}, #{item.apptype}, #{item.category}, #{item.departmentid}, #{item.ipctype}, #{item.isdn}, #{item.levelName}, #{item.levelNumber}, #{item.levelNumberList}, #{item.location}, #{item.name}, #{item.priority}, #{item.shape}, #{item.subusercategory}, #{item.uetype}, #{item.vpnid}, #{item.vpnin}, #{item.vpnout}, #{item.createTime}, #{item.lat}, #{item.lon}, #{item.alt})" +//
        "</foreach>" + //
        "</script>")
    int insertBatch(@Param("list") List<Camera> cameraList);

    /**
     * 批量新增或更新摄像头信息（根据isdn判断，存在则更新，不存在则新增）
     *
     * @param cameraList 摄像头列表
     * @return 影响记录数
     */
    @Insert("<script>" +//
        "INSERT INTO tb_camera (id, ipc_privilege, alias, apptype, category, departmentid, ipctype, isdn, level_name, level_number, level_number_list, location, name, priority, shape, subusercategory, uetype, vpnid, vpnin, vpnout, create_time, lat, lon, alt) VALUES " + //
        "<foreach collection='list' item='item' separator=','>" + //
        "(#{item.id}, #{item.ipcPrivilege}, #{item.alias}, #{item.apptype}, #{item.category}, #{item.departmentid}, #{item.ipctype}, #{item.isdn}, #{item.levelName}, #{item.levelNumber}, #{item.levelNumberList}, #{item.location}, #{item.name}, #{item.priority}, #{item.shape}, #{item.subusercategory}, #{item.uetype}, #{item.vpnid}, #{item.vpnin}, #{item.vpnout}, #{item.createTime}, #{item.lat}, #{item.lon}, #{item.alt})" +//
        "</foreach>" + //
        " ON DUPLICATE KEY UPDATE " +
        "ipc_privilege = VALUES(ipc_privilege), " +
        "alias = VALUES(alias), " +
        "apptype = VALUES(apptype), " +
        "category = VALUES(category), " +
        "departmentid = VALUES(departmentid), " +
        "ipctype = VALUES(ipctype), " +
        "level_name = VALUES(level_name), " +
        "level_number = VALUES(level_number), " +
        "level_number_list = VALUES(level_number_list), " +
        "location = VALUES(location), " +
        "name = VALUES(name), " +
        "priority = VALUES(priority), " +
        "shape = VALUES(shape), " +
        "subusercategory = VALUES(subusercategory), " +
        "uetype = VALUES(uetype), " +
        "vpnid = VALUES(vpnid), " +
        "vpnin = VALUES(vpnin), " +
        "vpnout = VALUES(vpnout), " +
        "create_time = VALUES(create_time), " +
        "lat = VALUES(lat), " +
        "lon = VALUES(lon), " +
        "alt = VALUES(alt)" +
        "</script>")
    int insertOrUpdateBatch(@Param("list") List<Camera> cameraList);

    /**
     * 查询tb_camera表中category, subusercategory, apptype去重后的结果
     * @return 去重后的ISDN类型列表（仅包含category, subusercategory, apptype字段）
     */
    @Select("SELECT DISTINCT category, subusercategory, apptype FROM tb_camera")
    List<IsdnType> selectDistinctKey();

}