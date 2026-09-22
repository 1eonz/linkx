package com.tdtech.cloudcmd.im.jingxin.server.rpc;

import javax.annotation.Resource;

import org.apache.dubbo.config.annotation.DubboService;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.im.jingxin.api.TaskRpcApi;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksCreateReq;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksDelReq;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksFavoriteCreateReq;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksFavoriteDelReq;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksProcesses;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksProcessesCreateReq;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksQueryReq;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksUpdateReq;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksVO;
import com.tdtech.cloudcmd.im.jingxin.server.service.ITasksFavoriteService;
import com.tdtech.cloudcmd.im.jingxin.server.service.ITasksProcessesService;
import com.tdtech.cloudcmd.im.jingxin.server.service.ITasksService;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@DubboService
public class TaskRpc implements TaskRpcApi {

    @Resource
    private ITasksService tasksService;

    @Resource
    private ITasksProcessesService tasksProcessesService;

    @Resource
    private ITasksFavoriteService tasksFavoriteService;

    /**
     * 分页查询任务
     */
    @Override
    public Page<TasksVO> tasksPage(int page, int pageSize, TasksQueryReq queryReq) {
        return (tasksService.tasksPage(page, pageSize, queryReq));
    }

    /**
     * 删除任务
     */
    @Override
    public void deleteTasks(String taskNumber, TasksDelReq tasksDelReq) {
        tasksService.delete(taskNumber, tasksDelReq);
    }

    /**
     * 修改任务
     */
    @Override
    public void updateTasks(String taskNumber, TasksUpdateReq tasksUpdateReq) {
        tasksService.update(taskNumber, tasksUpdateReq);
    }

    /**
     * 查询任务详情
     * 
     * @param taskNumber
     * @return
     */
    @Override
    public TasksVO tasksDetail(String taskNumber) {
        return tasksService.detail(taskNumber);
    }

    /**
     * 查询任务详情
     *
     * @param taskNumbers 任务编号列表
     * @return List<TasksVO> 任务详情列表
     */
    @Override
    public List<TasksVO> tasksDetails(List<String> taskNumbers) {
        return tasksService.details(taskNumbers);
    }

    /**
     * 新增任务
     * 
     * @param tasksCreateReq
     * @return
     */
    @Override
    public TasksVO saveTasks(TasksCreateReq tasksCreateReq) {
        return tasksService.save(tasksCreateReq);
    }

    /**
     * 任务处置
     * 
     * @param taskNumber
     * @param tasksProcessesCreateReq
     * @return
     */
    @Override
    public void saveTasksProcesses(String taskNumber, TasksProcessesCreateReq tasksProcessesCreateReq) {
        tasksProcessesService.save(taskNumber, tasksProcessesCreateReq);
    }

    /**
     * 分页查询任务处理记录
     * 
     * @param taskNumber
     * @param page
     * @param pageSize
     * @return
     */
    @Override
    public Page<TasksProcesses> tasksProcessesPage(String taskNumber, int page, int pageSize) {
        return tasksProcessesService.page(page, pageSize, taskNumber);
    }

    /**
     * 收藏任务
     * 
     * @param taskNumber
     * @param tasksFavoriteCreateReq
     * @return
     */
    @Override
    public void saveFavorite(String taskNumber, TasksFavoriteCreateReq tasksFavoriteCreateReq) {
        tasksFavoriteService.save(taskNumber, tasksFavoriteCreateReq);
    }

    /**
     * 取消收藏
     * 
     * @param taskNumber
     * @param tasksFavoriteDelReq
     * @return
     */
    @Override
    public void deleteFavorite(String taskNumber, TasksFavoriteDelReq tasksFavoriteDelReq) {
        tasksFavoriteService.delete(taskNumber, tasksFavoriteDelReq);
    }

}
