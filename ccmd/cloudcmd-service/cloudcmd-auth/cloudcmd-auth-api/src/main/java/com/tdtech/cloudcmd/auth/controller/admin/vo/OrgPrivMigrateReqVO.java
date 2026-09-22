package com.tdtech.cloudcmd.auth.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "组织权限迁移请求VO")
public class OrgPrivMigrateReqVO {

    @Schema(description = "每批处理数量", example = "100")
    private Integer batchSize;
}