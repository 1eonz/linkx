package com.tdtech.cloudcmd.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "自定义通讯录节点创建或更新参数")
public class DepartmentNodeCustomCO {

    @Schema(description = "自定义通讯录ID", required = true, example = "1")
    private Long departmentCustomId;

    @Schema(description = "编码", example = "DEPT001")
    private String code;

    @Schema(description = "名称", required = true, example = "技术部")
    private String name;

    @Schema(description = "父节点ID，空或0表示根节点", example = "0")
    private Long parentId;

    @Schema(description = "类型：1-单位；2-部门", required = true, example = "2", allowableValues = {"1", "2"})
    private Integer type;

    public Long getDepartmentCustomId() {
        return departmentCustomId;
    }

    public void setDepartmentCustomId(Long departmentCustomId) {
        this.departmentCustomId = departmentCustomId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }
}
