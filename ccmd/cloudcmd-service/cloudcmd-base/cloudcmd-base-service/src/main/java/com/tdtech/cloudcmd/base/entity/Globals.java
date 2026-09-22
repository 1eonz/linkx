package com.tdtech.cloudcmd.base.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 全局变量信息表
 * </p>
 *
 * @author zhuangzl
 * @since 2020-06-07
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
    private String name;
    /**
     * 值
     *
     */
    private String value;
    /**
     * 备注
     */
    private String remark;
    /**
     * 状态 0-可用 1-禁用
     */
    private Integer status;
    private Date gmtCreated;
    private Date gmtModified;

}
