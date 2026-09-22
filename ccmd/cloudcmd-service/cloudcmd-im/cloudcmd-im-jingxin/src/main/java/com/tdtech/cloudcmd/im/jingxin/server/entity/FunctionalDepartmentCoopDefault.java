package com.tdtech.cloudcmd.im.jingxin.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 部门协同岗用户关联默认表实体类
 */
@Data
@TableName("tb_functional_department_coop_default")
public class FunctionalDepartmentCoopDefault {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 协同岗用户ID
     */
    private Long userId;

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
    private Date gmtCreated;
}