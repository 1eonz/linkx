package com.tdtech.cloudcmd.im.jingxin.server.service.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import com.tdtech.cloudcmd.im.jingxin.server.entity.TrTasksGroup;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TrTasksGroupMapper extends MPJBaseMapper<TrTasksGroup> {
    @Insert("<script>" + //
            "INSERT INTO tr_tasks_group (id, task_id, group_id) VALUES" + //
            "    <foreach collection='list' item='item' separator=','>" + //
            "        (#{item.id}, #{item.taskId}, #{item.groupId})" + //
            "    </foreach>" + //
            "</script>")
    int insertBatch(@Param("list") List<TrTasksGroup> list);

}
