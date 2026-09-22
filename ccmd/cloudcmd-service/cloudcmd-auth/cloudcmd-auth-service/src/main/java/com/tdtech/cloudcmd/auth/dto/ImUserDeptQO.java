package com.tdtech.cloudcmd.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "IM用户按部门查询对象")
public class ImUserDeptQO extends ImUserQO {

    @Schema(description = "是否包含子部门：0-不包含（仅本部门直属人员），1-包含（本部门及子孙部门人员）")
    private Integer isChildren;
}
