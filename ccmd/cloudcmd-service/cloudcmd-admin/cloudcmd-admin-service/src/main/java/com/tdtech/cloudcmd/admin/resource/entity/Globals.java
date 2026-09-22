package com.tdtech.cloudcmd.admin.resource.entity;

import java.io.Serializable;
import java.util.Date;

import javax.validation.constraints.NotBlank;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 全局变量信息表
 * </p>
 *
 * @author mWX556161
 * @since 2020-07-16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_globals")
public class Globals implements Serializable {

    public static final String ID = "id";
    public static final String CLASSIFY = "classify";
    public static final String NAME = "name";
    public static final String VALUE = "value";
    public static final String REMARK = "remark";
    public static final String REMARK_EN = "remark_en";
    public static final String STATUS = "status";
    public static final String GMT_CREATED = "gmt_created";
    public static final String GMT_MODIFIED = "gmt_modified";
    private static final long serialVersionUID = 1L;
    /**
     * 主键id
     */
    private Long id;
    /**
     * 类别名称
     */
    private String classify;
    /**
     * 变量名称
     */
    @NotBlank
    private String name;
    /**
     * 值
     */
    private String value;
    /**
     * 备注
     */
    private String remark;

    private String remarkEn;
    /**
     * 状态 0-可用 1-禁用
     */
    private Integer status;
    /**
     * 创建时间
     */
    private Date gmtCreated;
    /**
     * 更新时间
     */
    private Date gmtModified;

}
