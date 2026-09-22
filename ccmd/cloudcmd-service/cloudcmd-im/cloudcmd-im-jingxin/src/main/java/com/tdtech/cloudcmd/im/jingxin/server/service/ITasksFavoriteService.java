
package com.tdtech.cloudcmd.im.jingxin.server.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksFavoriteCreateReq;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksFavoriteDelReq;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksQueryReq;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.TasksFavorite;

public interface ITasksFavoriteService extends IService<TasksFavorite> {

    Page<TasksVO> page(int pageNum, int pageSize, TasksQueryReq tasksQueryReq);

    String save(String taskNumber, TasksFavoriteCreateReq tasksFavoriteCreateReq);

    boolean delete(String taskNumber, TasksFavoriteDelReq tasksFavoriteDelReq);

    TasksFavorite findOne(Long taskId, String idCard);

    List<Long> favoriteTaskIdList(List<Long> taskIdList, String idCard);

}