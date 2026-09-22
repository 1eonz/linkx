package com.tdtech.cloudcmd.icp.proxy.controller.vo;

import com.tdtech.cloudcmd.mysql.entity.CcmdPageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
@Schema(description = "部门查询对象")
public class DepartmentQO  extends CcmdPageParam {

    @Schema(description = "上级部门ID", example = "upper_dept001")
    private String upperDepartmentId;
    
    @Schema(description = "根部门ID", example = "root_dept001")
    private String rootDepartmentId;

    @Schema(description = "模糊查询关键字")
    private String keywords;
}
