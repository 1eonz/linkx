
package com.tdtech.cloudcmd.im.jingxin.server.service;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.Tasks;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksCreateReq;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksDelReq;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksQueryReq;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksUpdateReq;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.MyTasksCountVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.TasksCountVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.TasksDateCountVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.TasksGroupQO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.TasksGroupVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.TasksStatusStatisticsVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.TasksTypeCountVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.TasksTypeStatisticsVO;

import javax.validation.constraints.NotNull;

public interface ITasksService extends IService<Tasks> {

    Page<TasksVO> tasksPage(int pageNum, int pageSize, TasksQueryReq tasksQueryReq);

    boolean delete(String taskNumber, TasksDelReq tasksDelReq);

    boolean update(String taskNumber, TasksUpdateReq tasksUpdateReq);

    TasksVO save(TasksCreateReq tasksCreateReq);

    void sendToCAgent(String notifyType, String taskNumber);

    Tasks findOne(String taskNumber);

    long count(String taskNumber);

    TasksVO detail(String taskNumber);

    List<TasksVO> details(List<String> taskNumbers);

    List<TasksCountVO> statusCount(Date startTime, Date endTime);

    List<String> typeList();

    List<TasksTypeCountVO> typeCount(Date startTime, Date endTime);

    List<TasksTypeStatisticsVO> statisticsByBusinessType(Date startTime, Date endTime);

    List<TasksStatusStatisticsVO> statisticsStatusByCompleteness(Date startTime, Date endTime);

    List<TasksDateCountVO> statisticsByDate(Date startTime, Date endTime);

    List<MyTasksCountVO> mineCount(Date startTime, Date endTime);

    Integer mineToDoCount();

    List<TasksTypeStatisticsVO> statisticsByCompleteness(Date startTime, Date endTime);

    List<TasksTypeStatisticsVO> avgDuration(Date startTime, Date endTime);

    Page<TasksGroupVO> groupPage(Page<TasksGroupVO> page, TasksGroupQO tasksGroupQO);

    boolean bindGroup(Long groupId, List<Long> tasksIds);

    boolean deleteBinding(Long groupId, Long taskId);

    Map<Long,Integer> countBinding(@NotNull List<Long> groupIdList);

}