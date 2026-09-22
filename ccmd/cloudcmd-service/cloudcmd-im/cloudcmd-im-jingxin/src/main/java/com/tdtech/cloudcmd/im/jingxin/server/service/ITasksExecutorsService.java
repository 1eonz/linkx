
package com.tdtech.cloudcmd.im.jingxin.server.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksExecutors;

public interface ITasksExecutorsService extends IService<TasksExecutors> {
    boolean saveExecutors(Long taskId, List<TasksExecutors> dataList);

    boolean resetExecutors(Long taskId, List<TasksExecutors> dataList);

    boolean removeByTaskId(Long taskId);

    List<TasksExecutors> findList(Long taskId);

}