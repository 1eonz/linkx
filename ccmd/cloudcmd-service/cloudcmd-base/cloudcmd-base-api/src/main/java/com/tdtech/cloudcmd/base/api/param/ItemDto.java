package com.tdtech.cloudcmd.base.api.param;

import java.io.Serializable;

import lombok.Data;

/**
 * @author mWX556161
 * @date 2020/6/16 15:56
 */
@Data
public class ItemDto implements Serializable {

    /**
     * 字典项名称
     */
    private String name;

    /**
     * 字典项值（为int型？，注意类型转换）
     */
    private String value;

    /**
     * 是否是默认值（Option列表使用）0-否（默认） 1-是
     */
    private Integer isDefault;
}
