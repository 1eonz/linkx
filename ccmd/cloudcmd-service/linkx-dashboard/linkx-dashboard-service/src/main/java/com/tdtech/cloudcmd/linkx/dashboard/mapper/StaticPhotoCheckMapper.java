package com.tdtech.cloudcmd.linkx.dashboard.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.linkx.dashboard.entity.StaticPhotoCheck;
import com.tdtech.cloudcmd.linkx.dashboard.support.SyncWatermark;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 人员核查统计明细 Mapper
 * TableName：tb_static_photo_check
 */
@Mapper
public interface StaticPhotoCheckMapper extends BaseMapper<StaticPhotoCheck> {

    /**
     * 批量 upsert（按 uniq_check_data_id 去重）。
     */
    int upsertBatch(@Param("list") List<StaticPhotoCheck> list);

    /**
     * 兜底回查水位线：(gmt_create_time, check_data_id) DESC LIMIT 1。
     */
    SyncWatermark selectMaxWatermark();

    List<StaticPhotoCheck> selectByCursor(@Param("deptIds") List<Long> deptIds,
                                          @Param("startTime") Date startTime,
                                          @Param("endTime") Date endTime,
                                          @Param("lastTime") Date lastTime,
                                          @Param("lastId") Long lastId,
                                          @Param("pageSize") Integer pageSize);
}