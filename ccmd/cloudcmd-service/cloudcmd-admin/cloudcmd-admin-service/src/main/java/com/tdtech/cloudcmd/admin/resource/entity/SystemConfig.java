package com.tdtech.cloudcmd.admin.resource.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @author: S063874
 * @date: 2026-03-10 14:05
 */
@TableName("tb_system_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfig {
    /**
     * ID
     */
    @TableId
    private Long id;

    /**
     * 系统配置名称
     */
    @TableField("`key`")
    private String key;

    /**
     * 系统配置内容
     */
    @TableField("`value`")
    private String value;

    /**
     * 最后修改时间
     */
    private Date gmtLastModified;

    /**
     * 创建时间
     */
    private Date gmtCreateTime;
}
