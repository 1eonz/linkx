package com.tdtech.linkx.node.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客户端节点信息表
 */
@Data
@TableName("tb_peer_node_client")
public class PeerNodeClient {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 客户端节点标识
     */
    private String peerId;

    /**
     * 客户端节点IP
     */
    private String ip;

    /**
     * 客户端节点端口
     */
    private Integer port;

    /**
     * 客户端节点名称
     */
    private String name;

    /**
     * 客户端备注名称
     */
    private String remark;

    /**
     * 客户端节点的授权状态。0：未授权；1：已授权；2：拒绝
     */
    @TableField("`grant`")
    private Integer grant;

    /**
     * 授权人ID
     */
    private Long grantUserId;

    /**
     * 授权人名称
     */
    private String grantUserName;

    /**
     * 授权操作时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime grantTime;

    /**
     * 被授权的有效时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiredIn;

    /**
     * 节点标签
     */
    private String tag;

    /**
     * 数据互通授权描述
     */
    private String grantDesc;

    /**
     * 数据互通授权回调URL
     */
    private String callbackUrl;

    /**
     * 是否删除。0：未删除；1：已删除
     */
    private Integer deleted;

    /**
     * 删除时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deletedTime;

    /**
     * 服务器端的版本号
     */
    private String version;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime gmtCreated;
}
