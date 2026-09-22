package com.tdtech.cloudcmd.im.jingxin.server.entity;

import lombok.Data;

/**
 * @author lsc
 * @date 2025/8/19
 **/
@Data
public class CollaborationTaskStatisticsVO {

    /**
     * 未处理任务数量
     */
    private Long unprocessedTaskCount = 0L;

    /**
     * 未及时回复任务数量
     */
    private Long untimelyRepliedTaskCount = 0L;

    /**
     * 逾期任务数量
     */
    private Long overdueTaskCount = 0L;

    /**
     * 跟踪任务数量
     */
    private Long trackingTaskCount = 0L;

    /**
     * 待办任务数量
     */
    private Long pendingTaskCount = 0L;

    /**
     * 办结的任务数量
     */
    private Long completedTaskCount = 0L;

    /**
     * 忽略的任务数量
     */
    private Long ignoredTaskCount = 0L;

    /**
     * 已办任务数量
     */
    private Long processedTaskCount = 0L;
}
