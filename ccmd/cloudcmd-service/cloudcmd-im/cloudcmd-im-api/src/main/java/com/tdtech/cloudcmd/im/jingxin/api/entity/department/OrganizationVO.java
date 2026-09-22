package com.tdtech.cloudcmd.im.jingxin.api.entity.department;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class OrganizationVO implements Serializable{

    @Schema(description = "主键ID")
    private Long id;

    /**
     * 部门编号
     */
    @Schema(description = "部门编号")
    private String code;

    /**
     * 部门名称
     */
    @Schema(description = "部门名称")
    private String name;

    /**
     * 部门简称
     */
    @Schema(description = "部门简称")
    private String shortName;

    /**
     * 上级部门ID
     */
    @Schema(description = "上级部门ID")
    private Long parentId;

    /**
     * 上级部门编号
     */
    @Schema(description = "上级部门编号")
    private String parentCode;

    /**
     * 上级部门名称
     */
    @Schema(description = "上级部门名称")
    private String parentName;

    /**
     * 排序
     */
    @Schema(description = "排序")
    private Integer sort;

    /**
     * 部门ID全路径，逗号隔开
     */
    @Schema(description = "部门ID全路径，逗号隔开")
    private String fullPath;

    /**
     * 部门编码全路径，逗号隔开
     */
    @Schema(description = "部门编码全路径，逗号隔开")
    private String fullPathCode;

    /**
     * 部门名称全路径，逗号隔开
     */
    @Schema(description = "部门名称全路径，逗号隔开")
    private String fullPathName;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date gmtCreated;

    /**
     * 修改时间
     */
    @Schema(description = "修改时间")
    private Date gmtModified;

    private List<OrganizationVO> children = new ArrayList<>();
}
