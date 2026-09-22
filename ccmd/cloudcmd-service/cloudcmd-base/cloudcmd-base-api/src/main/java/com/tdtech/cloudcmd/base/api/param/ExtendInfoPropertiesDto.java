package com.tdtech.cloudcmd.base.api.param;

import java.io.Serializable;

import lombok.Data;

/**
 * @author mWX556161
 * @date 2020/6/16 16:08
 */
@Data
public class ExtendInfoPropertiesDto implements Serializable {

    /**
     * 属性字段名称
     */
    private String name;

    /**
     * 属性显示名称。要考虑国际化
     */
    private String label;
}
