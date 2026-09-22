package com.tdtech.cloudcmd.im.jingxin.server.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 任务标准件配置表
 *
 * 对应 linkx_open.tb_tasks_config
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
@TableName("linkx_open.tb_tasks_config")
public class TasksConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId
    private Long id;
    /**
     * 开放接口的系统编号
     */
    private String systemCode;
    /**
     * 任务标准件模块名称
     */
    private String module;
    /**
     * 是否在PC显示。1：要显示；0：不显示
     */
    private Integer showInPc;
    /**
     * 最后修改时间
     */
    private Date gmtLastModified;
    /**
     * 创建时间
     */
    private Date gmtCreated;
}
