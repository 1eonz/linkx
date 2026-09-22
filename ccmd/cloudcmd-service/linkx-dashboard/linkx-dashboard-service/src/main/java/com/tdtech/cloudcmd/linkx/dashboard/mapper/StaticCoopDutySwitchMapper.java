package com.tdtech.cloudcmd.linkx.dashboard.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.linkx.dashboard.entity.StaticCoopDutySwitch;
import com.tdtech.cloudcmd.linkx.dashboard.support.SyncWatermark;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 协同岗上下岗统计明细 Mapper
 * TableName：tb_static_coop_duty_switch
 */
@Mapper
public interface StaticCoopDutySwitchMapper extends BaseMapper<StaticCoopDutySwitch> {

    /**
     * 批量 upsert（按 uniq_attendance_id 去重）。
     */
    int upsertBatch(@Param("list") List<StaticCoopDutySwitch> list);

    /**
     * 兜底回查水位线：(gmt_create_time, attendance_id) DESC LIMIT 1。
     */
    SyncWatermark selectMaxWatermark();

    List<StaticCoopDutySwitch> selectByCursor(@Param("deptIds") List<Long> deptIds,
                                              @Param("startTime") Date startTime,
                                              @Param("endTime") Date endTime,
                                              @Param("lastTime") Date lastTime,
                                              @Param("lastId") Long lastId,
                                              @Param("pageSize") Integer pageSize);
}