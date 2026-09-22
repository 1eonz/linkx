package com.tdtech.cloudcmd.base.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 装备资源信息表（包括设备和坐席）
 * </p>
 *
 * @author LSM
 * @since 2021-06-03
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_equipment")
public class Equipments implements Serializable {

    public static final String ID = "id";
    public static final String CODE = "code";
    public static final String NAME = "name";
    public static final String CATEGORY = "category";
    public static final String TYPE_ID = "type_id";
    public static final String ORGANIZATION_ID = "organization_id";
    public static final String EXTEND_INFO = "extend_info";
    public static final String CAPABILITY = "capability";
    public static final String REMARK = "remark";
    public static final String STATUS = "status";
    public static final String GMT_CREATED = "gmt_created";
    public static final String GMT_MODIFIED = "gmt_modified";
    public static final String ACCOUNT = "account";
    public static final String EXECUTOR_ID = "executor_id";
    private static final long serialVersionUID = 1L;
    /**
     * 主键id
     */
    private Long id;
    private String code;
    /**
     * 资源名称
     */
    private String name;
    /**
     * 装备种类(数据字典定义) 0-未知 1-设备 2-坐席 3-车辆（驾驶工具） 4-摩托车
     */
    private Integer category;
    /**
     * 装备分类ID。
     */
    private Long typeId;
    /**
     * 组织ID。坐席时，为所在组织ID。
     */
    private Long organizationId;
    /**
     * 扩展属性
     */
    private String extendInfo;
    /**
     * 装备能力（同一类型的装备可能具有不通的能力）。多个能力以逗号隔开。 装备能力在数据字典定义。目前的装备能力有点呼、短彩信、视频监控、视频回放等，可扩展。
     */
    private String capability;
    /**
     * 备注描述
     */
    private String remark;
    /**
     * 状态 0-正常 1-离职/报废/丢失
     */
    private Integer status;
    /**
     * gbid
     */
    private String gbid;
    private Date gmtCreated;
    private Date gmtModified;
    // 绑定的账号
    private String account;
    // 领用人
    private String executorId;

}
