package com.tdtech.cloudcmd.im.jingxin.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 部门协同岗用户关联实体类
 */
@Data
@TableName("tb_functional_department_coop")
public class FunctionalDepartmentCoop {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 职能部门ID（linkx.tb_functional_department.id）
     */
    private Long deptId;

    /**
     * 协同岗用户ID（警信的协同岗用户ID）
     */
    private Long userId;

    /**
     * 是否勾选。0：不勾选；1：勾选
     */
    private Integer checked;

    /**
     * 排序序号，值越小越靠前
     */
    private Integer sort;

    /**
     * 创建人
     */
    private String creator;

    /**
     * 最后更新人
     */
    private String updater;

    /**
     * 是否删除：0-未删除，1-已删除
     */
    @TableLogic
    private Integer isDeleted;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 创建时间
     */
    private Date gmtCreated;
}