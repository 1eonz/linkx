package com.tdtech.cloudcmd.auth.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.Date;

@TableName("tb_department_custom")
@Schema(description = "自定义通讯录")
public class DepartmentCustom implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    @Schema(description = "ID")
    private Long id;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "自定义通讯录名称")
    private String name;

    @Schema(description = "管理员创建人ID")
    private Long createUserId;

    @Schema(description = "创建时间")
    private Date gmtCreated;

    @Schema(description = "值班类型")
    private Integer dutyType;

    public Long getId() {
        return id;
    }

    public DepartmentCustom setId(Long id) {
        this.id = id;
        return this;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public DepartmentCustom setTenantId(Long tenantId) {
        this.tenantId = tenantId;
        return this;
    }

    public String getName() {
        return name;
    }

    public DepartmentCustom setName(String name) {
        this.name = name;
        return this;
    }

    public Long getCreateUserId() {
        return createUserId;
    }

    public DepartmentCustom setCreateUserId(Long createUserId) {
        this.createUserId = createUserId;
        return this;
    }

    public Date getGmtCreated() {
        return gmtCreated;
    }

    public DepartmentCustom setGmtCreated(Date gmtCreated) {
        this.gmtCreated = gmtCreated;
        return this;
    }

    public Integer getDutyType() {
        return dutyType;
    }

    public DepartmentCustom setDutyType(Integer dutyType) {
        this.dutyType = dutyType;
        return this;
    }
}
