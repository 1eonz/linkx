
package com.tdtech.cloudcmd.im.jingxin.server.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksProcesses;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksProcessesCreateReq;

import java.util.List;

public interface ITasksProcessesService extends IService<TasksProcesses> {

    Page<TasksProcesses> page(int pageNum, int pageSize, String taskNumber);

    List<TasksProcesses> list(String taskNumber);

    boolean save(String taskNumber, TasksProcessesCreateReq tasksProcessesCreateReq);
}