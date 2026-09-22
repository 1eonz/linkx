package com.tdtech.cloudcmd.im.jingxin.server.service.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksProcesses;

/**
 * @author lsc
 * @date 2025/7/18
 **/
@Mapper
public interface TasksProcessesMapper extends BaseMapper<TasksProcesses> {

    /**
     * 分页查询任务处理记录（走自定义 resultMap，确保 nextExecutors 字段的 typeHandler 生效）
     *
     * @param page       分页参数
     * @param taskNumber 任务编号
     * @return 分页结果
     */
    Page<TasksProcesses> selectProcessesPage(Page<TasksProcesses> page, @Param("taskNumber") String taskNumber);

    /**
     * 查询备注不为空的任务处理记录列表（走自定义 resultMap，确保 nextExecutors 字段的 typeHandler 生效）
     *
     * @param taskNumber 任务编号
     * @return 处理记录列表
     */
    List<TasksProcesses> selectProcessesList(@Param("taskNumber") String taskNumber);

}