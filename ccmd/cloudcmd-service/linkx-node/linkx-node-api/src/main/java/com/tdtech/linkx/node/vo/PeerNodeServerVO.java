package com.tdtech.linkx.node.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tdtech.linkx.node.enums.NodeStatusEnum;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 服务端节点VO（包含连接状态）
 */
@Data
public class PeerNodeServerVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String peerId;

    private String ip;

    private String name;

    private String remark;

    private Long createUserId;

    private String createUserName;

    private String tag;

    /**
     * 对方是否已授权数据互通。0：未授权；1：已授权；2：拒绝
     */
    private Integer authorized;

    private String authorizedDesc;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime authorizedTime;

    /**
     * 数据互通授权有效期（对端授权本端的有效期）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiredIn;

    /**
     * 组织部门数据授权：1=授权，0=不授权
     */
    private Integer org;

    /**
     * 看板数据授权：1=授权，0=不授权
     */
    private Integer dashboard;

    /**
     * 协同用户数据授权：1=授权，0=不授权
     */
    private Integer coopUser;

    /**
     * H5数据授权：1=授权，0=不授权
     */
    private Integer h5;

    private String version;

    private String peerNodeGatewayPrefix;

    private Integer status;

    private String statusDesc;

    private Integer retryStrategy;

    private String lastAuthFailReason;

    private String session;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastSeen;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime gmtCreated;

    public void setStatusWithDesc(Integer status) {
        this.status = status;
        if (status != null) {
            NodeStatusEnum statusEnum = NodeStatusEnum.fromCode(status);
            this.statusDesc = statusEnum != null ? statusEnum.getDesc() : "未知";
        } else {
            this.statusDesc = "未连接";
        }
    }
}