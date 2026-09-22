package com.tdtech.cloudcmd.im.jingxin.client.entity;

import lombok.Data;

@Data
public class ImUserDeptInfoVO {
    private Long departmentId;
    private String departmentCode;
    private String departmentName;
    private Boolean isPrimary;
    private String positionName;
    private Integer sort;
    private String fullPath;
    private String fullPathCode;
    private String fullPathName;
}
