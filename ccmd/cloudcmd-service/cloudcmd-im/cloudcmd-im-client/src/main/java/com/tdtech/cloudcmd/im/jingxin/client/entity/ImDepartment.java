package com.tdtech.cloudcmd.im.jingxin.client.entity;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ImDepartment {

    private Long id;
    private String code;
    private String name;
    private String shortName;
    private Long parentId;
    private String parentCode;
    private String parentName;
    private Integer sort;
    private String fullPath;
    private String fullPathCode;
    private String fullPathName;
    private Long gmtCreated;
    private Long gmtModified;
    private List<ImDepartment> children;

}
