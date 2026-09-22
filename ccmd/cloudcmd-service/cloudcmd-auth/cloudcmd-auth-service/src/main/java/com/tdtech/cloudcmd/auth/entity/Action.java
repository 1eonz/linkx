package com.tdtech.cloudcmd.auth.entity;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 操作权限信息表
 * </p>
 *
 * @author zhuangzl
 * @since 2020-06-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class Action implements Serializable {

    public static final String NAME = "name";
    public static final String ACTION = "action";
    public static final String MENU_ID = "menu_id";
    public static final String APPLICATION_ID = "application_id";
    public static final String REMARK = "remark";
    public static final String STATUS = "status";
    public static final String GMT_CREATED = "gmt_created";
    public static final String GMT_MODIFIED = "gmt_modified";
    private static final long serialVersionUID = 1L;
    /**
     * 名称
     */
    private String name;
    /**
     * 权限标识
     */
    private String action;
    /**
     * 所属菜单ID
     */
    private Long menuId;
    /**
     * 所属应用ID(同一接口提供给CAPP和CDC，名称不同，标识相同)
     */
    private Long applicationId;
    /**
     * 备注
     */
    private String remark;
    /**
     * 状态 0-正常 1-禁用
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
