package com.tdtech.cloudcmd.base.api.param;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

/**
 * <p>
 * 数据字典项表（获取字典项通过字典类型code）
 * </p>
 *
 * @author zhuangzl
 * @since 2020-06-07
 */
@Data
public class DictionaryDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 编号，主键ID。数据初始化。顺序加1
     */
    private Long id;

    /**
     * 所属字典类型ID
     */
    private Long typeId;

    /**
     * 字典项名称
     */
    private String name;

    /**
     * 字典项值（为int型？，注意类型转换）
     */
    private String value;

    /**
     * （同类型下）字典项排序
     */
    private Integer sort;

    /**
     * 是否是默认值（Option列表使用）0-否（默认） 1-是
     */
    private Integer isDefault;

    /**
     * 状态 0-正常 1-禁用
     */
    private Integer status;

    private Date gmtCreated;

    private Date gmtModified;

}
