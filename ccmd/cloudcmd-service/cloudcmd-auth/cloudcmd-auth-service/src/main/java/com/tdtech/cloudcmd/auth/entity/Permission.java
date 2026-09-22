package com.tdtech.cloudcmd.auth.entity;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 权限信息表(可以很方便的扩展)
 * </p>
 *
 * @author zhuangzl
 * @since 2020-06-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class Permission implements Serializable {

    public static final String CATEGORY = "category";
    public static final String RESOURCE_ID = "resource_id";
    public static final String STATUS = "status";
    public static final String GMT_CREATED = "gmt_created";
    public static final String GMT_MODIFIED = "gmt_modified";
    private static final long serialVersionUID = 1L;
    /**
     * 权限种类（数据字典定义,可扩展）：0-组织权限 1-功能权限；2-菜单权限 3--应用权限 4-摄像头
     */
    private Integer category;
    /**
     * 权限资源ID,根据类型，可以是菜单ID,功能ID等
     */
    private Integer resourceId;
    /**
     * 状态 0-可用 1-禁用
     */
    private Integer status;
    /**
     * 创建时间
     */
    private Date gmtCreated;
    /**
     * 修改时间
     */
    private Date gmtModified;

}
