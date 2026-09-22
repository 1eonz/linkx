package com.tdtech.cloudcmd.im.jingxin.api.entity.group;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "开放API群组统计项")
public class OpenApiGroupCountVO implements Serializable {

    @Schema(description = "普通群组未归档的计数")
    private Integer normalGroupUnarchivedCount;

    @Schema(description = "普通群组已归档的计数")
    private Integer normalGroupArchivedCount;

    @Schema(description = "协同群组已归档的计数")
    private Integer coopGroupArchivedCount;

    @Schema(description = "协同群组未归档的计数")
    private Integer coopGroupUnarchivedCount;
}
