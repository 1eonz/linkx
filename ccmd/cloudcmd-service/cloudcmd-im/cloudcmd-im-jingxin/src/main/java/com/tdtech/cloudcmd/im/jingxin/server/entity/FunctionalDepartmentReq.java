package com.tdtech.cloudcmd.im.jingxin.server.entity;

import lombok.Data;

/**
 * 职能部门请求参数
 */
@Data
public class FunctionalDepartmentReq {

    /**
     * 部门名称
     */
    private String name;

    /**
     * 父部门ID，顶级部门为NULL
     */
    private Long parentId;

    /**
     * 创建人
     */
    private String creator;
}