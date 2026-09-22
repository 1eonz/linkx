package com.tdtech.cloudcmd.im.jingxin.server.entity;

import lombok.Data;

/**
 * @author ly
 * @date 2025/8/27
 **/
@Data
public class TasksToDoCountVO {

    /**
     * 身份证号
     */
    private String idCard;

    /**
     * 代办的数量
     */
    private Long count = 0L;
}
