package com.tdtech.cloudcmd.base.entity;

/**
 * 租户全局变量信息表
 * @author hks
 * @date 2024/7/2
 */

import java.io.Serializable;
import java.util.Date;

import javax.validation.constraints.NotBlank;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_tenant_globals")
public class TenantGlobals implements Serializable {

    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String VALUE = "value";
    public static final String REMARK = "remark";
    public static final String STATUS = "status";
    public static final String ORG_ID = "org_id";
    public static final String GMT_CREATED = "gmt_created";
    public static final String GMT_MODIFIED = "gmt_modified";
    private static final long serialVersionUID = 1L;
    /**
     * 主键id
     */
    private Long id;
    /**
     * 变量名称
     */
    @NotBlank
    private String name;
    /**
     * 值
     */
    @NotBlank
    private String value;
    /**
     * 备注
     */
    private String remark;
    /**
     * 状态 0-可用 1-禁用
     */
    private Integer status;
    /**
     * 组织id
     */
    private Long orgId;
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date gmtCreated;
    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date gmtModified;

}

