package com.tdtech.cloudcmd.auth.dto;

import com.tdtech.cloudcmd.auth.entity.DepartmentCustom;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "自定义通讯录")
public class DepartmentCustomVO extends DepartmentCustom {

    @Schema(description = "根节点数量")
    private Long rootNodeCount;

    public Long getRootNodeCount() {
        return rootNodeCount;
    }

    public void setRootNodeCount(Long rootNodeCount) {
        this.rootNodeCount = rootNodeCount;
    }
}
