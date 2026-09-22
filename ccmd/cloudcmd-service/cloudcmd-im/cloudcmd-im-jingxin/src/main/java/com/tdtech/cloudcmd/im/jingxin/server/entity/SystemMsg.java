package com.tdtech.cloudcmd.im.jingxin.server.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_system_msg")
public class SystemMsg implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /**
     * 发送人ID
     */
    private Long fromUserId;

    /**
     * 被通知的用户ID
     */
    private Long userId;

    /**
     * 被通知的用户名
     */
    private String userName;

    /**
     * 被通知的用户所属部门ID
     */
    private Long deptId;

    /**
     * 被通知的用户所属部门名称
     */
    private String deptName;

    /**
     * 被通知消息类型，和警信的消息类型保持一致。例如：TEXT_MSG、MEDIA_MSG等
     */
    private String msgType;

    /**
     * 通知原因：1值班临期 2权限审批
     * @see com.tdtech.cloudcmd.im.jingxin.server.enums.NotifyTypeEnum
     */
    private Integer notifyType;

    /**
     * 业务ID
     */
    private Long bizId;

    /**
     * 被通知内容
     */
    private String content;

    /**
     * 被通知结果
     */
    private String result;

    /**
     * 通知时间
     */
    private Date gmtCreated;
}
