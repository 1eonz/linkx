package com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations;

import lombok.Data;

import java.io.Serializable;

/**
 * @author lsc
 * @date 2025/7/14
 **/
@Data
public class CollaborationPostOnlineVO implements Serializable {

    /**
     * 岗位名称
     */
    private String postName;

    /**
     * 在线人数
     */
    private Integer onlineCount = 0;

    /**
     * 总人数
     */
    private Integer totalCount = 0;

    /**
     * 在线人员
     */
    private String onlineUsers = "";

    /**
     * 支撑群数
     */
    private Integer groupCount = 0;

    /**
     * 图标URL
     */
    private String iconUrl;
}
