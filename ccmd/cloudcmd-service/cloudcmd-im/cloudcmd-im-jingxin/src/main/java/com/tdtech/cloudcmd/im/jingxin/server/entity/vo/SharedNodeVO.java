package com.tdtech.cloudcmd.im.jingxin.server.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 协同岗已分享节点 VO
 */
@Data
public class SharedNodeVO {

    /**
     * 分享记录ID
     */
    private Long id;

    /**
     * 被授权的节点ID
     */
    private String peerId;

    /**
     * 被授权的节点名称（无名称时展示IP）
     */
    private String peerName;

    /**
     * 被授权的节点IP
     */
    private String peerIp;

    /**
     * 协同岗原始归属节点ID（本节点的为 null）
     */
    private String coopUserOrigin;

    /**
     * 被授权的目标组织ID（null 表示全局可见）
     */
    private Long targetOrgId;

    /**
     * 被授权的目标组织名称
     */
    private String targetOrgName;

    /**
     * 分享人ID
     */
    private Long sharedByUserId;

    /**
     * 分享人名称
     */
    private String sharedByUserName;

    /**
     * 分享时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime gmtCreated;
}
