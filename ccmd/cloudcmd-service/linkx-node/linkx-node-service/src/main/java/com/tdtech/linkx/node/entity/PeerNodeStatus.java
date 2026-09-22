package com.tdtech.linkx.node.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 统一节点状态表（合并 client_status + server_status）
 */
@Data
@TableName("tb_peer_node_status")
public class PeerNodeStatus {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * P2P节点标识
     */
    private String peerId;

    /**
     * 节点类型。1：server（连入本机的客户端）；2：client（本机连出的服务端）
     */
    private Integer peerType;

    /**
     * 节点状态。0：未连接；1：连接中；2：已连接；3：认证中；4：已认证；5：认证失败；6：重连中
     */
    private Integer status;

    /**
     * 重试策略。0：正常；1：降低频率；2：不重试（仅server类型使用）
     */
    private Integer retryStrategy;

    /**
     * 最近一次认证失败原因（仅server类型使用）
     */
    private String lastAuthFailReason;

    /**
     * 当前使用的JWT token
     */
    private String jwt;

    /**
     * websocket连接会话ID
     */
    private String session;

    /**
     * 最后活跃时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastSeen;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime gmtCreated;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime gmtModified;
}
