package com.tdtech.cloudcmd.im.jingxin.server.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * @author cangPeng
 * @date 2025/2/26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_task_response")
public class CollaborationTaskResponse {
    public static final String ID = "id";
    public static final String TASK_ID = "task_id";
    public static final String USER_ID = "user_id";
    public static final String POST_ID = "post_id";
    public static final String USER_NAME = "user_name";
    public static final String SEQID= "seqid";
    public static final String CONTENT = "content";
    public static final String DEPARTMENT_ID = "department_id";
    public static final String DEPARTMENT_NAME = "department_name";
    public static final String USER_NICK = "user_nick";
    public static final String MSG_FILE_ID= "msg_file_id";
    public static final String IS_DELETED = "is_deleted";
    public static final String GMT_MODIFIED = "gmt_modified";
    public static final String GMT_CREATED = "gmt_created";
    private static final long serialVersionUID = 1L;
    private Long id;

    /**
     * 协同任务ID
     */
    private Long taskId;

    /**
     * 答复人ID
     */
    private Long userId;

    private Long postId;

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
