package com.tdtech.cloudcmd.base.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 数据字典项表（获取字典项通过字典类型code）
 * </p>
 *
 * @author zhuangzl
 * @since 2020-06-07
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_dictionary_item")
public class DictionaryItem implements Serializable {

    public static final String ID = "id";
    public static final String TYPE_ID = "type_id";
    public static final String NAME = "name";
    public static final String VALUE = "value";
    public static final String SORT = "sort";
    public static final String IS_DEFAULT = "is_default";
    public static final String STATUS = "status";
    public static final String GMT_CREATED = "gmt_created";
    public static final String GMT_MODIFIED = "gmt_modified";
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
