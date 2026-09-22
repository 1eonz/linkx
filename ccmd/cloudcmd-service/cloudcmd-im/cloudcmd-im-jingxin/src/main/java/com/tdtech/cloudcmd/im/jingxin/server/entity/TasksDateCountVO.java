package com.tdtech.cloudcmd.im.jingxin.server.entity;

import lombok.Data;

/**
 * @author ly
 * @date 2025/8/27
 **/
@Data
public class TasksDateCountVO {

    /**
     * 日期
     */
    private String date;

    /**
     * 数量
     */
    private Long count = 0L;
}
