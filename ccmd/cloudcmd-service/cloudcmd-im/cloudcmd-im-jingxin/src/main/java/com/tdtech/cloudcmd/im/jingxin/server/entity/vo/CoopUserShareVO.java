package com.tdtech.cloudcmd.im.jingxin.server.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tdtech.cloudcmd.im.jingxin.server.entity.UserCoopShared;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 协同岗分享信息 VO
 */
@Data
public class CoopUserShareVO {

    /**
     * 协同岗ID
     */
    private Long coopUserId;

    /**
     * 协同岗名称
     */
    private String coopUserName;

    /**
     * 组织ID
     */
    private Long orgId;

    /**
     * 组织名称
     */
    private String orgName;

    /**
     * 来源类型：local=本节点；received=接收的
     */
    private String originType;

    /**
     * 来源节点ID（仅接收的协同岗有值）
     */
    private String originPeerId;

    /**
     * 来源节点名称（仅接收的协同岗有值）
     */
    private String originPeerName;

    /**
     * 目标组织ID
     */
    private Long targetOrgId;

    /**
     * 接收时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime receivedTime;

    /**
     * 已分享的节点列表
     */
    private List<UserCoopShared> sharedNodes;

    /**
     * 是否已分享到对端节点：0=未分享，1=已分享
     * 判定依据：tb_user_coop_shared 中存在该 coopUserId 的记录即为已分享
     */
    private Integer isShared = 0;
}
