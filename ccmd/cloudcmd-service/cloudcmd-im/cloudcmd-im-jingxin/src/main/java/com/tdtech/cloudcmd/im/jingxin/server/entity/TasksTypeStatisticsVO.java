package com.tdtech.cloudcmd.im.jingxin.server.entity;

import lombok.Data;

/**
 * @author ly
 * @date 2025/8/27
 **/
@Data
public class TasksTypeStatisticsVO {

    /**
     * 业务类型
     */
    private String businessType;

    /**
     * 数量
     */
    private Long count;

    /**
     * 占比
     */
    private Double ratio = 0d;

}
