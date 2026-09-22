package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import javax.annotation.Resource;

import com.tdtech.cloudcmd.enums.NotifyTypeEnum;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.Tasks;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksExecutors;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksProcesses;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksProcessesCreateReq;
import com.tdtech.cloudcmd.im.jingxin.server.enums.TasksActionEnum;
import com.tdtech.cloudcmd.im.jingxin.server.enums.TasksStatusEnum;
import com.tdtech.cloudcmd.im.jingxin.server.service.ITasksExecutorsService;
import com.tdtech.cloudcmd.im.jingxin.server.service.ITasksProcessesService;
import com.tdtech.cloudcmd.im.jingxin.server.service.ITasksService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.TasksProcessesMapper;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.IdWorker;

import lombok.extern.slf4j.Slf4j;

/**
 * @author ly
 * @date 2025/8/25
 **/
@Slf4j
@Service
public class TasksProcessesServiceImpl extends ServiceImpl<TasksProcessesMapper, TasksProcesses>
    implements ITasksProcessesService {
    @Resource
    private TasksProcessesMapper tasksProcessesMapper;

    @Resource
    private ITasksService tasksService;

    @Resource
    private ITasksExecutorsService tasksExecutorsService;

    @Resource
    private IdWorker idWorker;

    @Override
    public Page<TasksProcesses> page(int pageNum, int pageSize, String taskNumber) {
        Page<TasksProcesses> page = new Page<>(pageNum, pageSize);
        // 走自定义查询，确保 nextExecutors 字段的 typeHandler 生效
        return tasksProcessesMapper.selectProcessesPage(page, taskNumber);
    }

    @Override
    public List<TasksProcesses> list(String taskNumber) {
        // 走自定义查询，确保 nextExecutors 字段的 typeHandler 生效
        return tasksProcessesMapper.selectProcessesList(taskNumber);
    }

    @Override
    public boolean save(String taskNumber, TasksProcessesCreateReq tasksProcessesCreateReq) {
        Tasks oldTasks = tasksService.findOne(taskNumber);
        if (Objects.isNull(oldTasks)) {
            throw new BusinessException("任务不存在");
        }
        String status = oldTasks.getStatus();
        // 具体status的值待调试
        if (StringUtils.isNotBlank(status) && TasksStatusEnum.COMPLETED.getMsg().equals(status)) {
            throw new BusinessException("任务已完成");
        }
        if (StringUtils.isNotBlank(status) && TasksStatusEnum.DISCARDED.getMsg().equals(status)) {
            throw new BusinessException("任务已作废");
        }
        Date now = new Date();
        if (TasksActionEnum.COMPLETED.getCode() == tasksProcessesCreateReq.getAction()) {
            oldTasks.setCompleteTime(now);
        }

        TasksProcesses processes = new TasksProcesses();
        BeanUtils.copyProperties(tasksProcessesCreateReq, processes);
        processes.setId(idWorker.nextId());
        processes.setGmtCreated(now);

        List<TasksExecutors> nextExecutors = tasksProcessesCreateReq.getNextExecutors();
        if (CollectionUtils.isNotEmpty(nextExecutors)) {
            tasksExecutorsService.resetExecutors(oldTasks.getId(), nextExecutors);
            processes.setNextExecutors(nextExecutors);
        }
        oldTasks.setStatus(tasksProcessesCreateReq.getStatus());

        save(processes);
        tasksService.updateById(oldTasks);
        tasksService.sendToCAgent(NotifyTypeEnum.STATUS_CHANGE.getCode(), oldTasks.getNumber());
        return true;
    }
}