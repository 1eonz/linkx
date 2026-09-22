package com.tdtech.cloudcmd.im.jingxin.api.entity.tasks;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @author ly
 * @date 2025/8/26 16:39
 */
@Data
@Builder
public class TasksQueryReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 搜索关键字
     */
    private String keywords;

    /**
     * 执行人
     */
    private String executor;

    /**
     * 任务开始时间
     */
    private Date startTime;

    /**
     * 任务结束时间
     */
    private Date endTime;

    /**
     * 任务状态。业务系统任务状态文字描述
     */
    private String status;

    /**
     * 业务类型
     */
    private String businessType;

    /**
     * 任务等级
     */
    private String level;

    /**
     * 是否为紧急任务。默认为0（不紧急）
     */
    private Integer urgent;

    /**
     * 当前用户身份证号
     */
    private String idCard;

    /**
     * 业务类型
     */
    private List<String> businessTypeList;

    /**
     * 任务等级
     */
    private List<String> levelList;

}