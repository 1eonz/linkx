package com.tdtech.cloudcmd.auth.dto;

import java.io.Serializable;
import java.util.List;

import com.tdtech.cloudcmd.util.json.JsonArray;

import lombok.Data;
import lombok.ToString;

/**
 * @author mWX556161
 * @date 2021/1/22 14:18
 */
@Data
@ToString
public class PermissionDto implements Serializable {

    /**
     * 角色名称
     */
    @Deprecated
    private String role;

    /**
     * 用户所在组织
     */
    private Long organization;

    /**
     * 角色类型 0-管理员（可以操作管辖范围内的所有用户信息） 1-普通用户（只能管理自己的信息）
     */
    @Deprecated
    private Integer type;

    /**
     * 菜单权限列表
     */
    private List<String> menus;

    /**
     * 功能权限列表
     */
    @Deprecated
    private List<String> actions;

    /**
     * 所有功能权限列表
     */
    private List<String> allActions;

    /**
     * 组织权限列表
     */
    private JsonArray organizations;

    /**
    * 跟菜单权限menu 绑定，需要遍历每一个role的menu 缓存值。
    */
    private List<Long> roleIds;

    private String userId;

    /**
     * 跟权限相关
     */
    private Long applicationId;
    /**
     * 是否有子级数据权限 0 是 1 否
     */
    @Deprecated
    private Integer hasChildOrgPriv;
}
