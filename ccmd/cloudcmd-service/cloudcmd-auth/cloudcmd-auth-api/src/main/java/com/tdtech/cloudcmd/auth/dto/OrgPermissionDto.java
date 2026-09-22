package com.tdtech.cloudcmd.auth.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class OrgPermissionDto implements Serializable {
    private String id;
    private String code;
    private String name;
    private Integer sort;
    private List<OrgPermissionDto> children;
    private String fullPath;
    private String parentId;
    private String shortName;
    private String gmtCreated;
    private String parentCode;
    private String parentName;
    private String gmtModified;
    private String fullPathCode;
    private String fullPathName;
    /**
     * 是否有当前节点的数据权限
     */
    private boolean hasPermission;
}
