package com.tdtech.cloudcmd.linkx.dashboard.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.linkx.dashboard.entity.StaticTaskResponse;
import com.tdtech.cloudcmd.linkx.dashboard.support.SyncWatermark;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 任务回复统计明细 Mapper
 * TableName：tb_static_task_response
 */
@Mapper
public interface StaticTaskResponseMapper extends BaseMapper<StaticTaskResponse> {

    /**
     * 批量 upsert（按 uniq_response_id 去重）。
     */
    int upsertBatch(@Param("list") List<StaticTaskResponse> list);

    /**
     * 兜底回查水位线：(response_time, response_id) DESC LIMIT 1。
     */
    SyncWatermark selectMaxWatermark();

    List<StaticTaskResponse> selectByCursor(@Param("deptIds") List<Long> deptIds,
                                            @Param("startTime") Date startTime,
                                            @Param("endTime") Date endTime,
                                            @Param("lastTime") Date lastTime,
                                            @Param("lastId") Long lastId,
                                            @Param("pageSize") Integer pageSize);
}