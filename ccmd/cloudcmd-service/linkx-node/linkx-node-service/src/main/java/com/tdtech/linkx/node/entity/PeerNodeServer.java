package com.tdtech.linkx.node.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 服务端节点配置表
 */
@Data
@TableName("tb_peer_node_server")
public class PeerNodeServer {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 服务端节点标识
     */
    private String peerId;

    /**
     * 服务端节点IP
     */
    private String ip;

    /**
     * 服务端节点端口
     */
    private Integer port;

    /**
     * 服务端节点名称
     */
    private String name;

    /**
     * 服务端节点备注
     */
    private String remark;

    /**
     * 创建人ID
     */
    private Long createUserId;

    /**
     * 创建人名称
     */
    private String createUserName;

    /**
     * 服务端节点标签
     */
    private String tag;

    /**
     * 是否删除。0：未删除；1：已删除
     */
    private Integer deleted;

    /**
     * 对方是否已授权数据互通。0：未授权；1：已授权；2：拒绝
     */
    private Integer authorized;

    /**
     * 数据互通被授权描述
     */
    private String authorizedDesc;

    /**
     * 数据互通被授权时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime authorizedTime;

    /**
     * 数据互通授权有效期（对端授权本端的有效期，由对端授权回调时传入）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiredIn;

    /**
     * 删除时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deletedTime;

    /**
     * 客户端的版本号
     */
    private String version;

    /**
     * 授权回调URL
     */
    private String callbackUrl;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime gmtCreated;
}
