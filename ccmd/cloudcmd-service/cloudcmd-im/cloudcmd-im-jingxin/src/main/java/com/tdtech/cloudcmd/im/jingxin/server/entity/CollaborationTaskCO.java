package com.tdtech.cloudcmd.im.jingxin.server.entity;

import lombok.Data;

import java.util.Date;

/**
 * @author cangPeng
 * @date 2025/2/26
 */
@Data
public class CollaborationTaskCO {
    private Long id;
    /**
     * IM协同岗用户ID
     */
    private Long userId;

    /**
     * IM消息来源的
     */
    private Long groupId;

    /**
     * 群组名称
     */
    private String groupName;

    /**
     * IM消息序列号
     */
    private Long seqid;

    /**
     * IM消息内容
     */
    private String text;

    /**
     * IM消息发送人ID
     */
    private Long fromUserId;

    /**
     * IM消息发送人名称
     */
    private String fromUserName;

    /**
     * IM消息发送人昵称
     */
    private String fromUserNick;

    /**
     * IM消息发送人所属部门ID
     */
    private Long fromUserDepartmentId;

    /**
     * IM消息发送人所属部门名称
     */
    private String fromUserDepartmentName;

    /**
     * IM消息发送时间
     */
    private Date msgSentTime;

    /**
     * IM附件ID
     */
    private String msgFileId;

    /**
     * 状态。-1:待分配；1:待办；2:跟踪；3:已办结；4:无需回复
     */
    private Integer status;

    /**
     * 是否删除
     */
    private Integer isDeleted;

    /**
     * 最后修改时间
     */
    private Date gmtModified;

    /**
     * 创建时间
     */
    private Date gmtCreated;

    /**
     * 任务对应消息id
     */
    private Long icsMsgId;

    /**
     * 发送执行者
     */
    private Long fromExecutorId;

    /**
     * 接收执行者
     */
    private Long toExecutorId;

    /**
     * 是否回复
     */
    private Integer reply;

    private Long postId;
}
