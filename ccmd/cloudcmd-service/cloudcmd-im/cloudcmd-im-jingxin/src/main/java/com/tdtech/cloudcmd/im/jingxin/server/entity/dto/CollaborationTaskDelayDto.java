package com.tdtech.cloudcmd.im.jingxin.server.entity.dto;

import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationTask;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationTaskCO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;


/**
 * 新增任务后发生延迟消息处理的dto
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CollaborationTaskDelayDto {
    /**
     * task-co
     */
    private CollaborationTaskCO co;

    /**
     * task
     */
    private CollaborationTask task;
}
