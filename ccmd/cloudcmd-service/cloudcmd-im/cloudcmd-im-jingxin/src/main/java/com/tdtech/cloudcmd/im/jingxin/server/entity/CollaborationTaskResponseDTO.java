package com.tdtech.cloudcmd.im.jingxin.server.entity;

import lombok.Data;

import java.util.Date;

/**
 * @author cangPeng
 * @date 2025/2/26
 */
@Data
public class CollaborationTaskResponseDTO {
    private Long id;

    /**
     * 协同任务ID
     */
    private Long taskId;

    /**
     * 答复人ID
     */
    private Long userId;

    /**
     * 答复人姓名
     */
    private String userName;

    /**
     * 答复内容
     */
    private String content;

    /**
     * 答复人部门ID
     */
    private Long departmentId;

    /**
     * 答复人部门名称
     */
    private String departmentName;

    /**
     * 答复人在群组昵称
     */
    private String userNick;

    /**
     * IM消息序列号
     */
    private Long seqid;

    /**
     * IM附件ID
     */
    private String msgFileId;

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
}
