package com.tdtech.cloudcmd.auth.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Objects;

@Data
@Schema(description = "组织权限DTO（用于管理后台数据交互）")
public class OrgPrivDto implements Serializable {

    @Schema(description = "组织ID")
    private Long id;

    @Schema(description = "组织名称")
    private String name;

    @Schema(description = "组织编码")
    private String code;

    @Schema(description = "组织完整路径")
    private String fullPath;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        OrgPrivDto privDto = (OrgPrivDto) o;
        return Objects.equals(id, privDto.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}