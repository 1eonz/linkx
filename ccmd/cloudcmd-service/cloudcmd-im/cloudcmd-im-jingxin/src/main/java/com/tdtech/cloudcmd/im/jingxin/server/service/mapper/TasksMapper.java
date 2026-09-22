package com.tdtech.cloudcmd.im.jingxin.server.service.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.base.MPJBaseMapper;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.Tasks;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksQueryReq;
import com.tdtech.cloudcmd.im.jingxin.server.entity.TasksToDoCountVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.TasksTypeCountVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * @author ly
 * @date 2025/8/4
 **/
@Mapper
public interface TasksMapper extends MPJBaseMapper<Tasks> {
    List<Tasks> findByIdCard(@Param("startTime") Date startTime, @Param("endTime") Date endTime,
        @Param("idCard") String idCard);

    List<Tasks> findToDoList(@Param("startTime") Date startTime, @Param("endTime") Date endTime,
        @Param("idCard") String idCard);

    List<TasksTypeCountVO> countByIdCard(@Param("startTime") Date startTime, @Param("endTime") Date endTime,
        @Param("idCard") String idCard, @Param("limit") Integer limit);

    List<String> typeByIdCard(@Param("idCard") String idCard);

    List<Tasks> findByOrgCodeList(@Param("startTime") Date startTime, @Param("endTime") Date endTime,
        @Param("orgCodeList") List<String> orgCodeList);

    Page<Tasks> selectPageWithCondition(Page<Tasks> page, @Param("tasksQueryReq") TasksQueryReq tasksQueryReq);

    List<TasksToDoCountVO> countToDoTasks(@Param("idCardList") List<String> idCardList);

    Integer countMyToDoTask(@Param("idCard") String idCard);
}
