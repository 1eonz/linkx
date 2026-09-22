package com.tdtech.cloudcmd.im.jingxin.server.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 建群候选协同岗 VO
 * 用于建群时展示协同岗列表，含头像和所属组织信息
 */
@Data
@Schema(description = "建群候选协同岗")
public class CoopUserCandidateVO {

    /**
     * 协同岗ID
     */
    @Schema(description = "协同岗ID")
    private Long coopUserId;

    /**
     * 协同岗名称
     */
    @Schema(description = "协同岗名称")
    private String coopUserName;

    /**
     * 图标完整URL（本节点拼接本端地址，接收的协同岗拼接来源节点地址）
     */
    @Schema(description = "图标完整URL")
    private String iconUrl;

    /**
     * 所属组织ID
     */
    @Schema(description = "所属组织ID")
    private Long orgId;

    /**
     * 所属组织名称
     */
    @Schema(description = "所属组织名称")
    private String orgName;

    /**
     * 来源节点ID（本节点协同岗为 null）
     */
    @Schema(description = "来源节点ID（本节点协同岗为 null）")
    private String originPeerId;

    /**
     * 来源节点名称（本节点协同岗为 null）
     */
    @Schema(description = "来源节点名称（本节点协同岗为 null）")
    private String originPeerName;
}
