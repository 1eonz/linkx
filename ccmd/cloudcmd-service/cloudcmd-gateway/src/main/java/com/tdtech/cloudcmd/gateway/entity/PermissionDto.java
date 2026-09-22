package com.tdtech.cloudcmd.gateway.entity;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

/**
 * @author mWX556161
 * @date 2021/1/22 14:18
 */
@Data
public class PermissionDto implements Serializable {

    /**
     * 角色名称
     */
    private String role;

    /**
     * 用户所在组织
     */
    private Long organization;

    /**
     * 角色类型 0-管理员（可以操作管辖范围内的所有用户信息） 1-普通用户（只能管理自己的信息）
     */
    private Integer type;

    /**
     * 菜单权限列表
     */
    private List<String> menus;

    /**
     * 功能权限列表
     */
    private List<String> actions;

    /**
     * 所有功能权限列表
     */
    private List<String> allActions;

    /**
     * 组织权限列表
     */
    private List<String> organizations;
}
