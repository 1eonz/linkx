package com.tdtech.cloudcmd.auth.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.Date;

@TableName("tb_department_node_custom")
@Schema(description = "自定义通讯录节点")
public class DepartmentNodeCustom implements Serializable {

    public static final int TYPE_UNIT = 1;
    public static final int TYPE_DEPARTMENT = 2;

    private static final long serialVersionUID = 1L;

    @TableId
    @Schema(description = "ID")
    private Long id;

    @Schema(description = "自定义通讯录ID")
    private Long departmentCustomId;

    @Schema(description = "编码")
    private String code;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "父ID")
    private Long parentId;

    @Schema(description = "类型：1-单位；2-部门")
    private Integer type;

    @Schema(description = "创建人ID")
    private Long createUserId;

    @Schema(description = "创建时间")
    private Date gmtCreated;

    public Long getId() {
        return id;
    }

    public DepartmentNodeCustom setId(Long id) {
        this.id = id;
        return this;
    }

    public Long getDepartmentCustomId() {
        return departmentCustomId;
    }

    public DepartmentNodeCustom setDepartmentCustomId(Long departmentCustomId) {
        this.departmentCustomId = departmentCustomId;
        return this;
    }

    public String getCode() {
        return code;
    }

    public DepartmentNodeCustom setCode(String code) {
        this.code = code;
        return this;
    }

    public String getName() {
        return name;
    }

    public DepartmentNodeCustom setName(String name) {
        this.name = name;
        return this;
    }

    public Long getParentId() {
        return parentId;
    }

    public DepartmentNodeCustom setParentId(Long parentId) {
        this.parentId = parentId;
        return this;
    }

    public Integer getType() {
        return type;
    }

    public DepartmentNodeCustom setType(Integer type) {
        this.type = type;
        return this;
    }

    public Long getCreateUserId() {
        return createUserId;
    }

    public DepartmentNodeCustom setCreateUserId(Long createUserId) {
        this.createUserId = createUserId;
        return this;
    }

    public Date getGmtCreated() {
        return gmtCreated;
    }

    public DepartmentNodeCustom setGmtCreated(Date gmtCreated) {
        this.gmtCreated = gmtCreated;
        return this;
    }
}
