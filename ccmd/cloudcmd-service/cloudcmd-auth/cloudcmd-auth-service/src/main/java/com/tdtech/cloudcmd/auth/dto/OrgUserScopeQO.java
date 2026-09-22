package com.tdtech.cloudcmd.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 在管理员数据权限范围内搜索人员的查询对象。
 *
 * @author system
 * @since 2024-01-01
 */
@Data
@Schema(description = "管理员权限范围内人员搜索对象")
public class OrgUserScopeQO {

    @Schema(description = "组织ID，指定在该组织（含子部门）范围内搜索；为空则不限组织，仅在管理员授权范围内搜索")
    private Long orgId;

    @Schema(description = "是否包含子部门：0-仅本部门直属人员，1-包含子孙部门人员（默认0）")
    private Integer isChildren = 0;

    @Schema(description = "人员姓名，模糊查询")
    private String name;

    @Schema(description = "协同岗类型，用于标注绑定关系：0-普通协同岗，1-人员核查协同岗；为空时查询所有类型协同岗的绑定关系")
    private Integer type;
}