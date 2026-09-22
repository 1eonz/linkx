package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationTask;
import com.tdtech.cloudcmd.im.jingxin.server.entity.RelyAllTaskResponseCO;
import com.tdtech.cloudcmd.util.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.bean.PageResult;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationTaskResponse;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationTaskResponseCO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationTaskResponseDTO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollabsTaskResponseListReqCO;
import com.tdtech.cloudcmd.im.jingxin.server.service.ICollaborationTaskResponseService;
import com.tdtech.cloudcmd.im.jingxin.server.service.ICollaborationTaskService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.CollaborationTaskResponseMapper;
import com.tdtech.cloudcmd.util.BeanCopyUtils;

/**
 * @author cangPeng
 * @date 2025/2/26
 */
@Slf4j
@Service
public class CollaborationTaskResponseServiceImpl extends ServiceImpl<CollaborationTaskResponseMapper, CollaborationTaskResponse> implements ICollaborationTaskResponseService {
    @Resource
    private CollaborationTaskResponseMapper collaborationTaskResponseMapper;
    @Resource
    private ICollaborationTaskService iCollaborationTaskService;

    @Override
    public void save(CollaborationTaskResponseCO collaborationTaskResponseCO) {
        try {
            CollaborationTaskResponse collaborationTaskResponse =
                BeanCopyUtils.copyBean(collaborationTaskResponseCO, new CollaborationTaskResponse());
            collaborationTaskResponseMapper.insert(collaborationTaskResponse);
        } catch (DuplicateKeyException e) {
            log.warn("duplicated insert:{} {}",e.getMessage(),collaborationTaskResponseCO);
        }
    }

    @Override
    public PageResult<CollaborationTaskResponseDTO> listTaskResponse(CollabsTaskResponseListReqCO collabsTaskResponseListReqCO) {
        var page = new Page<CollaborationTaskResponse>(collabsTaskResponseListReqCO.getPage(), collabsTaskResponseListReqCO.getPageSize());
        Page<CollaborationTaskResponse> collaborationTaskResponsePage = collaborationTaskResponseMapper.listPage(page,collabsTaskResponseListReqCO);
        var pageResult = new PageResult<CollaborationTaskResponseDTO>();
        pageResult.setPages(collaborationTaskResponsePage.getPages());
        pageResult.setSize(collaborationTaskResponsePage.getSize());
        pageResult.setTotal(collaborationTaskResponsePage.getTotal());
        pageResult.setCurrent(collaborationTaskResponsePage.getCurrent());
        pageResult.setRecords(Optional.ofNullable(collaborationTaskResponsePage.getRecords()).stream().flatMap(Collection::stream)
                .map(this::toDTO).collect(Collectors.toList()));
        return pageResult;
    }

    private CollaborationTaskResponseDTO toDTO(CollaborationTaskResponse collaborationTaskResponse) {
        // copy
        var collaborationDTO = BeanCopyUtils.copyBean(collaborationTaskResponse, CollaborationTaskResponseDTO::new);

        return collaborationDTO;
    }

    @Override
    public void createTaskResponse(List<CollaborationTaskResponseCO> imTaskResponses) {
        for (CollaborationTaskResponseCO imTaskResponse : imTaskResponses){
            CollaborationTaskResponse taskResponse = BeanCopyUtils.copyBean(imTaskResponse, CollaborationTaskResponse::new);
            //保存回复消息
            try{
                collaborationTaskResponseMapper.insert(taskResponse);
            } catch (DuplicateKeyException e) {
                log.warn("duplicated insert:{} {}", e.getMessage(), taskResponse);
            }
            //更新任务状态，如果是待办则变为跟踪，其他的状态则不变
            Integer status = iCollaborationTaskService.getById(imTaskResponse.getTaskId()).getStatus();
            if (status == 1 || status == 7 || status == 8){
                status = 2;
            }
            iCollaborationTaskService.updateCollaborationTask(imTaskResponse.getTaskId(),imTaskResponse.getFromExecutorId() , status, 1);
        }

    }

    @Override
    public void replyAll(RelyAllTaskResponseCO relyAllTaskResponseCO) {
        Long groupId = relyAllTaskResponseCO.getGroupId();
        Long postId = relyAllTaskResponseCO.getPostId();
        List<Integer> statusList = Arrays.asList(1, 2, 7, 8);
        List<CollaborationTask> list = iCollaborationTaskService.findList(groupId, postId, statusList);
        if (CollectionUtils.isEmpty(list)) {
            // 这个协同岗没有需要回复的任务，则不做任何处理
            return;
        }
        // 不引用消息回复任务的时候，一键回复改协同所有任务
        list.forEach(task -> {
            CollaborationTaskResponse taskResponse = BeanCopyUtils.copyBean(relyAllTaskResponseCO, CollaborationTaskResponse::new);
            taskResponse.setTaskId(task.getId());
            //保存回复消息
            try{
                collaborationTaskResponseMapper.insert(taskResponse);
            } catch (DuplicateKeyException e) {
                log.warn("duplicated insert:{} {}", e.getMessage(), taskResponse);
            }
            iCollaborationTaskService.updateCollaborationTask(task.getId(), relyAllTaskResponseCO.getFromExecutorId() , 2, 1);
        });
    }
}
