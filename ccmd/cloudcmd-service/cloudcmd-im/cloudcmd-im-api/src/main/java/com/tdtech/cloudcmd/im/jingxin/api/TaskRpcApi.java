package com.tdtech.cloudcmd.im.jingxin.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksCreateReq;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksDelReq;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksFavoriteCreateReq;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksFavoriteDelReq;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksProcesses;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksProcessesCreateReq;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksQueryReq;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksUpdateReq;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksVO;

import java.util.List;

public interface TaskRpcApi {
    Page<TasksVO> tasksPage(int page, int pageSize, TasksQueryReq queryReq);

    void deleteTasks(String taskNumber, TasksDelReq tasksDelReq);

    void updateTasks(String taskNumber, TasksUpdateReq tasksUpdateReq);

    TasksVO tasksDetail(String taskNumber);

    List<TasksVO> tasksDetails(List<String> taskNumbers);

    TasksVO saveTasks(TasksCreateReq tasksCreateReq);

    void saveTasksProcesses(String taskNumber, TasksProcessesCreateReq tasksProcessesCreateReq);

    Page<TasksProcesses> tasksProcessesPage(String taskNumber, int page, int pageSize);

    void saveFavorite(String taskNumber, TasksFavoriteCreateReq tasksFavoriteCreateReq);

    void deleteFavorite(String taskNumber, TasksFavoriteDelReq tasksFavoriteDelReq);
}
