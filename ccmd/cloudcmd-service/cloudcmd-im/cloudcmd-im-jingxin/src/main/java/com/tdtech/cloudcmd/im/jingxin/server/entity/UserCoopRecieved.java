package com.tdtech.cloudcmd.im.jingxin.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 协同岗接收信息表
 * 记录本节点从其他节点接收到的协同岗分享信息
 */
@Data
@TableName("tb_user_coop_recieved")
public class UserCoopRecieved {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 协同岗ID
     */
    private Long coopUserId;

    /**
     * 协同岗默认归属的节点标识
     */
    private String originPeerId;

    /**
     * 协同岗默认归属的节点名称
     */
    private String originPeerName;

    /**
     * 被授权的目标组织ID（警信规则：默认被授权的节点子部门都可以看见）
     */
    private Long targetOrgId;

    /**
     * 协同岗名称（分享时同步，便于建群列表展示）
     */
    private String coopUserName;

    /**
     * 图标相对路径（分享时同步，建群列表展示时拼接完整URL）
     */
    private String iconUrl;

    /**
     * 协同岗所属组织ID（分享时同步）
     */
    private Long orgId;

    /**
     * 协同岗所属组织名称（分享时同步）
     */
    private String orgName;

    /**
     * 协同岗接收的时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime receivedTime;

    /**
     * 是否有效。1：有效；0：无效
     */
    private Integer status;

    /**
     * 取消时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime cancelTime;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime gmtCreated;
}
