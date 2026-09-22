package com.tdtech.cloudcmd.base.param;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

/**
 * <p>
 * 扩展信息属性定义，通过反射实现扩展类
 * </p>
 *
 * @author zhuangzl
 * @since 2020-06-07
 */
@Data
public class ExtendInfoPropertiesDto implements Serializable {

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

    private Date gmtCreated;

    private Date gmtModified;

}
