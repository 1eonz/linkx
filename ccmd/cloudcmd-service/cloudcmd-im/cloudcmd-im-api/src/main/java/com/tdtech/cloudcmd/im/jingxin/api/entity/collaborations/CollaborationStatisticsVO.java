package com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations;

import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author lsc
 * @date 2025/7/18
 **/
@Data
public class CollaborationStatisticsVO implements Serializable {

    /**
     * 协同岗总数
     */
    @Schema(description = "协同岗总数")
    private Integer total = 0;

    /**
     * 关联人员总数
     */
    @Schema(description = "关联人员总数")
    private Integer userTotal = 0;

    /**
     * 关联人员在线总数
     */
    @Schema(description = "关联人员在线总数")
    private Integer userOnlineTotal = 0;

    /**
     * 关联人员在线比例
     */
    @Schema(description = "关联人员在线比例")
    private Double userOnlineRatio = 0d;

    /**
     * 0人员在线协同岗个数
     */
    @Schema(description = "0人员在线协同岗个数")
    private Integer zeroUserOnlineTotal = 0;
}
