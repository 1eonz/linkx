package com.tdtech.cloudcmd.im.jingxin.server.service.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.Tasks;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksQueryReq;
import com.tdtech.cloudcmd.im.jingxin.server.entity.TasksFavorite;

/**
 * @author ly
 * @date 2025/9/4
 **/
@Mapper
public interface TasksFavoriteMapper extends BaseMapper<TasksFavorite> {
    Page<Tasks> selectPageWithCondition(Page<Tasks> page, @Param("tasksQueryReq") TasksQueryReq tasksQueryReq);
}
