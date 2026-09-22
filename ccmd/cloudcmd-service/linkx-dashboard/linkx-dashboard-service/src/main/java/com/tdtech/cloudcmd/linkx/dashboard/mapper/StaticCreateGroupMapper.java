package com.tdtech.cloudcmd.linkx.dashboard.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.linkx.dashboard.entity.StaticCreateGroup;
import com.tdtech.cloudcmd.linkx.dashboard.support.SyncWatermark;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 建群记录统计明细 Mapper
 * TableName：tb_static_create_group
 */
@Mapper
public interface StaticCreateGroupMapper extends BaseMapper<StaticCreateGroup> {

    /**
     * 批量 upsert（按 uniq_group_id 去重）。
     */
    int upsertBatch(@Param("list") List<StaticCreateGroup> list);

    /**
     * 兜底回查水位线：(group_updated_time, group_id) DESC LIMIT 1。
     */
    SyncWatermark selectMaxWatermark();

    List<StaticCreateGroup> selectByCursor(@Param("deptIds") List<Long> deptIds,
                                           @Param("startTime") Date startTime,
                                           @Param("endTime") Date endTime,
                                           @Param("lastTime") Date lastTime,
                                           @Param("lastId") Long lastId,
                                           @Param("pageSize") Integer pageSize);
}