package com.tdtech.cloudcmd.auth.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.List;


/**
 * 角色数据权限实体类，用于表示组织架构中的角色及其子节点信息。
 */
@Getter
@Setter
public class RoleDataPriv {

    /**
     * 主键ID，唯一标识一个角色数据权限项。
     */
    private Long id;

    /**
     * 角色编码，用于唯一标识角色。
     */
    private String code;

    /**
     * 角色名称，描述角色的具体含义。
     */
    private String name;

    /**
     * 排序字段，用于控制角色在列表中的显示顺序。
     */
    private Integer sort;

    /**
     * 是否有当前节点的数据权限
     */
    private boolean hasPermission;

    /**
     * 完整路径，表示当前角色在组织架构中的完整层级路径（以逗号分隔的ID集合）。
     */
    private String fullPath;

    /**
     * 父级角色ID，表示当前角色的上级角色。
     */
    private Long parentId;

    /**
     * 角色简称，通常用于简化显示。
     */
    private String shortName;

    /**
     * 创建时间，记录该角色数据权限项的创建时间。
     */
    private String gmtCreated;

    /**
     * 父级角色编码，表示上级角色的编码。
     */
    private String parentCode;

    /**
     * 父级角色名称，表示上级角色的名称。
     */
    private String parentName;

    /**
     * 最后修改时间，记录该角色数据权限项的最后更新时间。
     */
    private String gmtModified;

    /**
     * 完整路径编码，表示当前角色在组织架构中的完整层级路径（以逗号分隔的编码集合）。
     */
    private String fullPathCode;

    /**
     * 完整路径名称，表示当前角色在组织架构中的完整层级路径（以逗号分隔的名称集合）。
     */
    private String fullPathName;

    /**
     * 子角色列表，表示当前角色下的所有子角色数据权限项。
     */
    private List<RoleDataPriv> children;
}
