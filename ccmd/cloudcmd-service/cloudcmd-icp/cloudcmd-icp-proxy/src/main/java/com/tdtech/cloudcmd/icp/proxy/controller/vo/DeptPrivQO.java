package com.tdtech.cloudcmd.icp.proxy.controller.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
@Schema(description = "部门权限推送对象")
public class DeptPrivQO {

    @Schema(description = "部门ID列表")
    private List<String> deptCodes;

    @Schema(description = "权限列表")
    private List<String> privs;

    @Schema(description = "是否包含子节点：0-不包含，1-包含")
    private Integer isChildren;
}
