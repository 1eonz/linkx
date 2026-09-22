package com.tdtech.linkx.node.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 统一节点授权表（合并 client_grant + server_grant）
 */
@Data
@TableName("tb_peer_node_grant")
public class PeerNodeGrant {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 授权发起方节点标识
     */
    private String fromPeerId;

    /**
     * 授权发起方节点名称
     */
    private String fromPeerName;

    /**
     * 被授权方节点标识
     */
    private String toPeerId;

    /**
     * 被授权方节点名称
     */
    private String toPeerName;

    /**
     * 授权类型。1：授权；0：取消授权
     */
    private Integer grantType;

    /**
     * 授权权限项（org/dashboard/coopuser）
     */
    private String permission;

    /**
     * 授权范围JSON
     */
    private String grantData;

    /**
     * 授权过期时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiredTime;

    /**
     * 授权描述
     */
    private String grantDescription;

    /**
     * 授权人ID
     */
    private Long grantUserId;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime gmtCreated;
}
