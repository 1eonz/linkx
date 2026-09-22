package com.tdtech.cloudcmd.auth.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "组织权限迁移结果VO")
public class OrgPrivMigrateResultVO {

    @Schema(description = "总角色数")
    private Integer totalRoles;

    @Schema(description = "成功迁移的角色数")
    private Integer migratedRoles;

    @Schema(description = "失败的角色数")
    private Integer failedRoles;

    @Schema(description = "总关联关系数")
    private Integer totalRelations;

    @Schema(description = "耗时（格式：HH:mm:ss）")
    private String duration;

    @Schema(description = "失败详情列表")
    private List<OrgPrivMigrateFailedVO> failedList;
}