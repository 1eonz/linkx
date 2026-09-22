package com.tdtech.cloudcmd.im.jingxin.server.service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.cloudcmd.bean.PageResult;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.CollaborationOverdueVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CTaskRespVo;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationTask;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationTaskCO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollabsTaskListReqCO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.dto.CollaborationTaskDelayDto;

/**
 * @author cangPeng
 * @date 2025/2/26
 */
public interface ICollaborationTaskService extends IService<CollaborationTask> {
    void save(CollaborationTaskCO collaborationTaskCO);

    PageResult<CollaborationTask> listTask(CollabsTaskListReqCO collabsTaskListReqCO);

    Map<String, Long> statistics(Long userId, String postIds, String departmentCode, String startTime, String endTime);

    CTaskRespVo getCollaborationTaskDetail(Long taskId, Long userId, Long postId);

    void updateCollaborationTask(Long taskId, Long userId, Integer status, Integer reply);

    void updateCollaborationTasks(List<CollaborationTaskCO> cTaskCos);

    CTaskRespVo getCollaborationTaskDetailBySeqId(Long seqId, Long userId, Long postId);

    Map<String, Integer> getTaskNum(Long userId, Long postId);

    void updateGroupNameByGroupId(Long groupId, String groupName);

    Page<CollaborationOverdueVO> getOverdueList(String departmentCode, String startTime, String endTime, Integer pageSize, Integer pageNum);

    List<CollaborationTask> findList(List<Integer> statusList, List<Long> postIdList, String startTime, String endTime);

    List<CollaborationTask> findList(Long groupId, Long postId, List<Integer> statusList);


    /**
     * 处理未分配的任务
     *
     * @param postId 协同岗id
     * @param userId 用户id
     * @return 需要后续处理的列表
     */
    List<CollaborationTaskDelayDto> dealUnassignTask(Long postId, Long userId);

    /**
     * 任务放到延迟队列
     *
     * @param delayDtos 需要放到延迟队列的数据
     */
    void pushToDealyQueue(List<CollaborationTaskDelayDto> delayDtos);
}
