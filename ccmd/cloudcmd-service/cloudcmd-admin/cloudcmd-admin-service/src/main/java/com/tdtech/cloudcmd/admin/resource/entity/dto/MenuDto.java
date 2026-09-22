package com.tdtech.cloudcmd.admin.resource.entity.dto;

import java.util.Date;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.tdtech.cloudcmd.admin.resource.entity.Menu;

import lombok.Data;

/**
 * @author mWX556161
 * @date 2020/7/27 14:34
 */
@Data
public class MenuDto {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 菜单标题
     */
    @NotBlank
    private String name;

    /**
     * 上级菜单id。0-无上级
     */
    private Long parentId;

    /**
     * 所属应用ID
     */
    @NotNull
    private Long applicationId;

    /**
     * 所属应用名称
     */
    private String applicationName;

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
     * 父级名称
     */
    private String parentName;

    public void buildMenuDto(Menu menu) {
        this.setId(menu.getId());
        this.setApplicationId(menu.getApplicationId());
        this.setName(menu.getName());
        this.setImgurl(menu.getImgurl());
        this.setIsleaf(menu.getIsleaf());
        this.setLevel(menu.getLevel());
        this.setParentId(menu.getParentId());
        this.setSort(menu.getSort());
        this.setStatus(menu.getStatus());
        this.setUrl(menu.getUrl());
        this.setGmtCreated(menu.getGmtCreated());
        this.setGmtModified(menu.getGmtModified());
    }
}
