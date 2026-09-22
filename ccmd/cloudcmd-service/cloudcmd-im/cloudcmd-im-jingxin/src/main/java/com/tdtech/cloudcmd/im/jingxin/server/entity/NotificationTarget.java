package com.tdtech.cloudcmd.im.jingxin.server.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@TableName("linkx_open.tb_notification_targets")
public class NotificationTarget implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long notificationId;

    private Long userId;

    @TableField("`read`")
    private Integer read;

    private Date readTime;

    private Date gmtCreated;
}
