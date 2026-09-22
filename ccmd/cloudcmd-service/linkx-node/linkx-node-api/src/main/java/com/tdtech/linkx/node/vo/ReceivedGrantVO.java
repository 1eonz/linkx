package com.tdtech.linkx.node.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 接收到的授权信息 VO
 * 展示"谁授权给我"的授权数据
 */
@Data
@Schema(description = "接收到的授权信息")
public class ReceivedGrantVO {

    @Schema(description = "授权记录ID")
    private Long id;

    @Schema(description = "授权发起方节点标识")
    private String fromPeerId;

    @Schema(description = "授权发起方节点名称")
    private String fromPeerName;

    @Schema(description = "被授权方节点标识（本端）")
    private String toPeerId;

    @Schema(description = "组织部门数据授权：1=授权，0=不授权")
    private Integer org;

    @Schema(description = "看板数据授权：1=授权，0=不授权")
    private Integer dashboard;

    @Schema(description = "协同用户数据授权：1=授权，0=不授权")
    private Integer coopUser;

    @Schema(description = "授权描述")
    private String grantDescription;

    @Schema(description = "授权过期时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiredTime;

    @Schema(description = "授权时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime gmtCreated;
}
