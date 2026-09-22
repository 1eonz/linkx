package com.tdtech.cloudcmd.admin.resource.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 扩展信息属性定义，通过反射实现扩展类
 * </p>
 *
 * @author mWX556161
 * @since 2020-06-09
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_extend_info_properties")
public class ExtendInfoProperties implements Serializable {

    public static final String ID = "id";
    public static final String CODE = "code";
    public static final String NAME = "name";
    public static final String LABEL = "label";
    public static final String SORT = "sort";
    public static final String STATUS = "status";
    public static final String GMT_CREATED = "gmt_created";
    public static final String GMT_MODIFIED = "gmt_modified";
    private static final long serialVersionUID = 1L;
    /**
     * 主键id
     */

    private Long id;
    /**
     * 类型编码 PERSON,VEHICLE,DEVICE,SEAT,CEMERA,POLICEBOX,HYDRANT
     */
    private String code;
    /**
     * 属性字段名称
     */
    private String name;
    /**
     * 属性显示名称。要考虑国际化
     */
    private String label;
    /**
     * 显示顺序
     */
    private Integer sort;
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
