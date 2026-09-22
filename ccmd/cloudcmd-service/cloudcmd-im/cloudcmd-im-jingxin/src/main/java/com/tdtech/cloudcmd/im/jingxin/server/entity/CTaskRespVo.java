package com.tdtech.cloudcmd.im.jingxin.server.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @author Zht
 */
@Data
public class CTaskRespVo implements Serializable {

    /**
     * IM消息发送时间
     */
    private Date msgSentTime;

    /**
     * 群组名称
     */
    private String groupName;

    /**
     * IM发送人所属IM部门名称
     */
    private String fromUserDepartmentName;

    /**
     * IM消息发送人ID
     */
    private Long fromUserId;

    /**
     * IM发送人姓名
     */
    private String fromUserName;

    /**
     * IM发送人所在群组的昵称
     */
    private String fromUserNick;

    /**
     * 聊天消息内容
     */
    private String text;

    /**
     * 协同任务状态：1:待办；2:跟踪；3:已办结；4:无需回复；5:全部问题；6:全部回复
     */
    private Integer status;

    /**
     * 协同任务ID
     */
    private Long taskId;

    /**
     * 任务对应消息id
     */
    private Long icsMsgId;

    /**
     * 回复内容列表
     */
    private List<CollaborationTaskResponse> responses;

    /**
     * 是否回复
     */
    private Integer reply;

    /**
     * 协同任务是否归档
     */
    private Integer archive;
}
