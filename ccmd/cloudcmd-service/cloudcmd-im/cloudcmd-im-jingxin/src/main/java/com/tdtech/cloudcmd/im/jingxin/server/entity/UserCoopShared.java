package com.tdtech.cloudcmd.im.jingxin.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 协同岗分享授权信息表
 * 记录本节点主动分享出去的协同岗授权信息
 */
@Data
@TableName("tb_user_coop_shared")
public class UserCoopShared {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 协同岗ID
     */
    private Long coopUserId;

    /**
     * 协同岗默认归属peer_id，当前节点则为空
     */
    private String coopUserOrigin;

    /**
     * 被授权的节点ID
     */
    private String peerId;

    /**
     * 被授权的目标组织ID（NULL表示全局可见）
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
     * 分享的时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime gmtCreated;
}
