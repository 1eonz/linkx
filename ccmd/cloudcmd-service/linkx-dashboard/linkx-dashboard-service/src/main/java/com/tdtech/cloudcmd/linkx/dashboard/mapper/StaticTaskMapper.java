package com.tdtech.cloudcmd.linkx.dashboard.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.linkx.dashboard.entity.StaticTask;
import com.tdtech.cloudcmd.linkx.dashboard.support.SyncWatermark;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 协同任务统计明细 Mapper
 * TableName：tb_static_task
 */
@Mapper
public interface StaticTaskMapper extends BaseMapper<StaticTask> {

    /**
     * 批量 upsert（按 uniq_task_id 去重，INSERT ... ON DUPLICATE KEY UPDATE）。
     */
    int upsertBatch(@Param("list") List<StaticTask> list);

    /**
     * 兜底回查水位线：(task_updated_time, task_id) DESC LIMIT 1。
     */
    SyncWatermark selectMaxWatermark();

    List<StaticTask> selectByCursor(@Param("deptIds") List<Long> deptIds,
                                    @Param("startTime") Date startTime,
                                    @Param("endTime") Date endTime,
                                    @Param("lastTime") Date lastTime,
                                    @Param("lastId") Long lastId,
                                    @Param("pageSize") Integer pageSize);
}