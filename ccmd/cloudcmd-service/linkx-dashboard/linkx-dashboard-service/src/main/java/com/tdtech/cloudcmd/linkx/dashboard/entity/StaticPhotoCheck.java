package com.tdtech.cloudcmd.linkx.dashboard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 人员核查统计明细表
 * TableName：tb_static_photo_check
 */
@TableName("tb_static_photo_check")
@Data
public class StaticPhotoCheck implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long checkUserId;

    private String checkUserName;

    private Long checkUserDepartmentId;

    private String checkUserDepartmentName;

    private Long coopUserId;

    private String coopUserName;

    private Long checkDataId;

    private Date gmtCreateTime;

    private Integer checkResult;

    private Date syncCreatedTime;

    private Date syncUpdatedTime;
}