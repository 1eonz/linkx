package com.tdtech.cloudcmd.im.jingxin.server.controller;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tdtech.cloudcmd.bean.PageResult;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.enums.ResponseCodeEnum;
import com.tdtech.cloudcmd.i18n.I18nUtil;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.CollaborationOverdueVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CTaskRespVo;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationTask;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationTaskCO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollabsTaskListReqCO;
import com.tdtech.cloudcmd.im.jingxin.server.service.ICollaborationTaskService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequestMapping("/collaboration/v1/tasks")
@RestController
@Tag(name = "协同任务")
@RequiredArgsConstructor
public class CollabsTaskController {

    @Resource
    private ICollaborationTaskService collaborationTaskService;

    @PostMapping("/save")
    public R<Void> addCollabsTask(@RequestBody CollaborationTaskCO collaborationTaskCO) {
        collaborationTaskService.save(collaborationTaskCO);
        return R.success();
    }

    @GetMapping("")
    public R<PageResult<CollaborationTask>> getCollabsTaskList(
        @RequestParam(name = "type", required = false) Integer type,
        @RequestParam(name = "keywords", required = false) String keywords,
        @RequestParam(name = "pageSize", required = false) Integer pageSize,
        @RequestParam(name = "page", required = false) Integer page,
        @RequestParam(name = "userId", required = false) Long userId,
        @RequestParam(name = "postId", required = false) Long postId,
        @RequestParam(name = "postIds", required = false) String postIds) {
        PageResult<CollaborationTask> data =
            collaborationTaskService.listTask(new CollabsTaskListReqCO(type, keywords, page, pageSize, userId, postId, postIds));
        return R.success(data);
    }

    @GetMapping("/statistics")
    public R<Map<String, Long>> statistics(@RequestParam(name = "userId", required = false) Long userId,
        @RequestParam(name = "postIds", required = false) String postIds,
        @RequestParam(name = "departmentCode", required = false) String departmentCode,
        @RequestParam(name = "startTime", required = false) String startTime,
        @RequestParam(name = "endTime", required = false) String endTime) {
        Map<String, Long> data =
            collaborationTaskService.statistics(userId, postIds, departmentCode, startTime, endTime);
        return R.success(data);
    }

    /**
     * 查代办问题/问题详情
     *
     */
    @GetMapping("/detail")
    public R<CTaskRespVo> getCollaborationTaskDetail(@RequestParam(name = "taskId", required = false) Long taskId,
        @RequestParam(name = "userId", required = false) Long userId,
        @RequestParam(name = "postId", required = false) Long postId) {
        log.info("getCollaborationTaskDetail: {} , {}", taskId, userId);
        CTaskRespVo data = collaborationTaskService.getCollaborationTaskDetail(taskId, userId, postId);
        return R.success(ResponseCodeEnum.SUCCESS.getCode(), I18nUtil.get(ResponseCodeEnum.SUCCESS.getMsg()), data);
    }

    /**
     * 批量更新状态
     *
     */
    @PutMapping("/update")
    public R updateCollaborationTask(@RequestBody List<CollaborationTaskCO> cTaskCos) {
        log.info("updateCollaborationTask: {}", cTaskCos);
        collaborationTaskService.updateCollaborationTasks(cTaskCos);
        return R.success(ResponseCodeEnum.SUCCESS.getCode(), I18nUtil.get(ResponseCodeEnum.SUCCESS.getMsg()));
    }

    @PutMapping("/update/groupName")
    public R<Void> updateGroupNameByGroupId(@RequestParam("groupId") Long groupId,
        @RequestParam("groupName") String groupName) {
        collaborationTaskService.updateGroupNameByGroupId(groupId, groupName);
        return R.success(ResponseCodeEnum.SUCCESS.getCode(), I18nUtil.get(ResponseCodeEnum.SUCCESS.getMsg()));
    }

    /**
     * 查代办问题/问题详情
     *
     */
    @GetMapping("/detailBySeqId")
    public R<CTaskRespVo> getCollaborationTaskDetailBySeqId(
        @RequestParam(name = "icsMsgId", required = false) Long icsMsgId,
        @RequestParam(name = "userId", required = false) Long userId,
        @RequestParam(name = "postId", required = false) Long postId) {
        log.info("getCollaborationTaskDetailBySeqId: {} , {}", icsMsgId, userId);
        CTaskRespVo data = collaborationTaskService.getCollaborationTaskDetailBySeqId(icsMsgId, userId, postId);
        return R.success(ResponseCodeEnum.SUCCESS.getCode(), I18nUtil.get(ResponseCodeEnum.SUCCESS.getMsg()), data);
    }

    /**
     * 查首页问题处置数量
     *
     */
    @GetMapping("/taskNum")
    public R<Map<String, Integer>> getTaskNum(@RequestParam(name = "userId", required = false) Long userId,
        @RequestParam(name = "postId", required = false) Long postId) {
        log.info("getTaskNum userId: {}", userId);
        Map<String, Integer> data = collaborationTaskService.getTaskNum(userId, postId);
        return R.success(ResponseCodeEnum.SUCCESS.getCode(), I18nUtil.get(ResponseCodeEnum.SUCCESS.getMsg()), data);
    }

    /**
     * 查找逾期的消息列表
     */
    @GetMapping("/overdue")
    public R<Page<CollaborationOverdueVO>> getOverdueList(
        @RequestParam(required = false, value = "departmentCode") String departmentCode,
        @RequestParam(required = false, value = "startTime") String startTime,
        @RequestParam(required = false, value = "endTime") String endTime,
        @RequestParam(name = "pageSize", required = false) Integer pageSize,
        @RequestParam(name = "pageNum", required = false) Integer pageNum) {
        log.info("getOverdueList: {} , {} , {}", departmentCode, startTime, endTime);
        Page<CollaborationOverdueVO> data = collaborationTaskService.getOverdueList(departmentCode, startTime, endTime, pageSize, pageNum);

        return R.success(data);
    }
}
