package com.tdtech.cloudcmd.linkx.dashboard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 协同岗上下岗统计明细表
 * TableName：tb_static_coop_duty_switch
 * <p>
 * 注意：DDL 中列名为 {@code switch}（MySQL 保留字），Java 字段用 {@code switchFlag}，
 * 通过 {@code @TableField("`switch`")} 显式映射并在 SQL 中用反引号转义。
 */
@TableName("tb_static_coop_duty_switch")
@Data
public class StaticCoopDutySwitch implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private String userName;

    private Long userDepartmentId;

    private String userDepartmentName;

    private Long coopUserId;

    private String coopUserName;

    private Integer swithType;

    @TableField("`switch`")
    private Integer switchFlag;

    private Date gmtCreateTime;

    private Long attendanceId;

    private Date syncCreatedTime;

    private Date syncUpdatedTime;
}