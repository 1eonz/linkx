package com.tdtech.cloudcmd.im.jingxin.server.entity;

import lombok.Data;

/**
 * @author ly
 * @date 2025/8/27
 **/
@Data
public class MyTasksCountVO {

    /**
     * 业务类型
     */
    private String businessType;

    /**
     * 我创建的数量
     */
    private Long count = 0L;

    /**
     * 平均任务数
     */
    private Long avgCount = 0L;

    /**
     * 同比增长率
     */
    private Double ratio = 0d;
}
