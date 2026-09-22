package com.tdtech.cloudcmd.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "自定义通讯录创建或更新参数")
public class DepartmentCustomCO {

    @Schema(description = "自定义通讯录名称", required = true, example = "默认通讯录")
    private String name;

    @Schema(description = "值班类型", required = true, example = "1")
    private Integer dutyType;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getDutyType() {
        return dutyType;
    }

    public void setDutyType(Integer dutyType) {
        this.dutyType = dutyType;
    }
}
