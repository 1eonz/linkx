package com.tdtech.linkx.node.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 开放数据统计 VO
 * 聚合对端节点的 org 和 coopUser 统计数据
 */
@Data
@Schema(description = "开放数据统计")
public class OpenDataStatisticVO {

    /**
     * 组织部门统计数据
     */
    @Schema(description = "组织部门统计")
    private OrgStatistic org;

    /**
     * 协同岗统计数据
     */
    @Schema(description = "协同岗统计")
    private CoopUserStatistic coopUser;

    /**
     * 组织部门统计
     */
    @Data
    @Schema(description = "组织部门统计")
    public static class OrgStatistic {

        @Schema(description = "组织部门总数")
        private Integer total;
    }

    /**
     * 协同岗统计
     */
    @Data
    @Schema(description = "协同岗统计")
    public static class CoopUserStatistic {

        @Schema(description = "协同岗总数")
        private Integer total;

        @Schema(description = "关联人员总数")
        private Integer userTotal;

        @Schema(description = "关联人员在线总数")
        private Integer userOnlineTotal;
    }
}
