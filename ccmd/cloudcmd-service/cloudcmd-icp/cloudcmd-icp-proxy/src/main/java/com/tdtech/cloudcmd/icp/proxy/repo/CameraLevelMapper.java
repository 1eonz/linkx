package com.tdtech.cloudcmd.icp.proxy.repo;

import com.tdtech.cloudcmd.icp.proxy.entity.CameraLevel;
import com.tdtech.cloudcmd.mysql.enhance.ExBaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CameraLevelMapper extends ExBaseMapper<CameraLevel> {

    /**
     * 批量插入摄像头层级信息
     * @param cameraLevelList 摄像头层级列表
     * @return 插入记录数
     */
    @Insert("<script>" +
            "INSERT INTO tb_camera_level (id, high_level_number, level, level_number, node_name, display_priority, level_number_path, create_time) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.id}, #{item.highLevelNumber}, #{item.level}, #{item.levelNumber}, #{item.nodeName}, #{item.displayPriority}, #{item.levelNumberPath}, #{item.createTime})" +
            "</foreach>" +
            "</script>")
    int insertBatch(@Param("list") List<CameraLevel> cameraLevelList);

    /**
     * 批量新增或更新摄像头层级信息（根据level_number判断，存在则更新，不存在则新增）
     * @param cameraLevelList 摄像头层级列表
     * @return 影响记录数
     */
    @Insert("<script>" +
            "INSERT INTO tb_camera_level (id, high_level_number, level, level_number, node_name, display_priority, level_number_path, create_time) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.id}, #{item.highLevelNumber}, #{item.level}, #{item.levelNumber}, #{item.nodeName}, #{item.displayPriority}, #{item.levelNumberPath}, #{item.createTime})" +
            "</foreach>" +
            " ON DUPLICATE KEY UPDATE " +
            "high_level_number = VALUES(high_level_number), " +
            "level = VALUES(level), " +
            "node_name = VALUES(node_name), " +
            "display_priority = VALUES(display_priority), " +
            "level_number_path = VALUES(level_number_path), " +
            "create_time = VALUES(create_time)" +
            "</script>")
    int insertOrUpdateBatch(@Param("list") List<CameraLevel> cameraLevelList);
}
