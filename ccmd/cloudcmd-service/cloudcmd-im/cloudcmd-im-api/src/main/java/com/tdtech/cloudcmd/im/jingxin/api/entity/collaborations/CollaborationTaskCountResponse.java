package com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations;

import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author ChinasoftPortal
 * @date 2025/8/18 @Describe：
 */
@Data
public class CollaborationTaskCountResponse implements Serializable {

    @Schema(description = "协同岗id")
    private Long postId;

    @Schema(description = "数量")
    private Long cnt;
}
