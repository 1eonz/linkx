package com.tdtech.cloudcmd.im.jingxin.server.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.cloudcmd.bean.PageResult;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationTaskResponse;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationTaskResponseCO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationTaskResponseDTO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollabsTaskResponseListReqCO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.RelyAllTaskResponseCO;

/**
 * @author cangPeng
 * @date 2025/2/26
 */
public interface ICollaborationTaskResponseService extends IService<CollaborationTaskResponse> {
    void save(CollaborationTaskResponseCO collaborationTaskResponseCO);

    PageResult<CollaborationTaskResponseDTO> listTaskResponse(CollabsTaskResponseListReqCO collabsTaskResponseListReqCO);

    void createTaskResponse(List<CollaborationTaskResponseCO> imTaskResponses);

    void replyAll(RelyAllTaskResponseCO relyAllTaskResponseCO);
}
