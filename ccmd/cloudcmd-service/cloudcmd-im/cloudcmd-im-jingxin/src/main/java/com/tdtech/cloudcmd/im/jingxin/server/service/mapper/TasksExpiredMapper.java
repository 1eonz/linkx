package com.tdtech.cloudcmd.im.jingxin.server.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.im.jingxin.server.entity.TasksExpired;
import com.tdtech.cloudcmd.im.jingxin.server.entity.TasksExpiredVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TasksExpiredMapper extends BaseMapper<TasksExpired> {
    Page<TasksExpiredVO> findPage(
            Page<TasksExpiredVO> page,
            @Param("keywords") String keywords,
            @Param("orgCodeList") List<String> orgCodeList,
            @Param("startTime") String startTime,
            @Param("endTime") String endTime);

    Long countByCondition(@Param("orgIdList") List<Long> orgIdList,
               @Param("startTime") String startTime,
               @Param("endTime") String endTime);
}
