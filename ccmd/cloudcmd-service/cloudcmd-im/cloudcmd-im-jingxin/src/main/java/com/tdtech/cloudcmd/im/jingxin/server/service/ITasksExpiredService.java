
package com.tdtech.cloudcmd.im.jingxin.server.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.cloudcmd.im.jingxin.server.entity.TasksExpired;
import com.tdtech.cloudcmd.im.jingxin.server.entity.TasksExpiredVO;

import java.util.List;

public interface ITasksExpiredService extends IService<TasksExpired> {

    Page<TasksExpiredVO> findPage(int pageNum, int pageSize, String keywords, String deptCode, String startTime, String endTime);

    Long countByCondition(List<Long> orgIdList, String startTime, String endTime);
}