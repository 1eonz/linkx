package com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations;

import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author lsc
 * @date 2025/7/18
 **/
@Data
public class CollaborationDispositionVO implements Serializable {

    private Long postId;
    /**
     * 协同岗名称
     */
    @Schema(description = "协同岗名称")
    private String postName;
    /**
     * 部门名称
     */
    @Schema(description = "部门名称")
    private String orgName;
    /**
     * 回复数量
     */
    @Schema(description = "回复数量")
    private Integer replyCount;
    /**
     * 回复时长
     */
    @Schema(description = "回复时长")
    private Double avgReplyDuration;

    /**
     * 图标URL
     */
    @Schema(description = "图标URL")
    private String iconUrl;
}
