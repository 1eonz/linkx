package com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations;

import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author lsc
 * @date 2025/7/18
 **/
@Data
public class CollaborationReplyDurationVO implements Serializable {

    /**
     * 部门编码
     */
    @Schema(description = "部门编码")
    private String departmentCode;
    /**
     * 部门名称
     */
    @Schema(description = "部门名称")
    private String departmentName;
    /**
     * 平均回复时长
     */
    @Schema(description = "平均回复时长")
    private Double avgReplyDuration;
}
