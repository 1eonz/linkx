package com.tdtech.cloudcmd.im.jingxin.server.entity;

import lombok.Data;

/**
 * @author ly
 * @date 2025/8/27
 **/
@Data
public class TasksCountVO {

    /**
     * 总数
     */
    private String status;

    /**
     * 数量
     */
    private Long count = 0L;

    /**
     * 占比
     */
    private Double ratio = 0d;
}
