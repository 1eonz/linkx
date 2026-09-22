package com.tdtech.cloudcmd.linkx.dashboard.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.linkx.dashboard.entity.StatisticLogin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 客户端登录信息记录表 Mapper
 * TableName：tb_statistic_login
 */
@Mapper
public interface StatisticLoginMapper extends BaseMapper<StatisticLogin> {
    /**
     * 根据登录时间段 + 游标分页查询
     *
     * @param startTime 登录开始时间
     * @param endTime   登录结束时间
     * @param lastId    上一批最后一条id（首次传0）
     * @param pageSize  每页条数
     */
    List<StatisticLogin> selectByLoginTimeAndCursor(
            @Param("startTime") Date startTime,
            @Param("endTime") Date endTime,
            @Param("lastId") Long lastId,
            @Param("pageSize") Integer pageSize);
}
