package com.tdtech.cloudcmd.admin.resource.entity.dto;

import java.io.Serializable;
import java.util.Date;

import javax.validation.constraints.NotBlank;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 数据字典项表（获取字典项通过字典类型code）
 * </p>
 *
 * @author mWX556161
 * @since 2020-07-16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class DictionaryItemDto implements Serializable {

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
    @NotBlank
    private String name;

    /**
     * 字典项值（为int型？，注意类型转换）
     */
    @NotBlank
    private String value;

    /**
     * （同类型下）字典项排序。考虑数量少，不加索引
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

    /**
     * 创建时间
     */
    private Date gmtCreated;

    /**
     * 更新时间
     */
    private Date gmtModified;

    /**
     * 字典类型编号
     */
    @NotBlank
    private String typeCode;

}
