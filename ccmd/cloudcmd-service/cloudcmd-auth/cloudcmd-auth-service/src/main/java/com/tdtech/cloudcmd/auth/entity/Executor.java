package com.tdtech.cloudcmd.auth.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 执行者信息表（包括人和车辆，车辆作为执行主体，是执行者。作为驾驶工具时，是装备）
 * </p>
 *
 * @author mWX556161
 * @since 2020-11-24
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_executor")
public class Executor implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String CODE = "code";
    public static final String NAME = "name";
    public static final String ID_CARD_NUM = "idCardNum";
    /**
     * 主键ID
     */
    private Long id;
    /**
     * 警员编号
     */
    private String code;
    /**
     * 真实姓名
     */
    private String name;
    /**
     * 组织ID
     */
    private Long organizationId;
    /**
     * REPERESERVE
     */
    private String remark;
    /**
     * 创建时间
     */
    private Date gmtCreated;
    /**
     * 修改时间
     */
    private Date gmtModified;

    private String phoneNum;

    private String account;

    private String emailAddr;

    private String headShot;

    private String idCardNum;

    /**
     * 用户类型）0-超级管理员 1-个人用户 2-DELETED
     */
    private Integer type = 1;

    /**
     * 扩展属性
     */
    private String extendInfo;

}
