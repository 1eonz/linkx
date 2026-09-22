package com.tdtech.cloudcmd.icp.proxy.ws.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Schema(description = "部门通知信息")
public class DepartmentNotify {

    @JsonProperty("departmentcode")
    @Schema(description = "部门编码", example = "DEPT001")
    private String departmentcode;

    @JsonProperty("departmentid")
    @Schema(description = "部门ID", example = "dept001")
    private String departmentid;

    @JsonProperty("departmentname")
    @Schema(description = "部门名称", example = "技术部")
    private String departmentname;

    @JsonProperty("servermode")
    @Schema(description = "服务器模式", example = "master")
    private String servermode;

    @JsonProperty("upperdepartmentId")
    @Schema(description = "上级部门ID", example = "dept000")
    private String upperdepartmentId;

    @JsonProperty("vpnid")
    @Schema(description = "VPN ID", example = "vpn001")
    private String vpnid;
}
