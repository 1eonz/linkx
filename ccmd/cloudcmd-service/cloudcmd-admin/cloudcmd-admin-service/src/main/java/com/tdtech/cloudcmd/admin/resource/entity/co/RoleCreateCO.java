package com.tdtech.cloudcmd.admin.resource.entity.co;

import java.util.List;

import javax.validation.constraints.NotBlank;

import com.tdtech.cloudcmd.auth.controller.admin.vo.OrgPrivDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class RoleCreateCO {
    @NotBlank
    @Schema(description = "名称")
    private String name;
    @Schema(description = "icc菜单ID")
    private List<String> iccPrivJson;
    @Schema(description = "capp菜单ID")
    private List<String> cappPrivJson;
    @Schema(description = "admin菜单ID")
    private List<String> adminPrivJson;
    @Schema(description = "im组织权限列表")
    private List<OrgPrivDto> orgPrivList;

    private Integer type = 1;
}