package com.tdtech.cloudcmd.admin.resource.entity.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import lombok.Data;

/**
 * @author mWX556161
 * @date 2020/7/29 18:22
 */
@Data
public class MenuMoveDto {

    /**
     * 当前菜单id
     */
    @NotNull
    private Long currentId;

    /**
     * 当前菜单的父id
     */
    @NotNull
    private Long currentParentId;

    /**
     * 当前菜单的排序号
     */
    @NotNull
    private Integer currentSort;

    /**
     * 移动到的目标id
     */
    @NotNull
    private Long targetId;

    /**
     * 移动到的目标父id
     */
    @NotNull
    private Long targetParentId;

    /**
     * 目标菜单的排序号
     */
    @NotNull
    private Integer targetSort;

    /**
     * 目标菜单的层级
     */
    @NotNull
    private Integer targetLevel;

    /**
     * before、after、inner 具体的拖动方式
     */
    @NotBlank
    private String location;
}
