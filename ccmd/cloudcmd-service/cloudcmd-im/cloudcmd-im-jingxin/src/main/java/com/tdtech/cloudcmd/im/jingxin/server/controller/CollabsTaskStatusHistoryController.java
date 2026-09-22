package com.tdtech.cloudcmd.im.jingxin.server.controller;

import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationTaskStatusHistoryCO;
import com.tdtech.cloudcmd.im.jingxin.server.service.ICollaborationTaskStatusHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Slf4j
@RequestMapping("/collaboration/v1/tasks/status/history")
@RestController
@RequiredArgsConstructor
@Tag(name = "协作任务状态历史管理", description = "提供协作任务状态历史记录的保存功能")
public class CollabsTaskStatusHistoryController {

    @Resource
    private ICollaborationTaskStatusHistoryService collaborationTaskStatusHistoryService;

    @PostMapping("/save")
    @Operation(summary = "保存协作任务状态历史", description = "接收并保存协作任务的状态变更历史记录")
    public R<Void> addCollabsTaskStatusHistory(@RequestBody CollaborationTaskStatusHistoryCO collaborationTaskStatusHistoryCO) {
        collaborationTaskStatusHistoryService.save(collaborationTaskStatusHistoryCO);
        return R.success();
    }
}
