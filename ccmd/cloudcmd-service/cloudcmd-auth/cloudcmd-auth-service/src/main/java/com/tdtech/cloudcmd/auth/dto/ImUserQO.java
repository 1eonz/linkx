package com.tdtech.cloudcmd.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "IM用户查询对象")
public class ImUserQO {

    @Schema(description = "用户编码，模糊查询")
    private String code;

    @Schema(description = "用户姓名，模糊查询")
    private String name;

    @Schema(description = "手机号码，模糊查询")
    private String mobile;

    @Schema(description = "邮箱地址，模糊查询")
    private String email;

    @Schema(description = "身份证号码，模糊查询")
    private String idCard;

    @Schema(description = "部门编码，模糊查询")
    private String departmentCode;

    @Schema(description = "部门名称，模糊查询")
    private String departmentName;

    @Schema(description = "权限字符串，组织ID，逗号分割")
    private String privString;
}
