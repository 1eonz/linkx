package com.tdtech.cloudcmd.linkx.third.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;

/**
 * 推送消息表
 */
@Data
@TableName(value = "linkx_third.tb_push_msg")
public class PushMsg implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    @NotNull(message = "ID不能为null")
    private Long id;

    /**
     * 任务通知消息的id
     */
    @TableField(value = "notification_id")
    private Long notificationId;

    /**
     * 收到的用户ID
     */
    @TableField(value = "received_user_id")
    @NotNull(message = "收到的用户ID不能为null")
    private Long receivedUserId;

    /**
     * 通知URL
     */
    @TableField(value = "url")
    @Size(max = 255, message = "通知URL最大长度要小于 255")
    private String url;

    /**
     * 通知文本内容
     */
    @TableField(value = "`text`")
    @Size(max = 1000, message = "通知文本内容最大长度要小于 1000")
    @NotBlank(message = "通知文本内容不能为空")
    private String text;

    /**
     * 联系人ID
     */
    @TableField(value = "contact_id")
    private Long contactId;

    /**
     * 收到的消息
     */
    @TableField(value = "`data`")
    private String data;

    /**
     * 消息收到的时间
     */
    @TableField(value = "received_at")
    @NotNull(message = "消息收到的时间不能为null")
    private Date receivedAt;

    /**
     * 最后修改时间
     */
    @TableField(value = "gmt_last_modified", fill = FieldFill.INSERT_UPDATE)
    @NotNull(message = "最后修改时间不能为null")
    private Date gmtLastModified;

    /**
     * 创建时间
     */
    @TableField(value = "gmt_create_time", fill = FieldFill.INSERT)
    @NotNull(message = "创建时间不能为null")
    private Date gmtCreateTime;
}