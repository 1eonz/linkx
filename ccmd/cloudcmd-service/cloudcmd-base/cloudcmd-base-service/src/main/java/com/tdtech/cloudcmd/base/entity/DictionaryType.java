package com.tdtech.cloudcmd.base.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 数据字典类型表
 * </p>
 *
 * @author zhuangzl
 * @since 2020-06-07
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_dictionary_type")
public class DictionaryType implements Serializable {

    public static final String ID = "id";
    public static final String CODE = "code";
    public static final String NAME = "name";
    public static final String GMT_CREATED = "gmt_created";
    public static final String GMT_MODIFIED = "gmt_modified";
    private static final long serialVersionUID = 1L;
    /**
     * 编号，主键ID。数据初始化。顺序加1
     */
    private Long id;
    /**
     * 类型编码。根据编码获取字典项
     */
    private String code;
    /**
     * 类型名称
     */
    private String name;
    /**
     * 状态 0-正常 1-禁用
     */
    private Integer status;
    private Date gmtCreated;
    private Date gmtModified;

}
