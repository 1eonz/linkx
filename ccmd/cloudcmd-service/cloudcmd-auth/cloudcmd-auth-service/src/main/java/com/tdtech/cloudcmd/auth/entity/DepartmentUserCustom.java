package com.tdtech.cloudcmd.auth.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.Date;

/**
 * 自定义通讯录用户关系实体。
 */
@TableName("tb_department_node_user_custom")
@Schema(description = "自定义通讯录用户关系实体")
public class DepartmentUserCustom implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "自定义通讯录节点ID")
    private Long customDeptId;

    @Schema(description = "警信用户ID")
    private Long userId;

    @Schema(description = "创建人ID")
    private Long createUserId;

    @Schema(description = "创建时间")
    private Date gmtCreated;

    public Long getId() {
        return id;
    }

    public DepartmentUserCustom setId(Long id) {
        this.id = id;
        return this;
    }

    public Long getCustomDeptId() {
        return customDeptId;
    }

    public DepartmentUserCustom setCustomDeptId(Long customDeptId) {
        this.customDeptId = customDeptId;
        return this;
    }

    public Long getUserId() {
        return userId;
    }

    public DepartmentUserCustom setUserId(Long userId) {
        this.userId = userId;
        return this;
    }

    public Long getCreateUserId() {
        return createUserId;
    }

    public DepartmentUserCustom setCreateUserId(Long createUserId) {
        this.createUserId = createUserId;
        return this;
    }

    public Date getGmtCreated() {
        return gmtCreated;
    }

    public DepartmentUserCustom setGmtCreated(Date gmtCreated) {
        this.gmtCreated = gmtCreated;
        return this;
    }
}
