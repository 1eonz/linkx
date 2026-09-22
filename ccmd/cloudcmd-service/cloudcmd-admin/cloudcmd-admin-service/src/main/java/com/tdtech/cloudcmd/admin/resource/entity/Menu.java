package com.tdtech.cloudcmd.admin.resource.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 权限菜单信息表
 * </p>
 *
 * @author mWX556161
 * @since 2020-07-27
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_menu")
public class Menu implements Serializable {

    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String PARENT_ID = "parent_id";
    public static final String APPLICATION_ID = "application_id";
    public static final String URL = "url";
    public static final String IMGURL = "imgurl";
    public static final String ISLEAF = "isleaf";
    public static final String LEVEL = "level";
    public static final String SORT = "sort";
    public static final String STATUS = "status";
    public static final String GMT_CREATED = "gmt_created";
    public static final String GMT_MODIFIED = "gmt_modified";
    private static final long serialVersionUID = 1L;
    /**
     * 主键ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    /**
     * 菜单标题
     */
    @NotBlank
    private String name;
    /**
     * 上级菜单id。-1-无上级
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;
    /**
     * 所属应用ID
     */
    @NotNull
    @JsonSerialize(using = ToStringSerializer.class)
    private Long applicationId;
    /**
     * 菜单链接（相对URL）
     */
    private String url;
    /**
     * 菜单图片地址
     */
    private String imgurl;
    /**
     * 是否叶子节点（1-是，0-不是）
     */
    private Integer isleaf;
    /**
     * 菜单层级: 0：一级菜单,1：二级菜单,2：三级菜单
     */
    private Integer level;
    /**
     * 排序
     */
    private Integer sort;
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
    /**
     * 下属层级
     */
    @TableField(exist = false)
    private List<Menu> children;

}
