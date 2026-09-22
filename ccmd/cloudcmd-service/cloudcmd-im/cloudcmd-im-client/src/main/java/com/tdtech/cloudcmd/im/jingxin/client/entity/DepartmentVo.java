package com.tdtech.cloudcmd.im.jingxin.client.entity;

import lombok.Data;

@Data
public class DepartmentVo {
    private String departmentId;
    private String departmentName;
    private String fullPath;
    private String fullPathName;
    private Boolean primary;
    private Integer sort;
}