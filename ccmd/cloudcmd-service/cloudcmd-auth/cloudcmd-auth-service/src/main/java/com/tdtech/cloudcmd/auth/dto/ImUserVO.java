package com.tdtech.cloudcmd.auth.dto;

import com.tdtech.cloudcmd.auth.controller.admin.vo.OrgPrivDto;
import com.tdtech.cloudcmd.auth.entity.ImUserDO;
import com.tdtech.cloudcmd.auth.entity.RoleDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
public class ImUserVO extends ImUserDO {

    private RoleDto role;

    private List<Long> orgIds;

    private List<OrgPrivDto> orgList;

    @Schema(description = "是否被协同岗绑定：1-已绑定，0-未绑定")
    private Integer isBinding = 0;

    @Schema(description = "是否在当前管理员数据权限范围内：true-有权限，false-无权限")
    private boolean hasPermission = true;

}