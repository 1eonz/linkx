package com.tdtech.cloudcmd.linkx.dashboard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 建群记录统计明细表
 * TableName：tb_static_create_group
 */
@TableName("tb_static_create_group")
@Data
public class StaticCreateGroup implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private String userName;

    private Long userDepartmentId;

    private String userDepartmentName;

    private Long groupId;

    private String groupName;

    private Integer groupType;

    private Integer groupSubType;

    private Integer groupCreateType;

    private Date gmtCreateTime;

    private Date groupUpdatedTime;

    private Date syncCreatedTime;

    private Date syncUpdatedTime;
}