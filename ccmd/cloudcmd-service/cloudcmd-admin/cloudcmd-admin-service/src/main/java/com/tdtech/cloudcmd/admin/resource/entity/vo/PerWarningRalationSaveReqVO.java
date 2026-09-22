package com.tdtech.cloudcmd.admin.resource.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * @author: S063874
 * @date: 2026-01-13 14:49
 */
@Data
public class PerWarningRalationSaveReqVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "32745")
    private Long id;

    @Schema(description = "预警类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer classify;

    @Schema(description = "警信组织部门id或协调岗id", requiredMode = Schema.RequiredMode.REQUIRED, example = "12345")
    private Long businessId;

    @Schema(description = "预警通知的目标", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer targetType;

    @Schema(description = "需要被通知的用户id或群组id", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "12345")
    private Long targetId;

    @Schema(description = "需要被通知的用户id或群组id,多条用分号分隔", requiredMode = Schema.RequiredMode.REQUIRED, example = "12345,12345")
    private String targetIds;

    @Schema(description = "警信组织部门名称或协同岗名称", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "xx部门")
    private String businessName;

    @Schema(description = "警信组织部门名称或协同岗名称", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "xx部门")
    private List<String> targetNames;

    @Schema(description = "用户或群组名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "某某用户")
    private String targetName;

    @Schema(description = "父组织或所属组织名称", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "所属组织")
    private String orgName;

    @Schema(description = "身份证号", requiredMode = Schema.RequiredMode.REQUIRED, example = "51000019900307002X")
    private String idCard;
}
