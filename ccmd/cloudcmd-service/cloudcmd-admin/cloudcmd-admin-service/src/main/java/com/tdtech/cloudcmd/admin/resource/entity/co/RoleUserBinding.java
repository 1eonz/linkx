package com.tdtech.cloudcmd.admin.resource.entity.co;

import java.util.List;

import lombok.Data;

@Data
public class RoleUserBinding {

    private Long roleId;

    private List<Long> userIds;
}
