package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImDepartment;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.Tasks;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksFavoriteCreateReq;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksFavoriteDelReq;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksQueryReq;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.TasksFavorite;
import com.tdtech.cloudcmd.im.jingxin.server.service.ITasksFavoriteService;
import com.tdtech.cloudcmd.im.jingxin.server.service.ITasksService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.TasksFavoriteMapper;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.IdWorker;

import lombok.extern.slf4j.Slf4j;

/**
 * @author ly
 * @date 2025/8/25
 **/
@Slf4j
@Service
public class TasksFavoriteServiceImpl extends ServiceImpl<TasksFavoriteMapper, TasksFavorite>
    implements ITasksFavoriteService {
    @Resource
    private TasksFavoriteMapper tasksFavoriteMapper;

    @Resource
    private ITasksService tasksService;

    @Resource
    private IdWorker idWorker;

    @Resource
    private OrganizationDiversionService organizationDiversionService;

    @Override
    public Page<TasksVO> page(int pageNum, int pageSize, TasksQueryReq tasksQueryReq) {
        Page<Tasks> page = new Page<>(pageNum, pageSize);

        Page<Tasks> dataPage = tasksFavoriteMapper.selectPageWithCondition(page, tasksQueryReq);
        List<Tasks> records = dataPage.getRecords();
        List<TasksVO> dataList = records.stream().map(task -> {
            TasksVO vo = TasksVO.fromTasksView(task);
            vo.setFavorite(true);
            return vo;
        }).collect(Collectors.toList());
        Page<TasksVO> resultPage = new Page<>();
        resultPage.setTotal(dataPage.getTotal());
        resultPage.setPages(dataPage.getPages());
        resultPage.setCurrent(dataPage.getCurrent());
        resultPage.setSize(dataPage.getSize());
        resultPage.setRecords(dataList);
        return resultPage;
    }

    @Override
    public boolean delete(String taskNumber, TasksFavoriteDelReq tasksFavoriteDelReq) {
        // 从后台提取用户信息
        if(StringUtils.isBlank(tasksFavoriteDelReq.getOpUserId())){
            UserInfo user = SecurityUtils.getUser();
            if (Objects.isNull(user)) {
                throw new SecurityUtils.UnAuthException("access token invalid");
            }
            tasksFavoriteDelReq.setOpUserId(user.getIdCardNum());
        }
        Tasks oldTasks = tasksService.findOne(taskNumber);
        if (Objects.isNull(oldTasks)) {
            throw new BusinessException("任务不存在");
        }
        String idCard = tasksFavoriteDelReq.getOpUserId();
        if (StringUtils.isBlank(idCard)) {
            return false;
        }
        TasksFavorite myFavorite = findOne(oldTasks.getId(), tasksFavoriteDelReq.getOpUserId());
        if (Objects.isNull(myFavorite)) {
            throw new BusinessException("收藏记录不存在");
        }
        removeById(myFavorite.getId());
        return true;
    }

    @Override
    public TasksFavorite findOne(Long taskId, String idCard) {
        LambdaQueryWrapper<TasksFavorite> queryWrapper = new LambdaQueryWrapper<TasksFavorite>()
            .eq(TasksFavorite::getTaskId, taskId).eq(TasksFavorite::getOpUserId, idCard);
        return getOne(queryWrapper);
    }

    @Override
    public List<Long> favoriteTaskIdList(List<Long> taskIdList, String idCard) {
        if (CollectionUtils.isEmpty(taskIdList) || StringUtils.isBlank(idCard)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<TasksFavorite> queryWrapper = new LambdaQueryWrapper<TasksFavorite>()
            .in(TasksFavorite::getTaskId, taskIdList).eq(TasksFavorite::getOpUserId, idCard);
        return list(queryWrapper).stream().map(TasksFavorite::getTaskId).distinct().collect(Collectors.toList());
    }

    @Override
    public String save(String taskNumber, TasksFavoriteCreateReq tasksFavoriteCreateReq) {

        if (StringUtils.isBlank(tasksFavoriteCreateReq.getOpUserId())) {
            UserInfo user = SecurityUtils.getUser();
            if (Objects.isNull(user)) {
                throw new SecurityUtils.UnAuthException("access token invalid");
            }
            tasksFavoriteCreateReq.setOpUserId(user.getIdCardNum());
            tasksFavoriteCreateReq.setOpUserName(user.getUserName());
            ImDepartment department = organizationDiversionService.findOne(user.getOrganizationCode());
            if (department != null) {
                tasksFavoriteCreateReq.setOpUserDepartment(department.getName());
            }
        }

        Tasks oldTasks = tasksService.findOne(taskNumber);
        if (Objects.isNull(oldTasks)) {
            String msg = String.format("编号: %s的任务不存在", taskNumber);
            throw new BusinessException(msg);
        }
        TasksFavorite myFavorite = findOne(oldTasks.getId(), tasksFavoriteCreateReq.getOpUserId());
        if (Objects.nonNull(myFavorite)) {
            throw new BusinessException("该任务已被您收藏过了");
        }
        Date now = new Date();
        TasksFavorite tasksFavorite = TasksFavorite.builder().taskId(oldTasks.getId())
            .opUserId(tasksFavoriteCreateReq.getOpUserId()).opUserName(tasksFavoriteCreateReq.getOpUserName())
            .opUserDepartment(tasksFavoriteCreateReq.getOpUserDepartment()).build();
        tasksFavorite.setId(idWorker.nextId());
        tasksFavorite.setOperateTime(now);
        save(tasksFavorite);
        return taskNumber;
    }

}