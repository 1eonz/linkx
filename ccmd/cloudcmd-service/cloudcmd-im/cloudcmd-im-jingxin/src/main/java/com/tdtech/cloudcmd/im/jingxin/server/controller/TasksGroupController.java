package com.tdtech.cloudcmd.im.jingxin.server.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.im.jingxin.server.entity.TasksGroupQO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.TasksGroupVO;
import com.tdtech.cloudcmd.im.jingxin.server.service.ITasksService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;
import java.util.List;

@RestController
@RequestMapping("/collaboration/v1/tasksgroup")
@RequiredArgsConstructor
@Tag(name = "任务关联协同群组管理", description = "任务关联协同群组操作接口")
public class TasksGroupController {

    private final ITasksService tasksService;

    /**
     * 分页查询协同群组关联任务列表
     *
     * @return 分页结果
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询协同群组关联任务列表", description = "根据条件分页查询协同群组关联任务列表")
    @Parameter(name = "current", description = "当前页码")
    @Parameter(name = "size", description = "每页条数")
    public R<Page<TasksGroupVO>> findPage(@RequestParam(name = "current") Integer current,
                                          @RequestParam(name = "size") Integer size, TasksGroupQO tasksGroupQO) {
        Page<TasksGroupVO> page = new Page<>(current, size);
        return R.success(tasksService.groupPage(page, tasksGroupQO));
    }

    @PostMapping("/groupbind/{groupId}")
    @Operation(summary = "任务关联，修改也是这个接口，每次传全量任务ID", description = "任务关联")
    @Parameter(name = "groupId", description = "分组ID", required = true)
    public R<Void> bindGroup(@PathVariable("groupId") @NotNull Long groupId,
                             @RequestBody @NotNull List<Long> tasksIds) {
        tasksService.bindGroup(groupId, tasksIds);
        return R.success();
    }

    @DeleteMapping("/groupbind/{groupId}/{taskId}")
    @Operation(summary = "删除任务关联", description = "删除任务关联")
    @Parameter(name = "groupId", description = "分组ID", required = true)
    public R<Void> deleteBinding(@NotNull @PathVariable("groupId") Long groupId,
                                 @NotNull @PathVariable("taskId") Long taskId) {
        tasksService.deleteBinding(groupId, taskId);
        return R.success();
    }
}
