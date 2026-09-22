package com.tdtech.cloudcmd.im.jingxin.server.service;

import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationTaskCO;

/**
 * @author: S063874
 * @date: 2026-01-15 19:14
 */
public interface SendPreWarningMessageService {


    void sendUnattendedMessage(Long postId);

    void sendTaskexpiredMessage(CollaborationTaskCO collaborationTaskCO);
}
