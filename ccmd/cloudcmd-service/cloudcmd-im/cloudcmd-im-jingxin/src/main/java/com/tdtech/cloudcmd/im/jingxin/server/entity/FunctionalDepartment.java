package com.tdtech.cloudcmd.im.jingxin.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 职能部门实体类
 */
@Data
@TableName("tb_functional_department")
public class FunctionalDepartment {

    /**
     * 部门ID，主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 部门名称
     */
    private String name;

    /**
     * 父部门ID，顶级部门为NULL
     */
    private Long parentId;

    /**
     * 创建人
     */
    private String creator;

    /**
     * 是否删除：0-未删除，1-已删除
     */
    @TableLogic
    private Integer isDeleted;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 是否有子部门（非数据库字段）
     */
    @TableField(exist = false)
    private Boolean hasChildren;
}