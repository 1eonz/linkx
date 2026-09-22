package com.tdtech.cloudcmd.im.jingxin.server.entity;

import lombok.Data;

/**
 * @author ly
 * @date 2025/8/27
 **/
@Data
public class TasksStatusStatisticsVO {

    /**
     * 业务状态
     */
    private String businessStatus;

    /**
     * 占比
     */
    private Double ratio = 0d;
}
