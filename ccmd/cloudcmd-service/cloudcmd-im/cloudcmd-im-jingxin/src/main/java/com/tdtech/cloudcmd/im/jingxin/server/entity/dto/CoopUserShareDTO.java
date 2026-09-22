package com.tdtech.cloudcmd.im.jingxin.server.entity.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 协同岗分享请求 DTO
 */
@Data
public class CoopUserShareDTO {

    /**
     * 目标节点ID
     */
    @NotBlank(message = "peerId不能为空")
    private String peerId;

    /**
     * 目标组织ID（不传表示对端全部组织可见）
     */
    private Long orgId;

    /**
     * 目标组织名称（与 orgId 配合使用）
     */
    private String orgName;
}
