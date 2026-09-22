package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksExecutors;
import com.tdtech.cloudcmd.im.jingxin.server.service.ITasksExecutorsService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.TasksExecutorsMapper;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.IdWorker;

import lombok.extern.slf4j.Slf4j;

/**
 * @author ly
 * @date 2025/8/25
 **/
@Slf4j
@Service
public class TasksExecutorsServiceImpl extends ServiceImpl<TasksExecutorsMapper, TasksExecutors>
    implements ITasksExecutorsService {
    @Resource
    private TasksExecutorsMapper tasksExecutorsMapper;

    @Resource
    private IdWorker idWorker;

    @Override
    public boolean saveExecutors(Long taskId, List<TasksExecutors> dataList) {
        Date now = new Date();
        dataList.stream().forEach(data -> {
            data.setId(idWorker.nextId());
            data.setTaskId(taskId);
            data.setOperateTime(now);
        });
        return saveBatch(dataList);
    }

    @Override
    public boolean resetExecutors(Long taskId, List<TasksExecutors> dataList) {
        if (Objects.isNull(taskId) || CollectionUtils.isEmpty(dataList)) {
            return false;
        }
        removeByTaskId(taskId);
        return saveExecutors(taskId, dataList);
    }



    @Override
    public boolean removeByTaskId(Long taskId) {
        if (Objects.isNull(taskId)) {
            return false;
        }
        LambdaQueryWrapper<TasksExecutors> queryWrapper =
            new LambdaQueryWrapper<TasksExecutors>().eq(TasksExecutors::getTaskId, taskId);
        return remove(queryWrapper);
    }

    @Override
    public List<TasksExecutors> findList(Long taskId) {
        LambdaQueryWrapper<TasksExecutors> queryWrapper =
            new LambdaQueryWrapper<TasksExecutors>().eq(TasksExecutors::getTaskId, taskId);
        return list(queryWrapper);
    }
}