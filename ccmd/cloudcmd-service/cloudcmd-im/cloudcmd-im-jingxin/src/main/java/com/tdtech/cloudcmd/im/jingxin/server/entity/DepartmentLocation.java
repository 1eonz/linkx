package com.tdtech.cloudcmd.im.jingxin.server.entity;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.Accessors;

/**
 * <p>
 * 部门位置信息表
 * </p>
 *
 * @author example
 * @since 2025-08-13
 */
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_department_location")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DepartmentLocation implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 部门名称
     */
    @TableField("department_name")
    private String departmentName;

    /**
     * 部门编码（唯一）
     */
    @TableField("department_code")
    private String departmentCode;

    /**
     * 经纬度，格式：经度,纬度
     */
    @TableField("location")
    private String location;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;

}
