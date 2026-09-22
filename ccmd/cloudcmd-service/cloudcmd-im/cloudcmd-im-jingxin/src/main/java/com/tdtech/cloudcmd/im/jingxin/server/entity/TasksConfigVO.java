package com.tdtech.cloudcmd.im.jingxin.server.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * PC端任务标准件模块展示 VO
 */
@Data
public class TasksConfigVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;
    @Schema(description = "开放接口的系统编号")
    private String systemCode;
    @Schema(description = "任务标准件模块名称")
    private String module;
    @Schema(description = "是否在PC显示。1：要显示；0：不显示")
    private Integer showInPc;
    @Schema(description = "最后修改时间")
    private Date gmtLastModified;
    @Schema(description = "创建时间")
    private Date gmtCreated;
}
