package com.tdtech.cloudcmd.auth.entity;

import com.tdtech.cloudcmd.auth.controller.admin.vo.OrgPrivDto;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;


/**
 * 角色信息DTO-包含角色的数据权限
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class RoleDto extends Role implements Serializable {
    /**
     * 角色数据权限列表
     */
    private List<OrgPrivDto> orgPrivList;
}
