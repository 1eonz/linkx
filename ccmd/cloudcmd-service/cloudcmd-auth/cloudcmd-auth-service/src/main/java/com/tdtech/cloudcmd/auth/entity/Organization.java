package com.tdtech.cloudcmd.auth.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 是否 撤销：0表示未撤销，1表示已撤销
 * </p>
 *
 * @author mWX556161
 * @since 2020-06-08
 */
@Getter
@Setter
@TableName("tb_organization")
public class Organization implements Serializable {

    /**
     * id唯一不变,传给前台id的精度会丢失，使用字符串进行序列化。
     */
    private Long id;
    /**
     * 组织code
     */
    private String code;
    /**
     * 名称
     */
    private String name;
    /**
     * 分类ID
     */
    private Long typeId;
    /**
     * 上级组织机构ID
     */
    private Long parentId;
    /**
     * 组织机构简称
     */
    private String shortName;
    /**
     * 所属部门全路径(组织可以重名)。上级部门全路径—+本级name。方便查询，不显示。
     */
    private String fullPathName;
    /**
     * 行政区划。
     */
    private String administrativeArea;
    /**
     * 组织所在层级
     */
    private Integer level;
    /**
     * 排序
     */
    private Integer sort;
    /**
     * 备注
     */
    private String remark;
    /**
     * 状态：0-正常,1-禁用
     */
    private Integer status;
    /**
     * 创建时间
     */
    private Date gmtCreated;
    /**
     * 绝对组织路径，使用逗号隔开
     */
    private String fullPath;
    /**
     * 修改时间
     */
    private Date gmtModified;

}
