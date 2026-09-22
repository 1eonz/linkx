package com.tdtech.cloudcmd.im.openapi.repo.notification;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@TableName("tb_notification")
public class Notification implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String appId;

    private String moduleName;

    private String content;

    private Integer collaborativeMsg;

    private String userIds;

    private String url;

    private Integer showInNotification;

    private Date gmtCreated;
}
