package com.tdtech.linkx.node.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tdtech.linkx.node.enums.NodeStatusEnum;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 客户端节点VO（包含授权状态和连接状态）
 */
@Data
public class PeerNodeClientVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String peerId;

    private String ip;

    private String name;

    private String remark;

    /**
     * 授权状态。0：未授权；1：已授权；2：拒绝
     */
    private Integer grant;

    private String grantDesc;

    private Long grantUserId;

    private String grantUserName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime grantTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiredIn;

    private Boolean expired;

    private String tag;

    private String callbackUrl;

    private String version;

    private String peerNodeGatewayPrefix;

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

    private Integer status;

    private String statusDesc;

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
        }
    }

    public void setGrantInfo() {
        if (grant == null || grant == 0) {
            this.grantDesc = "未授权";
            this.expired = false;
        } else if (grant == 2) {
            this.grantDesc = "拒绝";
            this.expired = false;
        } else {
            if (expiredIn != null && expiredIn.isBefore(LocalDateTime.now())) {
                this.grantDesc = "授权已过期";
                this.expired = true;
            } else {
                this.grantDesc = "已授权";
                this.expired = false;
            }
        }
    }
}