package com.tdtech.cloudcmd.auth.dto;

import com.tdtech.cloudcmd.auth.entity.DepartmentNodeCustom;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "自定义通讯录节点")
public class DepartmentNodeCustomVO extends DepartmentNodeCustom {

    @Schema(description = "子节点列表")
    private List<DepartmentNodeCustomVO> children = new ArrayList<>();

    @Schema(description = "节点下用户数量")
    private Long userCount;

    public List<DepartmentNodeCustomVO> getChildren() {
        return children;
    }

    public void setChildren(List<DepartmentNodeCustomVO> children) {
        this.children = children;
    }

    public Long getUserCount() {
        return userCount;
    }

    public void setUserCount(Long userCount) {
        this.userCount = userCount;
    }
}
