package com.tdtech.cloudcmd.admin.resource.entity.dto;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

/**
 * 用户日志
 */
@Data
public class UserLogDto implements Serializable {

    /**
     * 主键ID
     */

    private Long id;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 操作详情
     */
    private String content;

    /**
     * 创建时间
     */
    private Date time;

    /**
     * 操作IP
     */
    private String ip;
}
