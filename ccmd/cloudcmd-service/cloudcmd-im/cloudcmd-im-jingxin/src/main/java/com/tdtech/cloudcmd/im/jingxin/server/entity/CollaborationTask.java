package com.tdtech.cloudcmd.im.jingxin.server.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * @author cangPeng
 * @date 2025/2/26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_task")
public class CollaborationTask implements Serializable {
    public static final String ID = "id";
    public static final String ICS_MSG_ID = "ics_msg_id";
    public static final String USER_ID = "user_id";
    public static final String POST_ID = "post_id";
    public static final String GROUP_ID = "group_id";
    public static final String GROUP_NAME = "group_name";
    public static final String SEQID= "seqid";
    public static final String TEXT = "text";
    public static final String FROM_USER_ID = "from_user_id";
    public static final String FROM_USER_NAME = "from_user_name";
    public static final String FROM_USER_NICK = "from_user_nick";
    public static final String FROM_USER_DEPARTMENT_ID = "from_user_department_id";
    public static final String FROM_USER_DEPARTMENT_NAME = "from_user_department_name";
    public static final String MSG_SENT_TIME = "msg_sent_time";
    public static final String MSG_FILE_ID= "msg_file_id";
    public static final String STATUS = "status";
    public static final String IS_DELETED = "is_deleted";
    public static final String GMT_MODIFIED = "gmt_modified";
    public static final String GMT_CREATED = "gmt_created";
    public static final String REPLY = "reply";
    private static final long serialVersionUID = 1L;
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
     *状态，1:待办；2:跟踪；3:已办结；4:无需回复,5是统计全部问题，6是统计全部回复问题，7未及时回复，8已逾期
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
     * 任务对应消息ID
     */
    private Long icsMsgId;

    /**
     * 是否回复
     */
    private Integer reply;

    /**
     * 协同岗ID
     */
    private Long postId;
}
