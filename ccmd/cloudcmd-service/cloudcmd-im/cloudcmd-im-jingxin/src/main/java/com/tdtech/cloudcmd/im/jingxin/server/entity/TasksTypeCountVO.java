package com.tdtech.cloudcmd.im.jingxin.server.entity;

import lombok.Data;

/**
 * @author ly
 * @date 2025/8/27
 **/
@Data
public class TasksTypeCountVO {

    /**
     * 业务类型
     */
    private String type;

    /**
     * 数量
     */
    private Long count = 0L;
}
